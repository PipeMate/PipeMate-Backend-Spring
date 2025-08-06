package com.example.pipemate.preset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pipeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PipelineBlock> blocks = new ArrayList<>();
}