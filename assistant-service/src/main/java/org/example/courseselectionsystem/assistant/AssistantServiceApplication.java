package org.example.courseselectionsystem.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "org.example.courseselectionsystem")
@MapperScan(basePackages = "org.example.courseselectionsystem.mapper")
public class AssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssistantServiceApplication.class, args);
    }
}
