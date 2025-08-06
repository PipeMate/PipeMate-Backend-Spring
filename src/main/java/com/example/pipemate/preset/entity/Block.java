package com.example.pipemate.preset.entity;

import com.example.pipemate.preset.converter.JsonNodeConverter;
import com.example.pipemate.preset.converter.StringArrayConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type;

    private String description;

    private String jobName;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode config;

    // step 타입일 때만 사용
    private String domain;

    @Column(name = "task", columnDefinition = "text[]")
    @Convert(converter = StringArrayConverter.class)
    private String[] task;

}