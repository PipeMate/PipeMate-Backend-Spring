package com.example.pipemate.pipeline.res;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class PipelineResponse {
    /**
     * MongoDB에 저장된 워크플로우 ID
     */
    private String workflowId;

    /**
     * 워크플로우 이름
     */
    private String workflowName;

    /**
     * 원본 JSON 데이터
     */
    private List<JsonNode> originalJson;

    /**
     * 변환된 JSON 데이터 (res_input.json 형식)
     */
    private Map<String, Object> convertedJson;

    /**
     * 변환된 YAML 내용
     */
    private String yamlContent;

    /**
     * GitHub에서의 파일 경로
     */
    private String githubPath;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 응답 메시지
     */
    private String message;
}
