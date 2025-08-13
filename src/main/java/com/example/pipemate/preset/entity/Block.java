package com.example.pipemate.preset.entity;

import com.example.pipemate.preset.converter.JsonNodeConverter;
import com.example.pipemate.preset.converter.StringArrayConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "block")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(of = "id")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예: "trigger" | "job" | "step" */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * ❗ 기존 필드 유지 (하위호환)
     * 생성 시 기본 NULL, 실제 파이프라인 jobName은 pipeline_block.jobName에서 관리
     */
    @Builder.Default
    private String jobName = null;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode config;

    private String domain;

    @Column(name = "task", columnDefinition = "text[]")
    @Convert(converter = StringArrayConverter.class)
    private String[] task;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (jobName == null || jobName.isBlank()) {
            jobName = null; // 강제 null
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
