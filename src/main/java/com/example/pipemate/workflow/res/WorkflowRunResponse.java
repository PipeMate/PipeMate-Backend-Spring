package com.example.pipemate.workflow.res;

import lombok.Data;

@Data
public class WorkflowRunResponse {
    private Long id;
    private String name;
    private String node_id;
    private Long check_suite_id;
    private String check_suite_node_id;
    private String head_branch;
    private String head_sha;
    private String path;
    private Long run_number;
    private String event;
    private String display_title;
    private String status;
    private String conclusion;
    private Long workflow_id;
    private String url;
    private String html_url;
    private String created_at;
    private String updated_at;

    // 필요 시 nested DTO (예: actor, pull_requests 등)도 정의
}