package com.example.agent.service;

import com.example.agent.tool.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 最小 Agent：把用户问题发给 LLM，并把订单工具提供给它。
 *
 * ChatClient 会自动完成这段循环：LLM 请求工具 -> Java 执行工具
 * -> 工具结果发回 LLM -> LLM 生成最终的自然语言回答。
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final OrderTools orderTools;

    public AgentService(ChatClient.Builder chatClientBuilder, OrderTools orderTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是订单查询助手。涉及订单事实时必须调用订单查询工具，不要猜测。")
                .build();
        this.orderTools = orderTools;
    }

    public String chat(String question) {
        return chatClient.prompt()
                .user(question)
                .tools(orderTools)
                .call()
                .content();
    }
}
