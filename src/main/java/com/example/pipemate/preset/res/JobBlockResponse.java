package com.example.pipemate.preset.res;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class JobBlockResponse extends BlockResponse {
    private String jobName;
}