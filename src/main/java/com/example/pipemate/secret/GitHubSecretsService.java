package com.example.pipemate.secret;

import com.example.pipemate.secret.req.GithubSecretRequest;
import com.example.pipemate.secret.res.GithubPublicKeyResponse;
import com.example.pipemate.secret.res.GithubSecretListResponse;
import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.Box;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.yaml.snakeyaml.tokens.Token.ID.Key;

@Service
@RequiredArgsConstructor
public class GitHubSecretsService {

    private final RestTemplate restTemplate = new RestTemplate();

    public GithubSecretListResponse getRepositorySecrets(String owner, String repo, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/actions/secrets", owner, repo);

        HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<GithubSecretListResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                GithubSecretListResponse.class
        );

        return response.getBody();
    }

    public void createOrUpdateSecret(String owner, String repo, String secretName, GithubSecretRequest request, String token) {
        // 1. 공개키 조회
        String keyUrl = String.format("https://api.github.com/repos/%s/%s/actions/secrets/public-key", owner, repo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> keyEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> keyResponse = restTemplate.exchange(keyUrl, HttpMethod.GET, keyEntity, Map.class);

        if (!keyResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to fetch public key");
        }

        String base64PublicKey = (String) keyResponse.getBody().get("key");
        String keyId = (String) keyResponse.getBody().get("key_id");

        // 2. 암호화 수행
        String encryptedValue = encryptSecret(request.getValue(), base64PublicKey);

        // 3. 요청 객체 구성
        Map<String, String> body = new HashMap<>();
        body.put("encrypted_value", encryptedValue);
        body.put("key_id", keyId); // 반드시 필요

        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setBearerAuth(token);
        putHeaders.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.github+json")));
        putHeaders.setContentType(MediaType.APPLICATION_JSON);
        putHeaders.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, putHeaders);

        String url = String.format("https://api.github.com/repos/%s/%s/actions/secrets/%s", owner, repo, secretName);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create or update secret. Status: " + response.getStatusCode());
        }
    }

    public GithubPublicKeyResponse getPublicKey(String owner, String repo, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/actions/secrets/public-key", owner, repo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GithubPublicKeyResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, request, GithubPublicKeyResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("GitHub public key 요청 실패");
        }

        return response.getBody();
    }


    public String encryptSecret(String plainText, String base64PublicKey) {
        LazySodiumJava lazySodium = new LazySodiumJava(new SodiumJava());

        byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);
        byte[] messageBytes = plainText.getBytes(StandardCharsets.UTF_8);

        byte[] cipherBytes = new byte[Box.SEALBYTES + messageBytes.length];

        boolean success = lazySodium.cryptoBoxSeal(cipherBytes, messageBytes, messageBytes.length, publicKeyBytes);
        if (!success) {
            throw new RuntimeException("Failed to encrypt secret");
        }

        return Base64.getEncoder().encodeToString(cipherBytes);
    }

    public void deleteSecret(String owner, String repo, String secretName, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/actions/secrets/%s", owner, repo, secretName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to delete secret. Status: " + response.getStatusCode());
        }
    }

}