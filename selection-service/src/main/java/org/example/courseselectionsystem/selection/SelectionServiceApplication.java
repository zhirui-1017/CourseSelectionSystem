package org.example.courseselectionsystem.selection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.mybatis.spring.annotation.MapperScan;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.example.courseselectionsystem.feign")
@SpringBootApplication(scanBasePackages = "org.example.courseselectionsystem")
@EnableJpaRepositories(basePackages = "org.example.courseselectionsystem.repository")
@EntityScan(basePackages = "org.example.courseselectionsystem.entity")
@MapperScan(basePackages = "org.example.courseselectionsystem.mapper")
public class SelectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelectionServiceApplication.class, args);
    }
}
