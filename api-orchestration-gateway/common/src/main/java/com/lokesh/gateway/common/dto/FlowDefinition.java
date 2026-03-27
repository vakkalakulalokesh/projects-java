package com.lokesh.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowDefinition {

    @NotEmpty
    @Valid
    @Builder.Default
    private List<StepDefinition> steps = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<EdgeDefinition> edges = new ArrayList<>();
}
