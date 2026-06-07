package org.example.courseselectionsystem.selection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SelectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelectionServiceApplication.class, args);
    }
}
