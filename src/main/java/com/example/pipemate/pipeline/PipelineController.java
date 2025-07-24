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
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final PipelineService workflowConversionService;

    @PostMapping("/convert")
    @Operation(summary = "워크플로우 생성 및 저장",
            description = "input.json을 받아서 res_input.json으로 변환하고, 이를 다시 YAML로 변환하여 GitHub에 업로드합니다.")
    public ResponseEntity<Void> convertAndSaveWorkflow(
            @RequestBody PipelineRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Converting workflow for owner: {}, repo: {}", request.getOwner(), request.getRepo());

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        workflowConversionService.convertAndSaveWorkflow(request, cleanToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workflows/{workflowId}")
    @Operation(summary = "저장된 워크플로우 조회",
            description = "MongoDB에 저장된 워크플로우 정보를 조회합니다.")
    public ResponseEntity<PipelineResponse> getStoredWorkflow(
            @PathVariable String workflowId
    ) {
        log.info("Retrieving stored workflow: {}", workflowId);

        PipelineResponse response = workflowConversionService.getStoredWorkflow(workflowId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/workflows/{workflowId}")
    @Operation(summary = "저장된 워크플로우 업데이트",
            description = "MongoDB에 저장된 워크플로우를 업데이트하고 GitHub에 재업로드합니다.")
    public ResponseEntity<PipelineResponse> updateStoredWorkflow(
            @PathVariable String workflowId,
            @RequestBody PipelineRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Updating workflow: {} for owner: {}, repo: {}", workflowId, request.getOwner(), request.getRepo());

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        PipelineResponse response = workflowConversionService.updateStoredWorkflow(workflowId, request, cleanToken);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/workflows/{workflowId}")
    @Operation(summary = "저장된 워크플로우 삭제",
            description = "MongoDB에서 워크플로우를 삭제하고 GitHub에서도 제거합니다.")
    public ResponseEntity<Void> deleteStoredWorkflow(
            @PathVariable String workflowId,
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization");

        log.info("Deleting workflow: {} for owner: {}, repo: {}", workflowId, owner, repo);

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();

        workflowConversionService.deleteStoredWorkflow(workflowId, owner, repo, cleanToken);
        return ResponseEntity.noContent().build();
    }
}