package com.example.pipemate.preset.res;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StepBlockResponse extends BlockResponse {
    private String jobName;
    private String domain;
    private String[] task;
}