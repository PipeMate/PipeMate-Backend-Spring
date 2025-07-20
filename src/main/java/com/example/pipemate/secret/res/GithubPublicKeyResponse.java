package com.example.pipemate.secret.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubPublicKeyResponse {

    @JsonProperty("key_id")
    private String keyId;

    @JsonProperty("key")
    private String key;
}