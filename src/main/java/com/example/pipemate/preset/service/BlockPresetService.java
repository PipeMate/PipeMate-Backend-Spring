package com.example.pipemate.preset.service;

import com.example.pipemate.preset.repository.BlockRepository;
import com.example.pipemate.preset.res.BlockResponse;
import com.example.pipemate.preset.res.JobBlockResponse;
import com.example.pipemate.preset.res.StepBlockResponse;
import com.example.pipemate.preset.res.TriggerBlockResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockPresetService {

    private final BlockRepository blockRepository;
    private final ObjectMapper objectMapper;

    public BlockPresetService(BlockRepository blockRepository, ObjectMapper objectMapper) {
        this.blockRepository = blockRepository;
        this.objectMapper = objectMapper;
    }

    public List<BlockResponse> getAllBlocks() {
        return blockRepository.findAll().stream()
                .map(block -> {
                    String type = block.getType();
                    String raw = block.getConfigRawForView(objectMapper);

                    switch (type) {
                        case "trigger":
                            return TriggerBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(raw)
                                    .build();

                        case "job":
                            return JobBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(raw)
                                    .jobName(block.getJobName())
                                    .build();

                        case "step":
                            return StepBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(raw)
                                    .domain(block.getDomain())
                                    .task(block.getTask())
                                    .build();

                        default:
                            throw new IllegalArgumentException("Unsupported block type: " + type);
                    }
                })
                .toList();
    }
}