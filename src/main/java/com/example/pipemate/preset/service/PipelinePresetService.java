package com.example.pipemate.preset.service;

import com.example.pipemate.pipeline.res.PipelineResponse;
import com.example.pipemate.preset.entity.Pipeline;
import com.example.pipemate.preset.entity.PipelineBlock;
import com.example.pipemate.preset.repository.PipelineRepository;
import com.example.pipemate.preset.res.BlockResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelinePresetService {

    private final PipelineRepository pipelineRepository;
    private final ObjectMapper objectMapper;

    public List<PipelineResponse> getAllPipelineResponses() {
        List<Pipeline> pipelines = pipelineRepository.findAll();

        return pipelines.stream().map(pipeline -> {

            List<JsonNode> blockJsons = pipeline.getBlocks().stream()
                    .sorted(Comparator.comparingInt(PipelineBlock::getPosition))
                    .map(BlockResponse::from)
                    .map(block -> objectMapper.convertValue(block, JsonNode.class))
                    .collect(Collectors.toList());

            return PipelineResponse.builder()
                    .workflowId(String.valueOf(pipeline.getId()))
                    .workflowName(pipeline.getName())
                    .originalJson(blockJsons) // 타입 맞춰서 BlockResponse로 교체
                    .success(true)
                    .message("성공적으로 로딩됨")
                    .build();

        }).collect(Collectors.toList());
    }

}