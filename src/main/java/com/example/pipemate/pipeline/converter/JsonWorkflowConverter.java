package com.example.pipemate.pipeline.converter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonWorkflowConverter {

    private final ObjectMapper objectMapper;

    /**
     * 원본 JSON 배열을 GitHub Workflow 형식의 JSON으로 변환
     *
     * @param inputJsonBlocks 원본 JSON 블록들
     * @return 변환된 워크플로우 JSON
     */
    public Map<String, Object> convertToWorkflowJson(List<JsonNode> inputJsonBlocks) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode jobs = objectMapper.createObjectNode();
            Map<String, ArrayNode> jobStepsMap = new HashMap<>();

            log.info("Starting JSON conversion process with {} blocks", inputJsonBlocks.size());

            for (JsonNode block : inputJsonBlocks) {
                String type = block.path("type").asText(null);

                if (type == null) {
                    if (block.has("uses") && block.has("with")) {
                        String defaultJobName = jobStepsMap.keySet().isEmpty()
                                ? "ci-pipeline"
                                : jobStepsMap.keySet().iterator().next();

                        ArrayNode steps = jobStepsMap.computeIfAbsent(defaultJobName, k -> objectMapper.createArrayNode());

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
                        String jobId = block.path("job-name").asText("ci-pipeline");
                        ObjectNode jobNode = objectMapper.createObjectNode();

                        config.fieldNames().forEachRemaining(fieldName -> {
                            if (!fieldName.equals("steps")) {
                                jobNode.set(fieldName, config.get(fieldName));
                            }
                        });

                        jobs.set(jobId, jobNode);
                        jobStepsMap.put(jobId, objectMapper.createArrayNode());
                        log.debug("Processed job block with jobName: {}", jobId);
                        break;

                    case "step":
                        String targetJobId = block.path("job-name").asText("ci-pipeline");

                        // 없으면 생성
                        if (!jobs.has(targetJobId)) {
                            ObjectNode defaultJob = objectMapper.createObjectNode();
                            defaultJob.put("runs-on", "ubuntu-latest");
                            jobs.set(targetJobId, defaultJob);
                            jobStepsMap.put(targetJobId, objectMapper.createArrayNode());
                        }

                        ArrayNode stepList = jobStepsMap.get(targetJobId);
                        stepList.add(config);
                        log.debug("Processed step block for job: {}", targetJobId);
                        break;

                    default:
                        log.warn("Unknown type: {}", type);
                        break;
                }
            }

            // steps 추가
            jobStepsMap.forEach((jobName, steps) -> {
                if (jobs.has(jobName)) {
                    ObjectNode jobNode = (ObjectNode) jobs.get(jobName);
                    jobNode.set("steps", steps);
                }
            });

            // jobs가 없으면 기본 job 추가
            if (jobs.size() == 0) {
                ObjectNode defaultJob = objectMapper.createObjectNode();
                defaultJob.put("runs-on", "ubuntu-latest");
                defaultJob.set("steps", objectMapper.createArrayNode());
                jobs.set("ci-pipeline", defaultJob);
                log.warn("No jobs found, using default: ci-pipeline");
            }

            root.set("jobs", jobs);
            log.info("JSON conversion completed successfully");

            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Error during JSON conversion", e);
            throw new RuntimeException("Failed to convert JSON workflow: " + e.getMessage(), e);
        }
    }


    public List<JsonNode> convertWorkflowJsonToBlocks(Map<String, Object> workflowJson) {
        try {
            List<JsonNode> blocks = new ArrayList<>();
            ObjectNode root = objectMapper.convertValue(workflowJson, ObjectNode.class);

            // 1. trigger block
            if (root.has("on") || root.has("name")) {
                ObjectNode config = objectMapper.createObjectNode();
                if (root.has("name")) config.put("name", root.get("name").asText());
                if (root.has("on")) config.set("on", root.get("on"));

                ObjectNode triggerBlock = objectMapper.createObjectNode();
                triggerBlock.put("type", "trigger");
                triggerBlock.set("config", config);
                blocks.add(triggerBlock);
            }

            // 2. job + step block
            if (root.has("jobs")) {
                ObjectNode jobs = (ObjectNode) root.get("jobs");

                Iterator<String> jobIds = jobs.fieldNames();
                while (jobIds.hasNext()) {
                    String jobId = jobIds.next();
                    ObjectNode jobConfig = (ObjectNode) jobs.get(jobId);

                    // job block 생성 (steps 제거 후 추가)
                    ObjectNode jobConfigCopy = jobConfig.deepCopy();
                    JsonNode stepsNode = jobConfigCopy.remove("steps"); // steps 분리

                    // ✅ 수정된 job block 생성
                    ObjectNode jobWrapper = objectMapper.createObjectNode();
                    jobWrapper.put("type", "job");
                    jobWrapper.put("job-name", jobId);  // <- 핵심
                    jobWrapper.set("config", jobConfigCopy);
                    blocks.add(jobWrapper);

                    // step blocks
                    if (stepsNode != null && stepsNode.isArray()) {
                        for (JsonNode step : stepsNode) {
                            ObjectNode stepBlock = objectMapper.createObjectNode();
                            stepBlock.put("type", "step");
                            stepBlock.put("job-name", jobId); // ✅ step에도 job-name을 명시해줍니다
                            stepBlock.set("config", step);
                            blocks.add(stepBlock);
                        }
                    }
                }
            }

            log.info("Workflow JSON successfully converted to block-based JSON. Total blocks: {}", blocks.size());
            return blocks;

        } catch (Exception e) {
            log.error("Error converting workflow JSON to blocks", e);
            throw new RuntimeException("Failed to convert workflow JSON to block format: " + e.getMessage(), e);
        }
    }
}