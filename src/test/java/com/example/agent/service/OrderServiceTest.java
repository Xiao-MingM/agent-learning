package com.example.agent.service;

import com.example.agent.domain.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {
    private final OrderService orderService = new OrderService();

    @Test
    void shouldFindExistingOrder() {
        assertThat(orderService.findById("1001"))
                .isEqualTo(new Order("1001", "机械键盘", "已发货"));
    }

    @Test
    void shouldReturnNullForUnknownOrder() {
        assertThat(orderService.findById("9999")).isNull();
    }
}
