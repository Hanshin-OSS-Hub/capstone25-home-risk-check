# Branch Strategy for home-risk-check

이 문서는 `home-risk-check` 프로젝트 팀원들을 위한 브랜치 관리 규칙입니다.
프로젝트 안정성과 협업 효율을 위해 반드시 준수해주세요.

---

## 1. 브랜치 종류

| 브랜치 | 용도 |
|--------|-----|
| `main` | 배포/발표용 안정 브랜치. 직접 커밋 금지 |
| `feature/backend-*` | Spring Boot 관련 기능 개발 |
| `feature/ai-*` | FastAPI 관련 기능 개발 |
| `docs/*` | 문서 작성/수정 (README, API 명세 등) |
| `hotfix/*` | 긴급 버그 수정 브랜치 |

---

## 2. 브랜치 네이밍 규칙

- **feature 브랜치**: `feature/<서브시스템>-<기능>`  
  예시: `feature/backend-auth`, `feature/ai-risk-model`
- **hotfix 브랜치**: `hotfix/<문제>`  
  예시: `hotfix/login-bug`
- **docs 브랜치**: `docs/<문서>`  
  예시: `docs/api-spec`, `docs/onboarding`

---

## 3. 개발 흐름

1. main 브랜치 최신화
   ```bash
   git checkout main
   git pull origin main