package com.example.pipemate.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WorkflowItem {
    private long id;
    private String name;
    private String path;

    @JsonProperty("state")
    private String state;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private String url;
    private String html_url;
    private String badge_url;
}