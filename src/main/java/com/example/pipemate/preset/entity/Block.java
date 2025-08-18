package com.example.pipemate.preset.entity;

import com.example.pipemate.preset.converter.JsonNodeConverter;
import com.example.pipemate.preset.converter.StringArrayConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "block")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    private String jobName = null;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode config;

    @Column(name = "config_row", columnDefinition = "text")
    private String configRaw;

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

    /** 응답에 쓸 RAW JSON (우선순위: configRaw → config 직렬화) */
    public String getConfigRawForView(ObjectMapper mapper) {
        try {
            ObjectMapper m = (mapper != null) ? mapper : JsonMapper.builder().build();
            if (configRaw != null && !configRaw.isBlank()) return configRaw;
            if (config != null) return m.writeValueAsString(config);
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("config 직렬화 실패", e);
        }
    }
}
