package com.example.pipemate.preset.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pipeline_block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineBlock {

    @EmbeddedId
    private PipelineBlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pipelineId")
    @JoinColumn(name = "pipeline_id")
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockId")
    @JoinColumn(name = "block_id")
    private Block block;

    private Integer position;
}
