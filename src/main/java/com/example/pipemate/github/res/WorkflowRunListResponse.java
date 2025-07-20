package com.example.pipemate.github.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkflowRunListResponse {

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("workflow_runs")
    private List<WorkflowRun> workflowRuns;

    @Getter
    @Setter
    public static class WorkflowRun {
        private long id;
        private String name;
        @JsonProperty("node_id")
        private String nodeId;
        @JsonProperty("head_branch")
        private String headBranch;
        @JsonProperty("head_sha")
        private String headSha;
        private String path;
        private String status;
        private String conclusion;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("html_url")
        private String htmlUrl;
    }
}