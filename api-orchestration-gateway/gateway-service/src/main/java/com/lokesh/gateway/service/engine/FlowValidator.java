package com.lokesh.gateway.service.engine;

import com.lokesh.gateway.common.dto.EdgeDefinition;
import com.lokesh.gateway.common.dto.FlowDefinition;
import com.lokesh.gateway.common.dto.StepDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Component
public class FlowValidator {

    public List<String> validateFlow(FlowDefinition definition) {
        List<String> errors = new ArrayList<>();
        if (definition == null || definition.getSteps() == null || definition.getSteps().isEmpty()) {
            errors.add("Flow must contain at least one step");
            return errors;
        }

        Set<String> ids = new HashSet<>();
        for (StepDefinition s : definition.getSteps()) {
            if (s.getStepId() == null || s.getStepId().isBlank()) {
                errors.add("Step id is required");
            } else if (!ids.add(s.getStepId())) {
                errors.add("Duplicate step id: " + s.getStepId());
            }
            if (s.getType() == null) {
                errors.add("Step type is required for " + s.getStepId());
            }
            if (s.getName() == null || s.getName().isBlank()) {
                errors.add("Step name is required for " + s.getStepId());
            }
            errors.addAll(validateStepConfig(s));
        }

        List<EdgeDefinition> edges = definition.getEdges() != null ? definition.getEdges() : List.of();
        for (EdgeDefinition e : edges) {
            if (e.getFromStep() == null || e.getToStep() == null) {
                errors.add("Edge fromStep and toStep are required");
                continue;
            }
            if (!ids.contains(e.getFromStep())) {
                errors.add("Edge references unknown fromStep: " + e.getFromStep());
            }
            if (!ids.contains(e.getToStep())) {
                errors.add("Edge references unknown toStep: " + e.getToStep());
            }
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        Map<String, Set<String>> preds = new HashMap<>();
        Map<String, Set<String>> succs = new HashMap<>();
        for (String id : ids) {
            preds.put(id, new HashSet<>());
            succs.put(id, new HashSet<>());
        }
        for (EdgeDefinition e : edges) {
            succs.get(e.getFromStep()).add(e.getToStep());
            preds.get(e.getToStep()).add(e.getFromStep());
        }

        List<String> roots = ids.stream().filter(id -> preds.get(id).isEmpty()).toList();
        if (roots.isEmpty()) {
            errors.add("Flow must have at least one root step with no incoming edges");
        }

        List<String> topo = topologicalOrder(ids, succs);
        if (topo == null) {
            errors.add("Flow contains a cycle");
        }

        return errors;
    }

    private List<String> validateStepConfig(StepDefinition s) {
        List<String> e = new ArrayList<>();
        Map<String, Object> c = s.getConfig();
        if (c == null) {
            c = Map.of();
        }
        switch (s.getType()) {
            case HTTP_CALL -> {
                if (str(c.get("url")).isBlank()) {
                    e.add("HTTP_CALL step " + s.getStepId() + " requires config.url");
                }
            }
            case CONDITION -> {
                if (str(c.get("condition")).isBlank()) {
                    e.add("CONDITION step " + s.getStepId() + " requires config.condition");
                }
            }
            case DELAY -> {
                if (c.get("delayMs") == null) {
                    e.add("DELAY step " + s.getStepId() + " requires config.delayMs");
                }
            }
            case AGGREGATE -> {
                if (!(c.get("waitFor") instanceof List<?> list) || list.isEmpty()) {
                    e.add("AGGREGATE step " + s.getStepId() + " requires config.waitFor as non-empty list");
                }
            }
            case SCRIPT -> {
                if (str(c.get("code")).isBlank()) {
                    e.add("SCRIPT step " + s.getStepId() + " requires config.code");
                }
            }
            default -> {
            }
        }
        return e;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private List<String> topologicalOrder(Set<String> ids, Map<String, Set<String>> succs) {
        Map<String, Integer> in = new HashMap<>();
        for (String id : ids) {
            in.put(id, 0);
        }
        for (String u : ids) {
            for (String v : succs.get(u)) {
                in.merge(v, 1, Integer::sum);
            }
        }
        Queue<String> q = new ArrayDeque<>();
        for (String id : ids) {
            if (in.get(id) == 0) {
                q.add(id);
            }
        }
        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            String u = q.poll();
            order.add(u);
            for (String v : succs.get(u)) {
                int nv = in.merge(v, -1, Integer::sum);
                if (nv == 0) {
                    q.add(v);
                }
            }
        }
        if (order.size() != ids.size()) {
            return null;
        }
        return order;
    }
}
