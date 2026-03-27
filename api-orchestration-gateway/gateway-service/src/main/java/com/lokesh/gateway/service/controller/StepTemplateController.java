package com.lokesh.gateway.service.controller;

import com.lokesh.gateway.common.enums.StepType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/step-templates")
@Tag(name = "Step templates")
public class StepTemplateController {

    @GetMapping("/types")
    @Operation(summary = "List step types and config hints")
    public List<Map<String, Object>> types() {
        return List.of(
                type(StepType.HTTP_CALL, "Outbound HTTP", Map.of(
                        "url", "string (required)",
                        "method", "GET|POST|PUT|PATCH|DELETE",
                        "headers", "map",
                        "body", "object",
                        "extractors", "map of name -> JsonPath"
                )),
                type(StepType.TRANSFORM, "Map / SpEL transform", Map.of(
                        "mappings", "map of outputKey -> SpEL or input.field path"
                )),
                type(StepType.CONDITION, "SpEL branch", Map.of(
                        "condition", "SpEL boolean using #input",
                        "onTrue", "next step id",
                        "onFalse", "next step id"
                )),
                type(StepType.DELAY, "Sleep", Map.of(
                        "delayMs", "long"
                )),
                type(StepType.AGGREGATE, "Fan-in merge", Map.of(
                        "waitFor", "list of step ids",
                        "strategy", "merge | nest"
                )),
                type(StepType.SCRIPT, "Simulated script", Map.of(
                        "language", "string",
                        "code", "string (required)"
                ))
        );
    }

    private static Map<String, Object> type(StepType t, String title, Map<String, String> schema) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", t.name());
        m.put("title", title);
        m.put("configSchema", schema);
        return m;
    }
}
