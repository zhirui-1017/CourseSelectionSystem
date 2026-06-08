package org.example.courseselectionsystem.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false"
})
class GatewayServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
