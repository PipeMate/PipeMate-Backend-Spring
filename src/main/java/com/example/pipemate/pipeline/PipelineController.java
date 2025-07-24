package com.example.pipemate.pipeline;

import com.example.pipemate.pipeline.req.PipelineRequest;
import com.example.pipemate.pipeline.res.PipelineResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping
    @Operation(summary = "파이프라인(워크플로우) 블록 리스트를 받아서 YML 파일로 변환 후 깃허브에 업로드(생성)",
            description = "*해당 이름의 파이프라인(워크플로우)가 GitHub에 존재하는 경우 에러를 발생시킵니다.")
    public ResponseEntity<String> convertAndSaveWorkflow(
            @RequestBody PipelineRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Converting workflow for owner: {}, repo: {}", request.getOwner(), request.getRepo());

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        pipelineService.convertAndSaveWorkflow(request, cleanToken);
        return ResponseEntity.ok().body("Workflow conversion and upload on github successful");
    }

    @GetMapping("/{ymlFileName}")
    @Operation(summary = "특정 파이프라인(워크플로우)을 블록 리스트 형태로 조회",
            description = "Github에 저장된 특정 파이프라인(워크플로우) 정보를 블록 형태로 가공하여 반환합니다.")
    public ResponseEntity<PipelineResponse> getStoredWorkflow(
            @PathVariable String ymlFileName,
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest httpRequest
    ) {
        log.info("Retrieving stored ymlFile, name: {}", ymlFileName);
        String token = httpRequest.getHeader("Authorization");
        String cleanToken = token.substring("Bearer ".length()).trim();

        PipelineResponse response = pipelineService.getWorkflowFromGitHub(owner, repo, ymlFileName, cleanToken);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "깃허브에 저장된 특정 파이프라인(워크플로우) 업데이트",
            description = "*변경하고자 하는 이름의 파이프라인(워크플로우)이 GitHub에 존재하는 경우 에러를 발생시킵니다.")
    public ResponseEntity<PipelineResponse> updateStoredWorkflow(
            @RequestBody PipelineRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Updating workflow: {} for owner: {}, repo: {}", request.getWorkflowName(), request.getOwner(), request.getRepo());

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        PipelineResponse response = pipelineService.updateWorkflowOnGitHub(request, cleanToken);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ymlFileName}")
    @Operation(summary = "저장된 특정 파이프라인(워크플로우) 삭제",
            description = "깃허브에 저장된 특정 파이프라인(워크플로우)를 삭제합니다.")
    public ResponseEntity<Void> deleteStoredWorkflow(
            @PathVariable String ymlFileName,
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Deleting workflow: {} for owner: {}, repo: {}", ymlFileName, owner, repo);

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        pipelineService.deleteWorkflowFromGitHub(ymlFileName, owner, repo, cleanToken);
        return ResponseEntity.noContent().build();
    }
}