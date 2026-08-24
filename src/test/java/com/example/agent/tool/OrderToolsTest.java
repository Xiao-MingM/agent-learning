package com.example.agent.tool;

import com.example.agent.service.OrderService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderToolsTest {
    private final OrderTools orderTools = new OrderTools(new OrderService());

    @Test
    void shouldReturnStructuredToolResult() {
        OrderTools.OrderQueryResult result = orderTools.queryOrder("1002");

        assertThat(result.found()).isTrue();
        assertThat(result.order().status()).isEqualTo("处理中");
    }

    @Test
    void shouldExplainWhenOrderDoesNotExist() {
        OrderTools.OrderQueryResult result = orderTools.queryOrder("9999");

        assertThat(result.found()).isFalse();
        assertThat(result.order()).isNull();
    }
}
