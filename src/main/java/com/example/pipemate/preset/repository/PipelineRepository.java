package com.example.pipemate.preset.repository;

import com.example.pipemate.preset.entity.Pipeline;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {

    /**
     * Pipeline의 blocks (PipelineBlock 리스트)까지 한 번에 가져오기 위한 EntityGraph 설정.
     * -> Pipeline → PipelineBlock → Block 으로 연쇄적으로 접근 가능해짐
     */
    @EntityGraph(attributePaths = {
            "blocks",          // Pipeline.blocks (List<PipelineBlock>)
            "blocks.block"     // PipelineBlock.block (Block 자체)
    })
    List<Pipeline> findAll();
}