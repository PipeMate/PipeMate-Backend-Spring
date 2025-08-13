package com.example.pipemate.preset.res;

import com.example.pipemate.preset.entity.Block;
import com.example.pipemate.preset.entity.PipelineBlock;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BlockResponse {
    private Long id;
    private String name;
    private String type;  // "trigger", "job", "step"
    private String description;
    private JsonNode config;

    public static BlockResponse from(PipelineBlock pb) {
        Block block = pb.getBlock();

        switch (block.getType()) {
            case "job":
                return JobBlockResponse.builder()
                        .id(block.getId())
                        .name(block.getName())
                        .type(block.getType())
                        .description(block.getDescription())
                        .config(block.getConfig())
                        .jobName(pb.getJobName()) // PipelineBlock에서 가져오기
                        .build();

            case "step":
                return StepBlockResponse.builder()
                        .id(block.getId())
                        .name(block.getName())
                        .type(block.getType())
                        .description(block.getDescription())
                        .config(block.getConfig())
                        .jobName(pb.getJobName())
                        .domain(block.getDomain())
                        .task(block.getTask())
                        .build();

            case "trigger":
            default:
                return TriggerBlockResponse.builder()
                        .id(block.getId())
                        .name(block.getName())
                        .type(block.getType())
                        .description(block.getDescription())
                        .config(block.getConfig())
                        .build();
        }
    }
}