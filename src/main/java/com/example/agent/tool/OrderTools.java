package com.example.agent.tool;

import com.example.agent.domain.Order;
import com.example.agent.service.OrderService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 暴露给 LLM 的工具集合。
 * LLM 只能看到工具说明和参数结构，真正的查询仍由 Java 代码执行。
 */
@Component
public class OrderTools {

    private final OrderService orderService;

    public OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(description = "根据订单编号查询订单的商品和当前状态")
    public OrderQueryResult queryOrder(
            @ToolParam(description = "订单编号，例如 1001") String orderId) {
        Order order = orderService.findById(orderId);
        if (order == null) {
            return new OrderQueryResult(false, "没有找到订单 " + orderId, null);
        }
        return new OrderQueryResult(true, "查询成功", order);
    }

    /** 工具使用明确的结果结构，便于 LLM 区分查询成功与订单不存在。 */
    public record OrderQueryResult(boolean found, String message, Order order) {
    }
}
