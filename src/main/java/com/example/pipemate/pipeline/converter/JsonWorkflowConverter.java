package com.example.pipemate.pipeline.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonWorkflowConverter {

    private final ObjectMapper objectMapper;

    /**
     * 원본 JSON 배열을 GitHub Workflow 형식의 JSON으로 변환
     * @param inputJsonBlocks 원본 JSON 블록들
     * @return 변환된 워크플로우 JSON
     */
    public Map<String, Object> convertToWorkflowJson(List<JsonNode> inputJsonBlocks) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode steps = objectMapper.createArrayNode();
            String jobId = null;

            log.info("Starting JSON conversion process with {} blocks", inputJsonBlocks.size());

            for (JsonNode block : inputJsonBlocks) {
                String type = block.path("type").asText(null);

                // type이 없는 경우 deploy 블록 등으로 처리
                if (type == null) {
                    if (block.has("uses") && block.has("with")) {
                        ObjectNode step = objectMapper.createObjectNode();
                        step.put("name", block.get("name").asText());
                        step.put("uses", block.get("uses").asText());
                        step.set("with", block.get("with"));
                        steps.add(step);
                        continue;
                    }
                    log.warn("Unknown block without type: {}", block.toPrettyString());
                    continue;
                }

                JsonNode config = block.path("config");

                switch (type) {
                    case "trigger":
                        root.put("name", config.path("name").asText());
                        root.set("on", config.get("on"));
                        log.debug("Processed trigger block: {}", config.path("name").asText());
                        break;

                    case "job":
                        if (config.has("jobs") && config.get("jobs").isObject()) {
                            jobId = config.get("jobs").fieldNames().next();
                            log.debug("Processed job block with jobId: {}", jobId);
                        }
                        break;

                    case "step":
                        steps.add(config);
                        log.debug("Processed step block");
                        break;

                    default:
                        log.warn("Unknown type: {}", type);
                        break;
                }
            }

            // jobs 구성
            ObjectNode jobs = objectMapper.createObjectNode();
            ObjectNode jobConfig = objectMapper.createObjectNode();
            jobConfig.put("runs-on", "ubuntu-latest");
            jobConfig.set("steps", steps);

            // jobId가 null인 경우 기본값 설정
            if (jobId == null) {
                jobId = "ci-pipeline";
                log.warn("No jobId found, using default: {}", jobId);
            }

            jobs.set(jobId, jobConfig);
            root.set("jobs", jobs);

            log.info("JSON conversion completed successfully");

            // ObjectNode를 Map으로 변환
            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Error during JSON conversion", e);
            throw new RuntimeException("Failed to convert JSON workflow: " + e.getMessage(), e);
        }
    }
}