package com.example.pipemate.util;

import com.example.pipemate.workflow.WorkflowItem;
import com.example.pipemate.workflow.res.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
@RequiredArgsConstructor
public class GithubApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GithubApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public WorkflowListResponse fetchWorkflowList(String owner, String repo, String token) {
        String listUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/workflows";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(listUrl, HttpMethod.GET, entity, JsonNode.class);

        int totalCount = response.getBody().get("total_count").asInt();
        List<WorkflowItem> workflowItems = new ArrayList<>();

        for (JsonNode workflow : response.getBody().get("workflows")) {
            WorkflowItem item = new WorkflowItem();
            item.setId(workflow.get("id").asLong());
            item.setName(workflow.get("name").asText());
            item.setPath(workflow.get("path").asText());
            item.setState(workflow.get("state").asText());
            item.setCreatedAt(workflow.get("created_at").asText());
            item.setUpdatedAt(workflow.get("updated_at").asText());
            item.setUrl(workflow.get("url").asText());
            item.setHtmlUrl(workflow.get("html_url").asText());
            item.setBadgeUrl(workflow.get("badge_url").asText());

            // .yml 파일 내용 파싱
            String fileUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + item.getPath();
            ResponseEntity<JsonNode> fileResponse = restTemplate.exchange(fileUrl, HttpMethod.GET, entity, JsonNode.class);
            String encoded = fileResponse.getBody().get("content").asText();
            String cleaned = encoded.replaceAll("\\s+", "");
            String decodedYaml = new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);

            item.setManualDispatchEnabled(decodedYaml.contains("workflow_dispatch"));
            item.setAvailableBranches(parseBranches(decodedYaml));

