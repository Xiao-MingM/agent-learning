package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口。Spring Boot 会自动创建 ChatClient.Builder 和其他必要组件。
 */
@SpringBootApplication
public class AgentLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentLearningApplication.class, args);
    }
}
