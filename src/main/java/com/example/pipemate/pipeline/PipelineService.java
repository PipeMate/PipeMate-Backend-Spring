package com.example.pipemate.pipeline;

import com.example.pipemate.pipeline.converter.JsonWorkflowConverter;
import com.example.pipemate.pipeline.converter.YamlConverter;
import com.example.pipemate.pipeline.req.PipelineRequest;
import com.example.pipemate.pipeline.res.PipelineResponse;
import com.example.pipemate.util.GithubApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private final GithubApiClient githubApiClient;
    private final JsonWorkflowConverter jsonWorkflowConverter;
    private final YamlConverter yamlConverter;

    public void convertAndSaveWorkflow(PipelineRequest request, String token) {
        try {
            log.info("Starting workflow conversion process for {}/{}", request.getOwner(), request.getRepo());

            // 1. JSON 변환 (input.json -> res_input.json)
            Map<String, Object> convertedJson = jsonWorkflowConverter.convertToWorkflowJson(request.getInputJson());

            // 2. YAML 변환
            String yamlContent = yamlConverter.convertJsonToYaml(convertedJson);

            // 3. GitHub에 YAML 파일 업로드
            String filePath = ".github/workflows/" + request.getWorkflowName() + ".yml";
            githubApiClient.createFile(
                    request.getOwner(),
                    request.getRepo(),
                    filePath,
                    yamlContent,
                    "Add/Update workflow: " + request.getWorkflowName(),
                    token
            );

            log.info("Workflow uploaded to GitHub at path: {}", filePath);

        } catch (Exception e) {
            log.error("Error during workflow conversion process", e);
            throw new RuntimeException("Failed to convert and upload workflow: " + e.getMessage(), e);
        }
    }

    public PipelineResponse getWorkflowFromGitHub(String owner, String repo, String workflowName, String token) {
        try {
            String path = ".github/workflows/" + workflowName + ".yml";

            // 1. GitHub에서 YML 가져오기
            String yamlContent = githubApiClient.getFileContent(owner, repo, path, token);

            // 2. YAML → 일반 JSON Map
            Map<String, Object> convertedJson = yamlConverter.convertYamlToJson(yamlContent);

            // 3. 일반 JSON → 블록 기반 JSON
            List<JsonNode> originalJson = jsonWorkflowConverter.convertWorkflowJsonToBlocks(convertedJson);

            // 4. 응답 생성
            return PipelineResponse.builder()
                    .workflowName(workflowName)
                    .originalJson(originalJson)          // 이제 블록기반 JSON 포함
                    .githubPath(path)
                    .success(true)
                    .message("Workflow loaded and parsed from GitHub")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load workflow from GitHub: " + e.getMessage(), e);
        }
    }

    public PipelineResponse updateWorkflowOnGitHub(PipelineRequest request, String token) {
        try {
            log.info("Updating GitHub workflow for {}/{}", request.getOwner(), request.getRepo());

            // 1. JSON 변환
            Map<String, Object> convertedJson = jsonWorkflowConverter.convertToWorkflowJson(request.getInputJson());

            // 2. YAML 변환
            String yamlContent = yamlConverter.convertJsonToYaml(convertedJson);

            // 3. GitHub에 업로드 (덮어쓰기)
            // workflowName 와 ymlFileName 은 같은 의미이다.
            String filePath = ".github/workflows/" + request.getWorkflowName() + ".yml";
            githubApiClient.updateFile(
                    request.getOwner(),
                    request.getRepo(),
                    filePath,
                    yamlContent,
                    "Update workflow: " + request.getWorkflowName(),
                    token
            );

            log.info("Workflow updated and uploaded to GitHub at {}", filePath);

            return PipelineResponse.builder()
                    .workflowId(null)  // DB 사용 안 하므로 null
                    .workflowName(request.getWorkflowName())
                    .originalJson(request.getInputJson())
                    .githubPath(filePath)
                    .createdAt(null)
                    .updatedAt(LocalDateTime.now())
                    .success(true)
                    .message("Workflow successfully updated on GitHub")
                    .build();

        } catch (Exception e) {
            log.error("Error during workflow update process", e);
            throw new RuntimeException("Failed to update workflow: " + e.getMessage(), e);
        }
    }

    public void deleteWorkflowFromGitHub(String ymlFileName, String owner, String repo, String token) {
        try {
            // GitHub에서 파일 삭제
            String filePath = ".github/workflows/" + ymlFileName + ".yml";
            githubApiClient.deleteFile(owner, repo, filePath, "Delete workflow: " + ymlFileName, token);

            log.info("Workflow deleted from GitHub: {}", filePath);

        } catch (Exception e) {
            log.error("Error during GitHub workflow deletion process", e);
            throw new RuntimeException("Failed to delete workflow from GitHub: " + e.getMessage(), e);
        }
    }
}