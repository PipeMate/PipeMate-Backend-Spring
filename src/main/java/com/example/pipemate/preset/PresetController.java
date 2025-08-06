package com.example.pipemate.preset;

import com.example.pipemate.pipeline.res.PipelineResponse;
import com.example.pipemate.preset.res.BlockResponse;
import com.example.pipemate.preset.service.BlockPresetService;
import com.example.pipemate.preset.service.PipelinePresetService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/presets")
@AllArgsConstructor
public class PresetController {

    private final BlockPresetService blockPresetService;
    private final PipelinePresetService pipelinePresetService;

    // 전체 블록 조회
    @GetMapping("/blocks")
    @Operation(summary = "블록 프리셋 조회", description = "블록 프리셋을 조회합니다.")
    public List<BlockResponse> getAllBlocks() {
        return blockPresetService.getAllBlocks();
    }

    // 전체 파이프라인 조회
    @GetMapping("/pipelines")
    @Operation(summary = "파이프라인 프리셋 조회", description = "파이프라인 프리셋을 조회합니다.")
    public List<PipelineResponse> getAll() {
        return pipelinePresetService.getAllPipelineResponses();
    }
}