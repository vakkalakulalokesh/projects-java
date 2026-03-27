package com.lokesh.gateway.service.engine;

import com.lokesh.gateway.common.dto.EdgeDefinition;
import com.lokesh.gateway.common.dto.FlowDefinition;
import com.lokesh.gateway.common.dto.StepDefinition;
import com.lokesh.gateway.common.enums.StepType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowValidatorTest {

    private final FlowValidator validator = new FlowValidator();

    @Test
    void acceptsLinearFlow() {
        FlowDefinition def = FlowDefinition.builder()
                .steps(List.of(
                        step("a", "A", StepType.HTTP_CALL, java.util.Map.of("url", "https://example.com")),
                        step("b", "B", StepType.DELAY, java.util.Map.of("delayMs", 1))
                ))
                .edges(List.of(EdgeDefinition.builder().fromStep("a").toStep("b").build()))
                .build();
        assertTrue(validator.validateFlow(def).isEmpty());
    }

    @Test
    void rejectsCycle() {
        FlowDefinition def = FlowDefinition.builder()
                .steps(List.of(
                        step("a", "A", StepType.DELAY, java.util.Map.of("delayMs", 1)),
                        step("b", "B", StepType.DELAY, java.util.Map.of("delayMs", 1))
                ))
                .edges(List.of(
                        EdgeDefinition.builder().fromStep("a").toStep("b").build(),
                        EdgeDefinition.builder().fromStep("b").toStep("a").build()
                ))
                .build();
        assertFalse(validator.validateFlow(def).isEmpty());
    }

    private static StepDefinition step(String id, String name, StepType type, java.util.Map<String, Object> cfg) {
        return StepDefinition.builder().stepId(id).name(name).type(type).config(cfg).build();
    }
}
