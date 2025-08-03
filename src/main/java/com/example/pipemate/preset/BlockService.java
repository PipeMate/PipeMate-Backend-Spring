package com.example.pipemate.preset;

import com.example.pipemate.preset.res.BlockResponse;
import com.example.pipemate.preset.res.JobBlockResponse;
import com.example.pipemate.preset.res.StepBlockResponse;
import com.example.pipemate.preset.res.TriggerBlockResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BlockService {

    private final BlockRepository blockRepository;

    public BlockService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public List<BlockResponse> getAllBlocks() {
        return blockRepository.findAll().stream()
                .map(block -> {
                    String type = block.getType();
                    switch (type) {
                        case "trigger":
                            return TriggerBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(block.getConfig())
                                    .build();

                        case "job":
                            return JobBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(block.getConfig())
                                    .jobName(block.getJobName())
                                    .build();

                        case "step":
                            return StepBlockResponse.builder()
                                    .id(block.getId())
                                    .name(block.getName())
                                    .type(type)
                                    .description(block.getDescription())
                                    .config(block.getConfig())
                                    .jobName(block.getJobName())
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