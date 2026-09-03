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
    void apiDomainRoutesPointToOwnedServicesBeforeWebCatchall() {
        // Route 0: 用户/认证/角色/权限
        assertRoute(0, ServiceNames.USER_SERVICE,
                "/api/v1/auth/**,/api/v1/users/**,/api/v1/roles/**,/api/v1/permissions/**,/api/v1/messages/**,/api/v1/operation-logs/**,/api/v1/system-settings/**");
        // Route 1: 学生 + 班级
        assertRoute(1, ServiceNames.STUDENT_SERVICE, "/api/v1/students/**,/api/v1/classes/**");
        // Route 2: 教师
        assertRoute(2, ServiceNames.TEACHER_SERVICE, "/api/v1/teachers/**");
        // Route 3: 课程/学院/系部/专业/学期/公告
        assertRoute(3, ServiceNames.COURSE_SERVICE,
                "/api/v1/courses/**,/api/v1/colleges/**,/api/v1/departments/**,/api/v1/majors/**,/api/v1/semesters/**,/api/v1/course-announcements/**");
        // Route 4: 选课/评价/成绩
        assertRoute(4, ServiceNames.SELECTION_SERVICE,
                "/api/v1/course-selections/**,/api/v1/evaluations/**,/api/v1/grades/**");
    }

    @Test
    void webServiceRouteIsCatchallLast() {
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].id"))
                .isEqualTo(ServiceNames.WEB_SERVICE);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].uri"))
                .isEqualTo("lb://" + ServiceNames.WEB_SERVICE);
        assertThat(properties.getProperty("spring.cloud.gateway.routes[5].predicates[0]"))
                .isEqualTo("Path=/**");
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
