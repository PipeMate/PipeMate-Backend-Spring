package com.example.pipemate.github;

import com.example.pipemate.github.res.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubWorkflowController {

    private final GitHubWorkflowService gitHubWorkflowService;

    @GetMapping("/workflows")
    @Operation(summary = "특정 레포지토리의 워크플로우 목록 조회",
            description = "레포지토리 소유자와 레포지토리 이름을 기반으로 워크플로우 목록을 조회합니다. ")
    public ResponseEntity<WorkflowListResponse> getWorkflows(
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");

        log.info("owner: {}", owner);
        log.info("repo: {}", repo);
        log.info("Authorization header: {}", token);

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        // "Bearer " 접두사 제거 후 깔끔하게 token 정리
        String cleanToken = token.substring("Bearer ".length()).trim();

        WorkflowListResponse workflows = gitHubWorkflowService.getWorkflows(owner, repo, cleanToken);
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/workflows/{workflowId}")
    @Operation(summary = "특정 워크플로우 상세 정보 조회",
            description = "워크플로우 ID를 기반으로 워크플로우의 상세 정보를 조회합니다.")
    public ResponseEntity<WorkflowDetailResponse> getWorkflowDetail(
            @RequestParam String owner,
            @RequestParam String repo,
            @PathVariable("workflowId") String workflowId,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.substring("Bearer ".length()).trim();
        WorkflowDetailResponse response = gitHubWorkflowService.getWorkflowDetail(owner, repo, workflowId, cleanToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflow-runs")
    @Operation(summary = "특정 레포지토리의 워크플로우 실행 목록 조회")
    public ResponseEntity<WorkflowRunListResponse> getWorkflowRuns(
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.replace("Bearer ", "").trim();

        WorkflowRunListResponse response = gitHubWorkflowService.getWorkflowRuns(owner, repo, cleanToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflow-run")
    @Operation(summary = "특정 워크플로우 실행 상세 정보 조회")
    public ResponseEntity<WorkflowRunResponse> getWorkflowRun(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Long runId,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();
        WorkflowRunResponse response = gitHubWorkflowService.getWorkflowRun(owner, repo, runId, cleanToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflow-run/logs/raw")
    @Operation(summary = "특정 워크플로우 실행 로그 텍스트 반환",
            description = "실행 ID를 통해 압축된 로그를 다운로드하고 압축을 해제해 텍스트 형태로 반환합니다.")
    public ResponseEntity<String> getWorkflowRunLogsText(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Long runId,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.substring("Bearer ".length()).trim();

        String logsText = gitHubWorkflowService.downloadAndExtractLogs(owner, repo, runId, cleanToken);
        return ResponseEntity.ok().body(logsText);
    }

    @GetMapping("/workflow-run/jobs")
    @Operation(summary = "특정 워크플로우 실행 내 모든 Job 상세 정보 조회",
            description = "runId를 기준으로 모든 Job 상세 정보 (status, steps 등)를 반환합니다.")
    public ResponseEntity<List<GithubJobDetailResponse>> getJobsForWorkflowRun(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Long runId,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.substring("Bearer ".length()).trim();
        List<GithubJobDetailResponse> jobs = gitHubWorkflowService.getWorkflowJobs(owner, repo, runId, cleanToken);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/workflow-run/job")
    @Operation(summary = "특정 워크플로우 Job 상세 정보 조회",
            description = "jobId를 기준으로 해당 Job의 상태, conclusion, steps 등의 상세 정보를 조회합니다.")
    public ResponseEntity<GithubJobDetailResponse> getJobDetail(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Long jobId,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.substring("Bearer ".length()).trim();
        GithubJobDetailResponse jobDetail = gitHubWorkflowService.getWorkflowJobDetail(owner, repo, jobId, cleanToken);
        return ResponseEntity.ok(jobDetail);
    }
}


