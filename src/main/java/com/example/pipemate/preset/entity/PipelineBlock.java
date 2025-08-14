package com.example.pipemate.preset.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pipeline_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_pipeline_block_pipeline_position",
                        columnNames = {"pipeline_id", "position"})
        },
        indexes = {
                @Index(name = "ix_pipeline_block_pipeline", columnList = "pipeline_id"),
                @Index(name = "ix_pipeline_block_block", columnList = "block_id"),
                @Index(name = "ix_pipeline_block_job_name", columnList = "job_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PipelineBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어느 파이프라인에 배치되는지
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pipeline_block_pipeline"))
    private Pipeline pipeline;

    /**
     * 어떤 블록 템플릿을 쓰는지
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pipeline_block_block"))
    private Block block;

    /**
     * 파이프라인 내 표시/실행 순서 (1부터)
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * 이 배치가 속하는 job 키 (예: "job1", "job2")
     * trigger 블록 등의 경우 NULL 가능
     */
    @Column(name = "job_name")
    private String jobName;

    /**
     * 타임스탬프(옵션)
     */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
