package com.example.pipemate.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WorkflowItem {

    private long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("state")
    private String state;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private String url;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("badge_url")
    private String badgeUrl;

    /**
     * path에서 추출한 fileName (서버 응답에 포함됨)
     */
    @JsonProperty("fileName")
    public String getFileName() {
        if (path == null) return null;
        return path.substring(path.lastIndexOf('/') + 1);
    }
}