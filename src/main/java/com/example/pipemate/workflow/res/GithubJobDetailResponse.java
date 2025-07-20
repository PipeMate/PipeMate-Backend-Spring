package com.example.pipemate.workflow.res;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubJobDetailResponse {
    private Long id;
    private String name;
    private String status;
    private String conclusion;
    private List<Step> steps;

    @Data
    public static class Step {
        private String name;
        private String status;
        private String conclusion;
        private String startedAt;
        private String completedAt;
    }

    public static GithubJobDetailResponse from(JsonNode jobNode) {
        GithubJobDetailResponse response = new GithubJobDetailResponse();
        response.setId(jobNode.get("id").asLong());
        response.setName(jobNode.get("name").asText());
        response.setStatus(jobNode.get("status").asText());
        response.setConclusion(jobNode.get("conclusion").asText());

        List<Step> steps = new ArrayList<>();
        JsonNode stepsArray = jobNode.get("steps");
        if (stepsArray != null && stepsArray.isArray()) {
            for (JsonNode step : stepsArray) {
                Step s = new Step();
                s.setName(step.get("name").asText());
                s.setStatus(step.get("status").asText());
                s.setConclusion(step.get("conclusion").asText());
                s.setStartedAt(step.get("started_at").asText());
                s.setCompletedAt(step.get("completed_at").asText());
                steps.add(s);
            }
        }
        response.setSteps(steps);
        return response;
    }
}