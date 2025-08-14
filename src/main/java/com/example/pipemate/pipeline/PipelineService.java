package com.example.pipemate.pipeline;

import com.example.pipemate.pipeline.converter.JsonWorkflowConverter;
import com.example.pipemate.pipeline.converter.YamlConverter;
import com.example.pipemate.pipeline.req.PipelineRequest;
import com.example.pipemate.pipeline.res.PipelineResponse;
import com.example.pipemate.util.GithubApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

    private final GithubApiClient githubApiClient;
    private final JsonWorkflowConverter jsonWorkflowConverter;
    private final YamlConverter yamlConverter;

    /**
     * 요청받은 블록 기반 JSON 워크플로우 데이터를 GitHub Actions 워크플로우(YAML) 파일로 변환하여 업로드한다.
     * <p>
     * 처리 단계:
     * 1. JSON → GitHub Actions 워크플로우 JSON 구조로 변환
     * 2. JSON → YAML 변환
     * 3. 변환된 YAML을 `.github/workflows/` 경로에 파일로 업로드
     */
    @CacheEvict(value = "workflow-file-list", allEntries = true) // 깃허브 워크플로우 파일 목록 캐싱 초기화
    public void convertAndSaveWorkflow(PipelineRequest request, String token) {
        try {
            log.info("Starting workflow conversion process for {}/{}", request.getOwner(), request.getRepo());

            // 1. JSON 수동 변환 (input.json -> res_input.json)
            Map<String, Object> convertedJson = jsonWorkflowConverter.convertToWorkflowJson(request.getInputJson());

            // 2. YAML 자동 변환 (snakeYAML 사용)
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

    /**
     * GitHub 저장소에서 지정된 워크플로우(YAML)를 가져와
     * 블록 기반 JSON 구조(PipelineBlock)로 변환한 후 응답 객체로 반환한다.
     * <p>
     * 처리 순서:
     * 1. GitHub API를 통해 `.github/workflows/{workflowName}.yml` 파일 내용 조회
     * 2. YAML → 일반 JSON(Map 구조) 변환
     * 3. 일반 JSON → 블록 기반 JSON(List<JsonNode>) 변환
     * 4. 변환 결과를 PipelineResponse로 래핑 후 반환
     */
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
                    .success(true)
                    .message("Workflow loaded and parsed from GitHub")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load workflow from GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * GitHub에 저장된 특정 워크플로우(YAML) 파일을 업데이트한다.
     * <p>
     * 처리 순서:
     * 1. 입력으로 받은 블록 기반 JSON(workflow 블록 구조)을 GitHub Actions 호환 JSON으로 변환
     * 2. JSON을 YAML 포맷으로 변환
     * 3. 변환된 YAML 파일을 `.github/workflows/{workflowName}.yml` 경로에 덮어쓰기 방식으로 업로드
     * 4. 캐시되어 있던 워크플로우 목록(`workflow-file-list`)은 무효화(@CacheEvict) 처리
     */
    @CacheEvict(value = "workflow-file-list", allEntries = true) // 깃허브 워크플로우 파일 목록 캐싱 초기화
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
                    .success(true)
                    .message("Workflow successfully updated on GitHub")
                    .build();

        } catch (Exception e) {
            log.error("Error during workflow update process", e);
            throw new RuntimeException("Failed to update workflow: " + e.getMessage(), e);
        }
    }

    @CacheEvict(value = "workflow-file-list", allEntries = true) // 깃허브 워크플로우 파일 목록 캐싱 초기화
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