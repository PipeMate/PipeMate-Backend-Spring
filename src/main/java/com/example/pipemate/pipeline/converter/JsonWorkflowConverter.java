package com.example.pipemate.pipeline.converter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

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
                        String defaultJobName = jobStepsMap.isEmpty()
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
                        root.put("x_name", block.path("name").asText(""));
                        root.put("x_description", block.path("description").asText(""));
                        log.debug("Processed trigger block: {}", config.path("name").asText());
                        break;

                    case "job": {
                        String jobId = block.path("job-name").asText("ci-pipeline");
                        ObjectNode jobNode = objectMapper.createObjectNode();

                        config.fieldNames().forEachRemaining(fieldName -> {
                            if (!fieldName.equals("steps")) {
                                jobNode.set(fieldName, config.get(fieldName));
                            }
                        });

                        jobNode.put("x_name", block.path("name").asText(""));
                        jobNode.put("x_description", block.path("description").asText(""));

                        jobs.set(jobId, jobNode);
                        jobStepsMap.put(jobId, objectMapper.createArrayNode());
                        log.debug("Processed job block with jobName: {}", jobId);
                        break;
                    }

                    case "step": {
                        String targetJobId = block.path("job-name").asText("ci-pipeline");

                        if (!jobs.has(targetJobId)) {
                            ObjectNode defaultJob = objectMapper.createObjectNode();
                            defaultJob.put("runs-on", "ubuntu-latest");
                            jobs.set(targetJobId, defaultJob);
                            jobStepsMap.put(targetJobId, objectMapper.createArrayNode());
                        }

                        ArrayNode stepList = jobStepsMap.get(targetJobId);

                        ObjectNode configCopy = config.deepCopy();
                        configCopy.put("x_name", block.path("name").asText(""));
                        configCopy.put("x_description", block.path("description").asText(""));
                        configCopy.put("x_domain", block.path("domain").asText(""));
                        configCopy.set("x_task", block.path("task"));

                        stepList.add(configCopy);
                        log.debug("Processed step block for job: {}", targetJobId);
                        break;
                    }

                    default:
                        log.warn("Unknown type: {}", type);
                        break;
                }
            }

            jobStepsMap.forEach((jobName, steps) -> {
                if (jobs.has(jobName)) {
                    ObjectNode jobNode = (ObjectNode) jobs.get(jobName);
                    jobNode.set("steps", steps);
                }
            });

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


    public List<JsonNode> convertWorkflowJsonToBlocks(Map<String, Object> yamlJsonMap) {
        List<JsonNode> blockList = new ArrayList<>();

        // ObjectMapper 생성
        ObjectMapper mapper = new ObjectMapper();

        // Convert Map to JsonNode
        JsonNode root = mapper.convertValue(yamlJsonMap, JsonNode.class);

        // 1. trigger block
        // 1. config 안에 name (루트에 있던), on 포함
        ObjectNode triggerConfig = mapper.createObjectNode();
        triggerConfig.put("name", root.path("name").asText(""));  // 루트 name을 config.name에 포함
        triggerConfig.set("on", root.path("on"));

        // 2. trigger 블록 전체 구성
        ObjectNode triggerBlock = mapper.createObjectNode();
        triggerBlock.put("type", "trigger");
        triggerBlock.put("name", root.path("x_name").asText("워크플로우 기본 설정"));
        triggerBlock.put("description", root.path("x_description").asText("GitHub Actions 워크플로우 이름과 트리거 조건을 설정하는 블록입니다."));
        triggerBlock.set("config", triggerConfig);

        // 3. 리스트에 추가
        blockList.add(triggerBlock);


        // 2. jobs
        JsonNode jobsNode = root.path("jobs");
        Iterator<String> jobNames = jobsNode.fieldNames();

        while (jobNames.hasNext()) {
            String jobName = jobNames.next();
            JsonNode jobNode = jobsNode.path(jobName);

            // job block
            ObjectNode jobBlock = mapper.createObjectNode();
            jobBlock.put("type", "job");
            jobBlock.put("job-name", jobName);
            jobBlock.put("name", jobNode.path("x_name").asText(jobName));
            jobBlock.put("description", jobNode.path("x_description").asText(""));

            // config에서 steps, x_name, x_description 제외
            ObjectNode jobConfigNode = jobNode.deepCopy();
            jobConfigNode.remove(Arrays.asList("steps", "x_name", "x_description"));
            jobBlock.set("config", jobConfigNode);

            blockList.add(jobBlock);

            // step blocks
            JsonNode steps = jobNode.path("steps");
            if (steps.isArray()) {
                for (JsonNode step : steps) {
                    ObjectNode stepBlock = mapper.createObjectNode();
                    stepBlock.put("type", "step");
                    stepBlock.put("job-name", jobName);
                    stepBlock.put("name", step.path("x_name").asText(step.path("name").asText("이름 없음")));
                    stepBlock.put("description", step.path("x_description").asText(""));
                    stepBlock.put("domain", step.path("x_domain").asText(""));
                    stepBlock.set("task", step.path("x_task").isMissingNode() ? mapper.createArrayNode() : step.path("x_task"));

                    // config는 x_로 시작하는 key를 제외하고 모음
                    ObjectNode configNode = mapper.createObjectNode();
                    step.fields().forEachRemaining(entry -> {
                        if (!entry.getKey().startsWith("x_")) {
                            configNode.set(entry.getKey(), entry.getValue());
                        }
                    });

                    stepBlock.set("config", configNode);
                    blockList.add(stepBlock);
                }
            }
        }

        return blockList;
    }
}