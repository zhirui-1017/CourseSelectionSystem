package org.example.courseselectionsystem.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.mybatis.spring.annotation.MapperScan;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "org.example.courseselectionsystem")
@EnableJpaRepositories(basePackages = "org.example.courseselectionsystem.repository")
@EntityScan(basePackages = "org.example.courseselectionsystem.entity")
@MapperScan(basePackages = "org.example.courseselectionsystem.mapper")
public class CourseServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
