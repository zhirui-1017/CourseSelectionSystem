package org.example.courseselectionsystem.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "org.example.courseselectionsystem")
@EnableJpaRepositories(basePackages = "org.example.courseselectionsystem.repository")
@EntityScan(basePackages = "org.example.courseselectionsystem.entity")
@MapperScan(basePackages = "org.example.courseselectionsystem.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
