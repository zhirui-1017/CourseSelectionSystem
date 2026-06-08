package org.example.courseselectionsystem.gateway;

import org.example.courseselectionsystem.common.ServiceNames;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRoutesConfigurationTest {

    private final Properties properties = loadGatewayProperties();

    @Test
    void apiDomainRoutesPointToOwnedServicesBeforeLegacyFallback() {
        assertRoute(0, ServiceNames.COURSE_SERVICE,
                "/api/v1/courses/**,/api/v1/colleges/**,/api/v1/departments/**,/api/v1/majors/**");
        assertRoute(1, ServiceNames.SELECTION_SERVICE,
                "/api/v1/selections/**,/api/v1/course-selections/**");
        assertRoute(2, ServiceNames.USER_SERVICE,
                "/api/v1/users/**,/api/v1/roles/**,/api/v1/permissions/**");
        assertRoute(3, ServiceNames.STUDENT_SERVICE, "/api/v1/students/**");
        assertRoute(4, ServiceNames.TEACHER_SERVICE, "/api/v1/teachers/**");
    }

    @Test
    void legacyWebRouteKeepsPageAndFallbackApiCompatibilityLast() {
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].id"))
                .isEqualTo("web-service-legacy");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].uri"))
                .isEqualTo("lb://" + ServiceNames.WEB_SERVICE);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].predicates[0]"))
                .isEqualTo("Path=/,/login/**,/admin/**,/student/**,/teacher/**,/static/**,/css/**,/js/**,/images/**,/webjars/**,/api/v1/**");
    }

    @Test
    void everyRouteUsesCircuitBreakerFallback() {
        for (int i = 0; i <= 5; i++) {
            assertThat(properties.getProperty("spring.cloud.gateway.routes[" + i + "].filters[0].name"))
                    .isEqualTo("CircuitBreaker");
            assertThat(properties.getProperty("spring.cloud.gateway.routes[" + i + "].filters[0].args.fallbackUri"))
                    .isEqualTo("forward:/fallback");
        }
    }

    private void assertRoute(int index, String serviceName, String pathPredicate) {
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + index + "].id"))
                .isEqualTo(serviceName);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + index + "].uri"))
                .isEqualTo("lb://" + serviceName);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[" + index + "].predicates[0]"))
                .isEqualTo("Path=" + pathPredicate);
    }

    private static Properties loadGatewayProperties() {
        try (InputStream inputStream = new ClassPathResource("application.properties").getInputStream()) {
            Properties loaded = new Properties();
            loaded.load(inputStream);
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load gateway application.properties", ex);
        }
    }
}
