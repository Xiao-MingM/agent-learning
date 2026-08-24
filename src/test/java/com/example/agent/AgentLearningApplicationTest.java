package com.example.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 只检查 Spring 容器能否完整装配，不发送任何 LLM 请求。
 * 假 API Key 仅用于通过 OpenAI 自动配置的启动校验，不会产生网络调用或费用。
 */
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class AgentLearningApplicationTest {

    @Test
    void contextLoads() {
        // Spring 容器成功启动即表示 Controller、Service、ChatClient 和 Tool 均可装配。
    }
}
