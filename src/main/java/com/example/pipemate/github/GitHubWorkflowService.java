package com.example.pipemate.github;

import com.example.pipemate.github.res.*;
import com.example.pipemate.util.GithubApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GitHubWorkflowService {

    private final GithubApiClient githubApiClient;

    public WorkflowListResponse getWorkflows(String owner, String repo, String token) {
        return githubApiClient.fetchWorkflowList(owner, repo, token);
    }

    public WorkflowDetailResponse getWorkflowDetail(String owner, String repo, String workflowId, String token) {
        return githubApiClient.fetchWorkflowDetail(owner, repo, workflowId, token);
    }

    public WorkflowRunListResponse getWorkflowRuns(String owner, String repo, String token) {
        return githubApiClient.fetchWorkflowRuns(owner, repo, token);
    }

    public WorkflowRunResponse getWorkflowRun(String owner, String repo, Long runId, String token) {
        return githubApiClient.fetchWorkflowRun(owner, repo, runId, token);
    }

    public String downloadAndExtractLogs(String owner, String repo, Long runId, String token) {
        return githubApiClient.downloadAndExtractLogs(owner, repo, runId, token);
    }

    public List<GithubJobDetailResponse> getWorkflowJobs(String owner, String repo, Long runId, String token) {
        return githubApiClient.fetchWorkflowJobs(owner, repo, runId, token);
    }

    public GithubJobDetailResponse getWorkflowJobDetail(String owner, String repo, Long jobId, String token) {
        return githubApiClient.fetchWorkflowJobDetail(owner, repo, jobId, token);
    }
}




