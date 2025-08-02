package com.example.pipemate.secret;

import com.example.pipemate.secret.req.GithubSecretRequest;
import com.example.pipemate.secret.res.GithubPublicKeyResponse;
import com.example.pipemate.secret.res.GroupedGithubSecretListResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/repos")
@RequiredArgsConstructor
public class GitHubSecretsController {

    private final GitHubSecretsService secretsService;

    @GetMapping("/secrets")
    @Operation(summary = "레포지토리의 도메인별 시크릿 목록 조회",
            description = "도메인 접두어를 기준으로 그룹화된 시크릿 목록을 반환합니다.")
    public ResponseEntity<GroupedGithubSecretListResponse> getGroupedRepositorySecrets(
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();
        GroupedGithubSecretListResponse response = secretsService.getGroupedRepositorySecrets(owner, repo, cleanToken);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/secrets")
    @Operation(summary = "레포지토리 시크릿 생성 또는 수정",
            description = "레포지토리에 새로운 시크릿을 생성하거나 기존 시크릿을 업데이트합니다.")
    public ResponseEntity<Void> createOrUpdateSecret(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String secretName,
            @RequestBody GithubSecretRequest secretRequest,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }
        String cleanToken = token.substring("Bearer ".length()).trim();
        secretsService.createOrUpdateSecret(owner, repo, secretName, secretRequest, cleanToken);
        return ResponseEntity.status(201).build();
    }

    //    @GetMapping("/secrets/public-key")
    @Operation(summary = "레포지토리 퍼블릭 키 조회",
            description = "GitHub 레포지토리의 시크릿 암호화를 위한 퍼블릭 키를 조회합니다.")
    public ResponseEntity<GithubPublicKeyResponse> getPublicKey(
            @RequestParam String owner,
            @RequestParam String repo,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();
        GithubPublicKeyResponse response = secretsService.getPublicKey(owner, repo, cleanToken);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/secrets")
    @Operation(summary = "레포지토리 시크릿 삭제", description = "지정한 시크릿 이름의 레포지토리 시크릿을 삭제합니다.")
    public ResponseEntity<Void> deleteSecret(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String secretName,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in 'Bearer ghp_xxx' format");
        }

        String cleanToken = token.substring("Bearer ".length()).trim();
        secretsService.deleteSecret(owner, repo, secretName, cleanToken);
        return ResponseEntity.noContent().build(); // 204
    }


}