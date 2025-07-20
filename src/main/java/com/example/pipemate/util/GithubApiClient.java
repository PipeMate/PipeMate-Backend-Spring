package com.example.pipemate.util;

import com.example.pipemate.workflow.res.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class GithubApiClient {

    private final RestTemplate restTemplate;

    public GithubApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public WorkflowListResponse fetchWorkflowList(String owner, String repo, String token) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/actions/workflows";

        HttpHeaders headers = new HttpHeaders();
        System.out.println("token = " + token);
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<WorkflowListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WorkflowListResponse.class
        );

        return response.getBody();
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
}