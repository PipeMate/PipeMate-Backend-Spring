package com.example.pipemate.preset.res;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
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
}