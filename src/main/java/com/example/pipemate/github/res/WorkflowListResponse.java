package com.example.pipemate.github.res;

import com.example.pipemate.github.WorkflowItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowListResponse {
    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("workflows")
    private List<WorkflowItem> workflows;
}