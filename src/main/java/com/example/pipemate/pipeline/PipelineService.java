//package com.example.pipemate.pipeline;
//
//import com.example.pipemate.pipeline.converter.JsonWorkflowConverter;
//import com.example.pipemate.pipeline.converter.YamlConverter;
//import com.example.pipemate.pipeline.req.PipelineRequest;
//import com.example.pipemate.pipeline.res.PipelineResponse;
//import com.example.pipemate.util.GithubApiClient;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PipelineService {
//
//    private final PipelineRepository pipelineRepository;
//    private final GithubApiClient githubApiClient;
//    private final JsonWorkflowConverter jsonWorkflowConverter;
//    private final YamlConverter yamlConverter;
//    private final ObjectMapper objectMapper;
//
//    public PipelineRequest convertAndSaveWorkflow(PipelineRequest request, String token) {
//        try {
//            log.info("Starting workflow conversion process for {}/{}", request.getOwner(), request.getRepo());
//
//            // 1. JSON 변환 (input.json -> res_input.json)
//            Map<String, Object> convertedJson = jsonWorkflowConverter.convertToWorkflowJson(request.getInputJson());
//
//            // 2. YAML 변환
//            String yamlContent = yamlConverter.convertJsonToYaml(convertedJson);
//
//            // 3. MongoDB에 저장
//            PipelineEntity entity = PipelineEntity.builder()
//                    .id(UUID.randomUUID().toString())
//                    .owner(request.getOwner())
//                    .repo(request.getRepo())
//                    .workflowName(request.getWorkflowName())
//                    .originalJson(request.getInputJson())
//                    .convertedJson(convertedJson)
//                    .yamlContent(yamlContent)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//
//            PipelineEntity savedEntity = pipelineRepository.save(entity);
//            log.info("Workflow saved to MongoDB with ID: {}", savedEntity.getId());
//
//            // 4. GitHub에 YAML 파일 업로드
//            String filePath = ".github/workflows/" + request.getWorkflowName() + ".yml";
//            githubApiClient.createOrUpdateFile(
//                    request.getOwner(),
//                    request.getRepo(),
//                    filePath,
//                    yamlContent,
//                    "Add/Update workflow: " + request.getWorkflowName(),
//                    token
//            );
//
//            log.info("Workflow uploaded to GitHub at path: {}", filePath);
//
//            return WorkflowConversionResponse.builder()
//                    .workflowId(savedEntity.getId())
//                    .owner(request.getOwner())
//                    .repo(request.getRepo())
//                    .workflowName(request.getWorkflowName())
//                    .originalJson(request.getInputJson())
//                    .convertedJson(convertedJson)
//                    .yamlContent(yamlContent)
//                    .githubPath(filePath)
//                    .createdAt(savedEntity.getCreatedAt())
//                    .updatedAt(savedEntity.getUpdatedAt())
//                    .success(true)
//                    .message("Workflow successfully converted, saved to MongoDB, and uploaded to GitHub")
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Error during workflow conversion process", e);
//            throw new RuntimeException("Failed to convert and save workflow: " + e.getMessage(), e);
//        }
//    }
//
//    public PipelineResponse getStoredWorkflow(String workflowId) {
//        PipelineEntity entity = pipelineRepository.findById(workflowId)
//                .orElseThrow(() -> new RuntimeException("Workflow not found with ID: " + workflowId));
//
//        return WorkflowConversionResponse.builder()
//                .workflowId(entity.getId())
//                .owner(entity.getOwner())
//                .repo(entity.getRepo())
//                .workflowName(entity.getWorkflowName())
//                .originalJson(entity.getOriginalJson())
//                .convertedJson(entity.getConvertedJson())
//                .yamlContent(entity.getYamlContent())
//                .githubPath(".github/workflows/" + entity.getWorkflowName() + ".yml")
//                .createdAt(entity.getCreatedAt())
//                .updatedAt(entity.getUpdatedAt())
//                .success(true)
//                .message("Workflow retrieved successfully")
//                .build();
//    }
//
//    public WorkflowConversionResponse updateStoredWorkflow(String workflowId, WorkflowConversionRequest request, String token) {
//        try {
//            WorkflowEntity existingEntity = workflowRepository.findById(workflowId)
//                    .orElseThrow(() -> new RuntimeException("Workflow not found with ID: " + workflowId));
//
//            log.info("Updating workflow: {} for {}/{}", workflowId, request.getOwner(), request.getRepo());
//
//            // 1. JSON 변환
//            Map<String, Object> convertedJson = jsonWorkflowConverter.convertToWorkflowJson(request.getInputJson());
//
//            // 2. YAML 변환
//            String yamlContent = yamlConverter.convertJsonToYaml(convertedJson);
//
//            // 3. MongoDB 업데이트
//            existingEntity.setWorkflowName(request.getWorkflowName());
//            existingEntity.setOriginalJson(request.getInputJson());
//            existingEntity.setConvertedJson(convertedJson);
//            existingEntity.setYamlContent(yamlContent);
//            existingEntity.setUpdatedAt(LocalDateTime.now());
//
//            WorkflowEntity updatedEntity = workflowRepository.save(existingEntity);
//
//            // 4. GitHub 업데이트
//            String filePath = ".github/workflows/" + request.getWorkflowName() + ".yml";
//            githubApiClient.createOrUpdateFile(
//                    request.getOwner(),
//                    request.getRepo(),
//                    filePath,
//                    yamlContent,
//                    "Update workflow: " + request.getWorkflowName(),
//                    token
//            );
//
//            log.info("Workflow updated and re-uploaded to GitHub");
//
//            return WorkflowConversionResponse.builder()
//                    .workflowId(updatedEntity.getId())
//                    .owner(request.getOwner())
//                    .repo(request.getRepo())
//                    .workflowName(request.getWorkflowName())
//                    .originalJson(request.getInputJson())
//                    .convertedJson(convertedJson)
//                    .yamlContent(yamlContent)
//                    .githubPath(filePath)
//                    .createdAt(updatedEntity.getCreatedAt())
//                    .updatedAt(updatedEntity.getUpdatedAt())
//                    .success(true)
//                    .message("Workflow successfully updated in MongoDB and GitHub")
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Error during workflow update process", e);
//            throw new RuntimeException("Failed to update workflow: " + e.getMessage(), e);
//        }
//    }
//
//    public void deleteStoredWorkflow(String workflowId, String owner, String repo, String token) {
//        try {
//            WorkflowEntity entity = workflowRepository.findById(workflowId)
//                    .orElseThrow(() -> new RuntimeException("Workflow not found with ID: " + workflowId));
//
//            // 1. GitHub에서 파일 삭제
//            String filePath = ".github/workflows/" + entity.getWorkflowName() + ".yml";
//            githubApiClient.deleteFile(owner, repo, filePath, "Delete workflow: " + entity.getWorkflowName(), token);
//
//            // 2. MongoDB에서 삭제
//            workflowRepository.deleteById(workflowId);
//
//            log.info("Workflow deleted from both MongoDB and GitHub: {}", workflowId);
//
//        } catch (Exception e) {
//            log.error("Error during workflow deletion process", e);
//            throw new RuntimeException("Failed to delete workflow: " + e.getMessage(), e);
//        }
//    }
//}