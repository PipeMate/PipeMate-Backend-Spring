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
     * 원본 JSON 블록(List<JsonNode>)을 GitHub Actions Workflow 형식의 JSON(Map)으로 변환한다.
     * <p>
     * 입력되는 블록 리스트는 "trigger" / "job" / "step" 등의 타입을 갖고 있으며,
     * 이를 GitHub Actions가 인식하는 on/jobs/steps 구조로 매핑한다.
     * <p>
     * 처리 순서:
     * 1. 반복문을 돌며 각 블록의 type에 따라 트리거/잡/스텝 구조 생성
     * 2. jobs 와 steps를 별도의 매핑(Map<String, ArrayNode>)으로 관리
     * 3. 최종적으로 jobs에 steps를 합쳐 루트 Workflow JSON 구성
     * 4. GitHub Workflow 규칙에 맞는 기본값 보정 (job이 없는 경우 default job 생성)
     */
    public Map<String, Object> convertToWorkflowJson(List<JsonNode> inputJsonBlocks) {
        try {
            // 워크플로우 최상위 노드와 jobs 노드 초기화
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode jobs = objectMapper.createObjectNode();

            // jobId → steps Array 매핑을 저장할 Map
            Map<String, ArrayNode> jobStepsMap = new HashMap<>();

            log.info("Starting JSON conversion process with {} blocks", inputJsonBlocks.size());

            // 모든 블록에 대해 GitHub Workflow 구조로 변환
            for (JsonNode block : inputJsonBlocks) {
                String type = block.path("type").asText(null);

                JsonNode config = block.path("config");

                switch (type) {
                    case "trigger": // trigger 블록 생성
                        root.put("name", config.path("name").asText());
                        root.set("on", config.get("on"));
                        // 확장 메타데이터(x_*)는 편집 UI를 위한 부가 설명
                        root.put("x_name", block.path("name").asText(""));
                        root.put("x_description", block.path("description").asText(""));
                        log.debug("Processed trigger block: {}", config.path("name").asText());
                        break;

                    case "job": { // job 블록 생성
                        String jobId = block.path("job-name").asText("ci-pipeline");
                        ObjectNode jobNode = objectMapper.createObjectNode();

                        config.fieldNames().forEachRemaining(fieldName -> {
                            if (!fieldName.equals("steps")) {
                                jobNode.set(fieldName, config.get(fieldName));
                            }
                        });
                        // 확장 메타데이터(x_*)는 편집 UI를 위한 부가 설명
                        jobNode.put("x_name", block.path("name").asText(""));
                        jobNode.put("x_description", block.path("description").asText(""));

                        jobs.set(jobId, jobNode);
                        jobStepsMap.put(jobId, objectMapper.createArrayNode());
                        log.debug("Processed job block with jobName: {}", jobId);
                        break;
                    }

                    case "step": { // step 블록 생성
                        String targetJobId = block.path("job-name").asText("ci-pipeline");

                        if (!jobs.has(targetJobId)) {
                            ObjectNode defaultJob = objectMapper.createObjectNode();
                            defaultJob.put("runs-on", "ubuntu-latest");
                            jobs.set(targetJobId, defaultJob);
                            jobStepsMap.put(targetJobId, objectMapper.createArrayNode());
                        }

                        ArrayNode stepList = jobStepsMap.get(targetJobId);

                        ObjectNode configCopy = config.deepCopy();
                        // 확장 메타데이터(x_*)는 편집 UI를 위한 부가 설명
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

            // jobStepsMap에서 steps를 jobs에 최종 병합
            jobStepsMap.forEach((jobName, steps) -> {
                if (jobs.has(jobName)) {
                    ObjectNode jobNode = (ObjectNode) jobs.get(jobName);
                    jobNode.set("steps", steps);
                }
            });

            // jobs를 root에 추가하고 반환
            root.set("jobs", jobs);
            log.info("JSON conversion completed successfully");

            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Error during JSON conversion", e);
            throw new RuntimeException("Failed to convert JSON workflow: " + e.getMessage(), e);
        }
    }

    /**
     * GitHub Workflow YAML(JSON 변환된 Map)을 블록 구조(List<JsonNode>)로 변환한다.
     * <p>
     * GitHub Actions 워크플로우는 on/jobs/steps 구조를 가지는데,
     * 이를 프론트엔드에서 블록 단위(Trigger, Job, Step)로 편집할 수 있는 형태로 변환한다.
     *
     * 변환 규칙:
     * 1. 루트(name, on)를 Trigger 블록으로 변환
     * 2. jobs 하위의 각 job을 Job 블록으로 변환 (steps 제외)
     * 3. 각 job의 steps를 Step 블록으로 변환
     *    - 확장 메타데이터(x_*)는 블록의 name, description, domain, task 등으로 매핑
     *    - config에는 x_로 시작하는 키들을 제외한 설정 값만 포함
     */
    public List<JsonNode> convertWorkflowJsonToBlocks(Map<String, Object> yamlJsonMap) {
        List<JsonNode> blockList = new ArrayList<>();

        // ObjectMapper 생성
        ObjectMapper mapper = new ObjectMapper();

        // Convert Map to JsonNode
        JsonNode root = mapper.convertValue(yamlJsonMap, JsonNode.class);

        // 1. trigger 블록 생성
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


        // 2. job 블록 및 step 블록 생성
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