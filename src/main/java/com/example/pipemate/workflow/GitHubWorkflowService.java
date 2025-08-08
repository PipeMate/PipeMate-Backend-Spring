package com.example.pipemate.workflow;

import com.example.pipemate.util.GithubApiClient;
import com.example.pipemate.workflow.res.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubWorkflowService {

    private final GithubApiClient githubApiClient;

    @Cacheable("workflow-file-list") // 깃허브 워크플로우 파일 목록 캐싱 적용
    public WorkflowListResponse getWorkflows(String owner, String repo, String token) {
        log.info("[GitHubWorkflowService] 워크플로우 파일 목록 조회");
        return githubApiClient.fetchWorkflowList(owner, repo, token);
    }

    public WorkflowDetailResponse getWorkflowDetail(String owner, String repo, String workflowId, String token) {
        return githubApiClient.fetchWorkflowDetail(owner, repo, workflowId, token);
    }

    public WorkflowRunListResponse getWorkflowRuns(String owner, String repo, String token) {
        return githubApiClient.fetchWorkflowRuns(owner, repo, token);
    }

    @Cacheable("workflow-run-detail") // 깃허브 워크플로우 실행 상세 정보 조회 캐싱 적용
    public WorkflowRunResponse getWorkflowRun(String owner, String repo, Long runId, String token) {
        log.info("[GitHubWorkflowService] 워크플로우 실행 상세 정보 조회");
        return githubApiClient.fetchWorkflowRun(owner, repo, runId, token);
    }

    @Cacheable("workflow-run-log") // 깃허브 워크플로우 실행의 텍스트 로그 조회 캐싱 적용
    public String downloadAndExtractLogs(String owner, String repo, Long runId, String token) {
        return githubApiClient.downloadAndExtractLogs(owner, repo, runId, token);
    }

    public List<GithubJobDetailResponse> getWorkflowJobs(String owner, String repo, Long runId, String token) {
        return githubApiClient.fetchWorkflowJobs(owner, repo, runId, token);
    }

    public GithubJobDetailResponse getWorkflowJobDetail(String owner, String repo, Long jobId, String token) {
        return githubApiClient.fetchWorkflowJobDetail(owner, repo, jobId, token);
    }

    public void dispatchWorkflow(String owner, String repo, String ymlFileName, String ref, String token) {
        githubApiClient.dispatchWorkflow(owner, repo, ymlFileName, ref, token);
    }

    public void cancelWorkflowRun(String owner, String repo, Long runId, String token) {
        githubApiClient.cancelWorkflowRun(owner, repo, runId, token);
    }
}