            workflowItems.add(item);
        }

        WorkflowListResponse result = new WorkflowListResponse();
        result.setTotalCount(totalCount);
        result.setWorkflows(workflowItems);
        return result;
    }

    private List<String> parseBranches(String yamlText) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> parsed = yaml.load(yamlText);
            Object on = parsed.get("on");

            if (on instanceof Map<?, ?> onMap) {
                Object push = onMap.get("push");

                if (push instanceof Map) {
                    Object branches = ((Map<?, ?>) push).get("branches");

                    if (branches instanceof List<?>) {
                        return ((List<?>) branches).stream().map(Object::toString).collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("브랜치 파싱 실패: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public WorkflowDetailResponse fetchWorkflowDetail(String owner, String repo, String workflowId, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/workflows/" + workflowId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<WorkflowDetailResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WorkflowDetailResponse.class
        );

        return response.getBody();
    }

    public WorkflowRunListResponse fetchWorkflowRuns(String owner, String repo, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<WorkflowRunListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WorkflowRunListResponse.class
        );

        return response.getBody();
    }

    public WorkflowRunResponse fetchWorkflowRun(String owner, String repo, Long runId, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs/" + runId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<WorkflowRunResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WorkflowRunResponse.class
        );

        return response.getBody();
    }

    public String downloadAndExtractLogs(String owner, String repo, Long runId, String token) {
        try {
            // 1. GitHub 로그 API URL
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/logs";

            // 2. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 3. .zip 파일 요청 (리다이렉션이든 바로 오든 모두 대응)
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("GitHub 로그 다운로드 실패: " + response.getStatusCode());
            }

            // 4. 임시 파일 저장
            String zipPath = "/tmp/github_logs_" + runId + ".zip";
            Files.write(Paths.get(zipPath), response.getBody());

            // 5. 압축 해제
            String extractDir = "/tmp/github_logs_" + runId;
            unzip(zipPath, extractDir);

            // 6. 로그 파일 읽기
            Path logFilePath = Files.walk(Paths.get(extractDir))
                    .filter(path -> path.toString().endsWith(".txt"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("로그 파일을 찾을 수 없습니다."));

            return Files.readString(logFilePath, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("로그 다운로드 또는 압축 해제 중 오류", e);
        }
    }

    private void unzip(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = Paths.get(destDir, entry.getName());
                Files.createDirectories(filePath.getParent());
                Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public List<GithubJobDetailResponse> fetchWorkflowJobs(String owner, String repo, Long runId, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode jobsArray = response.getBody().get("jobs");
        List<GithubJobDetailResponse> jobDetails = new ArrayList<>();
        if (jobsArray != null && jobsArray.isArray()) {
            for (JsonNode job : jobsArray) {
                jobDetails.add(GithubJobDetailResponse.from(job));
            }
        }
        return jobDetails;
    }

    public GithubJobDetailResponse fetchWorkflowJobDetail(String owner, String repo, Long jobId, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/jobs/" + jobId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode jobNode = response.getBody();
        return GithubJobDetailResponse.from(jobNode);
    }

    public void createFile(String owner, String repo, String path, String content, String message, String token) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);

            // 1. 파일 존재 여부 확인
            if (fileExists(owner, repo, path, token)) {
                throw new RuntimeException("파일이 이미 존재합니다. 다른 이름을 사용하거나 updateFile을 호출하세요.");
            }

            // 2. 생성 요청 바디 구성
            ObjectNode body = objectMapper.createObjectNode();
            body.put("message", message);
            body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            // 3. 파일 생성 요청
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("파일 생성 성공: {}", path);
            } else {
                throw new RuntimeException("파일 생성 실패: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("파일 생성 중 오류 발생", e);
            throw new RuntimeException("파일 생성 실패: " + e.getMessage(), e);
        }
    }

    public void updateFile(String owner, String repo, String path, String content, String message, String token) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);

            // 1. 파일 SHA 가져오기
            String existingSha = getFileSha(owner, repo, path, token);
            if (existingSha == null) {
                throw new RuntimeException("업데이트 실패: 해당 파일이 존재하지 않습니다.");
            }

            // 2. 요청 바디 구성
            ObjectNode body = objectMapper.createObjectNode();
            body.put("message", message);
            body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            body.put("sha", existingSha); // 반드시 필요

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            // 3. PUT 요청 (업데이트)
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("파일 업데이트 성공: {}", path);
            } else {
                throw new RuntimeException("파일 업데이트 실패: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("파일 업데이트 중 오류 발생", e);
            throw new RuntimeException("파일 업데이트 실패: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String owner, String repo, String path, String message, String token) {
        try {
            // 1. 삭제할 파일의 SHA 가져오기
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<String> getEntity = new HttpEntity<>(headers);
            ResponseEntity<String> getResponse = restTemplate.exchange(url, HttpMethod.GET, getEntity, String.class);

            if (!getResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("File not found: " + path);
            }

            JsonNode responseNode = objectMapper.readTree(getResponse.getBody());
            String sha = responseNode.path("sha").asText();

            // 2. 파일 삭제 요청
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("message", message);
            requestBody.put("sha", sha);

            HttpHeaders deleteHeaders = new HttpHeaders();
            deleteHeaders.setBearerAuth(token);
            deleteHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> deleteEntity = new HttpEntity<>(requestBody.toString(), deleteHeaders);
            ResponseEntity<String> deleteResponse = restTemplate.exchange(url, HttpMethod.DELETE, deleteEntity, String.class);

            if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted file: {}", path);
            } else {
                throw new RuntimeException("Failed to delete file: " + deleteResponse.getBody());
            }

        } catch (Exception e) {
            log.error("Error deleting file from GitHub", e);
            throw new RuntimeException("Failed to delete GitHub file: " + e.getMessage(), e);
        }
    }

    public String getFileContent(String owner, String repo, String path, String token) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);
            log.info("Fetching file content from URL: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to get file content: " + response.getStatusCode());
            }

            // JSON 응답에서 content 필드 추출 및 디코딩
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String base64Content = rootNode.path("content").asText();

            // GitHub는 줄바꿈 포함된 base64 인코딩을 반환하므로, 줄바꿈 제거 후 디코딩
            byte[] decodedBytes = Base64.getMimeDecoder().decode(base64Content.replaceAll("\\s", ""));
            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error while fetching file content from GitHub", e);
            throw new RuntimeException("Failed to fetch file content from GitHub: " + e.getMessage(), e);
        }
    }

    public boolean fileExists(String owner, String repo, String path, String token) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return response.getStatusCode().is2xxSuccessful();  // 파일 존재함
        } catch (HttpClientErrorException.NotFound e) {
            return false;  // 파일 없음
        } catch (Exception e) {
            log.error("Error checking file existence", e);
            throw new RuntimeException("파일 존재 여부 확인 중 오류 발생", e);
        }
    }

    public String getFileSha(String owner, String repo, String path, String token) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = objectMapper.readTree(response.getBody());
                String sha = body.path("sha").asText(null);
                if (sha == null) {
                    throw new RuntimeException("SHA 값이 존재하지 않습니다.");
                }
                return sha;
            } else {
                throw new RuntimeException("파일 정보를 가져오는 데 실패했습니다: " + response.getBody());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("파일이 존재하지 않아 SHA를 가져올 수 없습니다: " + path);
        } catch (Exception e) {
            log.error("SHA 조회 중 오류 발생", e);
            throw new RuntimeException("SHA 조회 실패: " + e.getMessage(), e);
        }
    }

    public void dispatchWorkflow(String owner, String repo, String ymlFileName, String ref, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/workflows/" + ymlFileName + "/dispatches";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ref", ref); // 필수: 실행할 브랜치 이름 (예: "main")

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        restTemplate.postForEntity(url, entity, Void.class);
    }

    public void cancelWorkflowRun(String owner, String repo, Long runId, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/actions/runs/%d/cancel", owner, repo, runId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            log.info("Successfully sent cancel request to: {}", url);
        } catch (HttpClientErrorException e) {
            log.error("Failed to cancel workflow run: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
}