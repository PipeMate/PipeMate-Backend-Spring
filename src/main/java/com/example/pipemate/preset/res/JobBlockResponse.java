package com.example.pipemate.preset.res;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class JobBlockResponse extends BlockResponse {
    private String jobName;
}