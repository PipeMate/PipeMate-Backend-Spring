package com.example.pipemate.preset.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PipelineBlockId implements Serializable {

    @Column(name = "pipeline_id")
    private Long pipelineId;

    @Column(name = "block_id")
    private Long blockId;
}