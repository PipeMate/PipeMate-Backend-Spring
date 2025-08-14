## 🛠️ PipeMate (Backend) 
**블록 기반 GitHub Actions 워크플로우(파이프라인) 관리 통합 서비스**인 "PipeMate" 의 백엔드 서버입니다.

## 주요 기능 
**블록 기반 워크플로우 생성, 조회, 수정, 삭제**
-  블록 기반 JSON 요청 -> GitHub Actions YAML 변환 및 업로드
- 기존 워크플로우(GitHub Actions YAML)를 블록 기반 JSON 형태로 응답 및 수정, 삭제

**워크플로우 실행 및 상세 정보 조회**
- 저장소 워크플로우 목록 및 상세 조회
- 실행(run) 목록, 실행 상세, 실행 로그 조회
- 수동 실행 및 취소

**프리셋 조회(preset)**
- 블록 기반 워크플로우 설계를 위한 프리셋 조회
- 기 제공되는 워크플로우 프리셋 조회 

**GitHub Secrets 관리**
- 저장소 시크릿 생성·수정 (LazySodium 암호화)
- 도메인별 시크릿 목록 조회
- 시크릿 삭제, 퍼블릭키 조회

## 서버 아키텍처
- GitHub API 연동을 위한 **`GithubApiClient`** 컴포넌트 구
- 캐싱을 통한 성능 최적화 (**`@Cacheable`** / **`@CacheEvict`**, Caffeine)
- 보안강화를 위한 시크릿 값 암호화 (LazySodium, libsodium)

## **기술 스택**
- **Language & Framework**: Java 17, Spring Boot 3.5.x
- **Dependencies**:
    - Spring Web, Spring Data JPA
    - Swagger/OpenAPI (springdoc)
    - Jackson, SnakeYAML
    - Caffeine Cache
    - LazySodium (libsodium Java binding, JNA)
- **Databases**: AWS RDS(PostgreSQL), H2 (test)
- **Build Tool**: Gradle

## **주요 API 개요**

## **🔹 블록 기반 워크플로우 생성, 조회, 수정, 삭제 `/api/pipelines`**

| **Method** | **Endpoint** | **설명** |
| --- | --- | --- |
| POST | **`/api/pipelines`** | 블록 기반 JSON 파이프라인 변환+GitHub 업로드 |
| GET | **`/api/pipelines/{ymlFileName}`** | 저장된 파이프라인 조회 |
| PUT | **`/api/pipelines`** | 파이프라인 업데이트 |
| DELETE | **`/api/pipelines/{ymlFileName}`** | 파이프라인 삭제 |

## **🔹 프리셋 `/api/presets`**

| **Method** | **Endpoint** | **설명** |
| --- | --- | --- |
| GET | **`/blocks`** | 블록 프리셋 조회 |
| GET | **`/pipelines`** | 파이프라인 프리셋 조회 |

## **🔹 워크플로우 관리 `/api/github`**

| **Method** | **Endpoint** | **설명** |
| --- | --- | --- |
| GET | **`/workflows`** | 저장소 워크플로우 목록 조회 |
| GET | **`/workflows/{workflowId}`** | 워크플로우 상세 조회 |
| GET | **`/workflow-runs`** | 실행(run) 목록 조회 |
| GET | **`/workflow-run`** | 실행 상세 조회 |
| GET | **`/workflow-run/logs/raw`** | 실행 로그 텍스트 반환 |
| GET | **`/workflow-run/jobs`** | 실행 내 Job 목록 조회 |
| GET | **`/workflow-run/job`** | 단일 Job 상세 조회 |
| POST | **`/workflows/dispatch`** | 워크플로우 수동 실행 |
| POST | **`/workflow-run/cancel`** | 실행 취소 |

## **🔹 GitHub Secrets `/api/github/repos/secrets`**

| **Method** | **Endpoint** | **설명** |
| --- | --- | --- |
| GET | **`/secrets`** | 도메인별 그룹화된 시크릿 목록 조회 |
| PUT | **`/secrets`** | 시크릿 생성 또는 수정 |
| DELETE | **`/secrets`** | 시크릿 삭제 |
| GET | **`/secrets/public-key`** | 퍼블릭 키 조회(암호화용) |


## **실행 방법**

1. 저장소 클론
```
git clone <repo-url>
cd <repo-dir>
```
2. 환경 설정

application.yml에 GitHub Token, DB 설정 추가

3. 실행

```./gradlew bootRun```

**Swagger UI**: http://localhost:8080/swagger-ui.html

## **인증**

모든 GitHub API 연동 요청은 **`Authorization: Bearer <GitHub Personal Access Token>`** 헤더 필요

- 최소 권한: **`repo`**, **`workflow`**

## **라이선스**

MIT License

## Commit Convention

- **커밋 메시지 규칙**
    - `{tag}: {commit message}`
    - ex) feat: login button component

| Tag Name  | Description                                                                                   |
| :-------- | :-------------------------------------------------------------------------------------------- |
| Feat      | 새로운 기능 추가                                                                              |
| Design    | CSS 등 사용자 UI 디자인 변경                                                                  |
| Style     | 코드 포맷 변경, 세미 콜론 누락, 코드 수정이 없는 경우                                         |
| Comment   | 필요한 주석 추가, 변경 및 삭제                                                                |
| Fix       | 버그 수정                                                                                     |
| Refactor  | 프로덕션 코드 리팩토링, 새로운 기능이나 버그 수정없이 현재 구현을 개선한 경우                 |
| Docs      | README.md 수정                                                                                |
| Rename    | 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우                                            |
| Remove    | 파일을 삭제하는 작업만 수행한 경우                                                            |
| Test      | 테스트 코드, 리펙토링 테스트 코드 추가, Production Code(실제로 사용하는 코드) 변경 없음       |
| Chore     | 빌드 업무 수정, 패키지 매니저 수정, 패키지 관리자 구성 등 업데이트, Production Code 변경 없음 |
| !BREAKING | CHANGE 커다란 API 변경의 경우                                                                 |
| !HOTFIX   | 급하게 치명적인 버그를 고쳐야하는 경우                                                        |

## Branch

- **브랜치 규칙**
    - `{접두사}/#{이슈 번호}_{이름}`
    - ex) feat/#23_social-login

