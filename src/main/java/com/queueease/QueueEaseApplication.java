package com.queueease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.queueease")
@EntityScan(basePackages = "com.queueease.entity")
@EnableJpaRepositories(basePackages = "com.queueease.repository")
public class QueueEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueEaseApplication.class, args);
    }
}
