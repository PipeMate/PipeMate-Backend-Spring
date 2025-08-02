package com.example.pipemate.secret.res;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GroupedGithubSecretListResponse {
    private Map<String, List<GithubSecretListResponse.SecretItem>> groupedSecrets;
}