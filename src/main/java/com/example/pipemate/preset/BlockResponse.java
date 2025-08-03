package com.example.pipemate.preset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockResponse {
    private Long id;
    private String name;
    private String type;
    private String description;
    private JsonNode config;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jobName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String domain;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String[] task;

}