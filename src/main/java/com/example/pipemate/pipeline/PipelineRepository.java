package com.example.pipemate.pipeline;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PipelineRepository extends MongoRepository<PipelineEntity, String> {
}
