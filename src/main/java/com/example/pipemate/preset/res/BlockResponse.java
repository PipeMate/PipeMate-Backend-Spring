package com.example.pipemate.preset.res;

import com.example.pipemate.preset.entity.Block;
import com.example.pipemate.preset.entity.PipelineBlock;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static ObjectMapper mapper;

    /** 순서 보존용 RAW JSON. 응답에 그대로 삽입됨 */
    @JsonRawValue
    private String config; // <- JsonNode -> String + @JsonRawValue 로 변경

    public static BlockResponse from(PipelineBlock pb) {
        Block block = pb.getBlock();
        String raw = block.getConfigRawForView(mapper);

        switch (block.getType()) {
            case "job":
                return JobBlockResponse.builder()
                        .id(block.getId())
                        .name(block.getName())
                        .type(block.getType())
                        .description(block.getDescription())
                        .config(raw)
                        .jobName(pb.getJobName()) // PipelineBlock에서 가져오기
                        .build();

            case "step":
                return StepBlockResponse.builder()
                        .id(block.getId())
                        .name(block.getName())
                        .type(block.getType())
                        .description(block.getDescription())
                        .config(raw)
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
                        .config(raw)
                        .build();
        }
    }
}