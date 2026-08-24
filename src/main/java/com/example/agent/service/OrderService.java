package com.example.agent.service;

import com.example.agent.domain.Order;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 订单领域服务。为了突出 Tool Calling，本示例用内存数据代替数据库。
 */
@Service
public class OrderService {

    private final Map<String, Order> orders = Map.of(
            "1001", new Order("1001", "机械键盘", "已发货"),
            "1002", new Order("1002", "显示器", "处理中")
    );

    public Order findById(String orderId) {
        return orders.get(orderId);
    }
}
