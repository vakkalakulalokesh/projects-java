package com.lokesh.gateway.service.engine;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StepInputResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern FULL_PLACEHOLDER = Pattern.compile("^\\{\\{([^}]+)}}$");

    @SuppressWarnings("unchecked")
    public Map<String, Object> resolveInput(Map<String, Object> stepConfig, Map<String, Object> executionContext) {
        if (stepConfig == null || stepConfig.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : stepConfig.entrySet()) {
            resolved.put(e.getKey(), resolveValue(e.getValue(), executionContext));
        }
        return resolved;
    }

    private Object resolveValue(Object value, Map<String, Object> ctx) {
        if (value instanceof String s) {
            return resolveString(s, ctx);
        }
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> inner = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                inner.put(String.valueOf(e.getKey()), resolveValue(e.getValue(), ctx));
            }
            return inner;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> resolveValue(v, ctx)).toList();
        }
        return value;
    }

    private Object resolveString(String s, Map<String, Object> ctx) {
        Matcher full = FULL_PLACEHOLDER.matcher(s);
        if (full.matches()) {
            Object v = lookup(full.group(1).trim(), ctx);
            return v != null ? v : "";
        }
        Matcher m = PLACEHOLDER.matcher(s);
        if (!m.find()) {
            return s;
        }
        m.reset();
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object v = lookup(m.group(1).trim(), ctx);
            m.appendReplacement(sb, Matcher.quoteReplacement(v != null ? String.valueOf(v) : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Object lookup(String path, Map<String, Object> ctx) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        if ("input".equals(parts[0])) {
            Map<String, Object> input = (Map<String, Object>) ctx.getOrDefault("input", Map.of());
            return navigate(input, parts, 1);
        }
        String stepId = parts[0];
        if (parts.length > 2 && "output".equals(parts[1])) {
            Map<String, Object> steps = (Map<String, Object>) ctx.getOrDefault("steps", Map.of());
            Object out = steps.get(stepId);
            if (out instanceof Map<?, ?> om) {
                return navigate((Map<String, Object>) om, parts, 2);
            }
        }
        Map<String, Object> steps = (Map<String, Object>) ctx.getOrDefault("steps", Map.of());
        Object stepOut = steps.get(stepId);
        if (stepOut instanceof Map<?, ?> om) {
            return navigate((Map<String, Object>) om, parts, 1);
        }
        return null;
    }

    private Object navigate(Map<String, Object> map, String[] parts, int start) {
        Object cur = map;
        for (int i = start; i < parts.length; i++) {
            if (!(cur instanceof Map<?, ?> m)) {
                return null;
            }
            cur = m.get(parts[i]);
        }
        return cur;
    }
}
