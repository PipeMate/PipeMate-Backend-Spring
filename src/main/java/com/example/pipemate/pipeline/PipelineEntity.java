package com.example.pipemate.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
public class PipelineEntity {
    @Id
    private String id;

    /**
     * GitHub 레포지토리 소유자
     */
    private String owner;

    /**
     * GitHub 레포지토리 이름
     */
    private String repo;

    /**
     * 워크플로우 이름
     */
    private String workflowName;

    /**
     * 원본 JSON 데이터 (input.json)
     */
    private List<JsonNode> originalJson;

    /**
     * 변환된 JSON 데이터 (res_input.json)
     */
    private Map<String, Object> convertedJson;

    /**
     * 변환된 YAML 내용
     */
    private String yamlContent;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
}
