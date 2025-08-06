package com.example.pipemate.preset;

import com.example.pipemate.pipeline.res.PipelineResponse;
import com.example.pipemate.preset.res.BlockResponse;
import com.example.pipemate.preset.service.BlockPresetService;
import com.example.pipemate.preset.service.PipelinePresetService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/presets")
@AllArgsConstructor
public class PresetController {

    private final BlockPresetService blockPresetService;
    private final PipelinePresetService pipelinePresetService;

    // 전체 블록 조회
    @GetMapping("/blocks")
    public List<BlockResponse> getAllBlocks() {
        return blockPresetService.getAllBlocks();
    }

    // 전체 파이프라인 조회
    @GetMapping("/pipelines")
    public List<PipelineResponse> getAll() {
        return pipelinePresetService.getAllPipelineResponses();
    }
}