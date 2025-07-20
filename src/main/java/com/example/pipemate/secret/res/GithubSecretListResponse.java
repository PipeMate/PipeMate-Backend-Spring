package com.example.pipemate.secret.res;

import lombok.Data;

import java.util.List;

@Data
public class GithubSecretListResponse {
    private int total_count;
    private List<SecretItem> secrets;

    @Data
    public static class SecretItem {
        private String name;
        private String created_at;
        private String updated_at;
    }
}