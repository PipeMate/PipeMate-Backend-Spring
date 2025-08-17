package com.example.pipemate.secret;

import com.example.pipemate.secret.req.GithubSecretRequest;
import com.example.pipemate.secret.res.GithubPublicKeyResponse;
import com.example.pipemate.secret.res.GithubSecretListResponse;
import com.example.pipemate.secret.res.GroupedGithubSecretListResponse;
import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.Box;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSecretsService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 지정한 저장소의 시크릿 목록을 조회한다.
     */
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

    /**
     * 저장소의 시크릿을 도메인별로 그룹화하여 조회한다.
     * (예: 'PAYMENT_KEY', 'PAYMENT_SECRET' → PAYMENT 그룹)
     */
    @Cacheable("secret-key-list")
    public GroupedGithubSecretListResponse getGroupedRepositorySecrets(String owner, String repo, String token) {
        log.info("[GitHubSecretsController] 레포지토리의 도메인별 시크릿 목록 조회");

        GithubSecretListResponse original = getRepositorySecrets(owner, repo, token); // 기존 로직 그대로 호출

        Map<String, List<GithubSecretListResponse.SecretItem>> grouped = new HashMap<>();

        for (GithubSecretListResponse.SecretItem item : original.getSecrets()) {
            String name = item.getName();
            String domain = name.contains("_") ? name.split("_")[0] : "UNKNOWN";

            grouped.computeIfAbsent(domain, k -> new ArrayList<>()).add(item);
        }

        GroupedGithubSecretListResponse response = new GroupedGithubSecretListResponse();
        response.setGroupedSecrets(grouped);
        return response;
    }

    /**
     * 저장소에 시크릿을 생성하거나 업데이트한다.
     * GitHub API에서 요구하는 공개키를 이용해 값 암호화 후 저장.
     */
    @CacheEvict(value = "secret-key-list", allEntries = true)
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

    /**
     * GitHub 저장소의 시크릿 암호화를 위한 Public Key를 조회한다.
     */
    @Cacheable("repo-public-key") // 깃허브 레포지토리 퍼블릭 키 캐싱 적용
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

    /**
     * 공개키를 사용해 시크릿 값을 암호화한다.
     * GitHub API는 libsodium의 sealed box 암호화 방식을 사용한다.
     */
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

    /**
     * 저장소의 시크릿을 삭제한다.
     */
    @CacheEvict(value = "secret-key-list", allEntries = true)
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