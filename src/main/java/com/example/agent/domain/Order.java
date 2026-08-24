package com.example.agent.domain;

/**
 * 工具返回给 LLM 的订单数据。
 * record 自带构造方法和访问方法，适合这个只承载数据的学习示例。
 */
public record Order(String id, String productName, String status) {
}
