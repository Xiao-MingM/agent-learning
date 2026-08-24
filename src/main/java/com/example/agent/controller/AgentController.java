package com.example.agent.controller;

import com.example.agent.service.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 的 HTTP 入口，只负责接收问题并返回最终回答。
 * Tool Calling 的细节不会放在 Controller 中。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return new ChatResponse(agentService.chat(request.question()));
    }

    /** HTTP 请求结构。这里只保留 Agent 所需的一个问题字段。 */
    public record ChatRequest(String question) {
    }

    /** HTTP 响应结构，answer 是 LLM 在工具执行后的最终总结。 */
    public record ChatResponse(String answer) {
    }
}
