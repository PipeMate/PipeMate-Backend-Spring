package com.example.pipemate.pipeline.req;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.List;

@Getter
public class PipelineRequest {
    /**
     * GitHub 레포지토리 소유자
     */
    private String owner;

    /**
     * GitHub 레포지토리 이름
     */
    private String repo;

    /**
     * 워크플로우 이름 (GitHub에 저장될 파일명에 사용)
     */
    private String workflowName;

    /**
     * 원본 JSON 배열 (input.json 내용)
     * 예: [{"type": "trigger", "config": {...}}, {"type": "job", "config": {...}}, ...]
     */
    private List<JsonNode> inputJson;

    /**
     * 워크플로우 설명 (선택사항)
     */
    private String description;
}
