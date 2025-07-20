package com.example.pipemate.workflow.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowDetailResponse {
    private Long id;
    @JsonProperty("node_id")
    private String nodeId;
    private String name;
    private String path;
    private String state;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("badge_url")
    private String badgeUrl;
}