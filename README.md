# SI Project Manager

SI 프로젝트 인력 투입 현황 대시보드 · Admin 콘솔(프로젝트/할당/사용자/월별 타임시트/계획 vs 실제) · 이슈 관리(Jira-like, 첨부 2MB 제한) · 프로젝트별 WBS 작성/수정.

- Backend: Java 17, Spring Boot 3.3.5, Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL 16
- Frontend: Vue 3 + Vite 5, Pinia, Vue Router, Element Plus, AG Grid Community 32, ECharts(vue-echarts)
- 설계 문서: [`docs/specs.md`](docs/specs.md), WBS 부록 [`docs/specs-wbs.md`](docs/specs-wbs.md) (구현 차이는 specs.md 마지막 "Implementation notes")

## 빠른 실행 (Docker Compose)

```bash
docker compose up -d --build
# 브라우저: http://localhost:3000
docker compose down        # 중지 (데이터 유지)
docker compose down -v     # 중지 + DB/업로드 볼륨 삭제 (시드 상태로 초기화)
```

| 서비스 | 주소 | 비고 |
|---|---|---|
| frontend (nginx) | http://localhost:3000 | SPA + `/api` → backend 프록시, `client_max_body_size 10m` |
| backend | http://localhost:8080 | health: `/actuator/health` |
| db (PostgreSQL 16) | localhost:5432 | DB `sipm`, 계정 `sipm` / `sipm` |

볼륨: `pgdata`(DB), `uploads`(첨부파일, 컨테이너 `/data/uploads`).

## 시드 계정

| 아이디 | 비밀번호 | 권한 |
|---|---|---|
| admin | admin1234 | ROLE_ADMIN, ROLE_USER |
| user | user1234 | ROLE_USER |
| kim | user1234 | ROLE_USER (SI-2026-001, SI-2026-002 멤버) |
| lee | user1234 | ROLE_USER (SI-2026-001 멤버) |

시드 프로젝트 2개, 할당 3건, 이슈 3건, SI-2026-001의 WBS 10개 항목이 들어 있다. 값은 specs.md §3.6과 specs-wbs.md §W3.1의 worked example과 같다(기준일 `asOf=2026-03-15`).

```bash
# 예: 로그인 후 계획 vs 실제 조회
TOKEN=$(curl -s -X POST http://localhost:3000/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin1234"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
curl -s "http://localhost:3000/api/admin/plan-vs-actual?projectId=1&from=2026-01&to=2026-04&asOf=2026-03-15" \
  -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3000/api/projects/1/wbs?asOf=2026-03-15" -H "Authorization: Bearer $TOKEN"
```

## 개발 환경에서 빌드/테스트

호스트에 Maven/Node 20이 없어도 Docker로 빌드할 수 있다(Windows는 절대 경로로 마운트).

```bash
# Backend: 패키징 / 테스트 (테스트는 H2 PostgreSQL 모드에서 같은 Flyway 마이그레이션 실행)
docker run --rm -v "<abs>/backend:/app" -v sipm-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B -DskipTests package
docker run --rm -v "<abs>/backend:/app" -v sipm-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B test
# (Maven 래퍼도 포함: ./mvnw package)

# Frontend: 의존성 설치 / 빌드 / 단위 테스트
docker run --rm -v "<abs>/frontend:/app" -w /app node:20-alpine sh -c "npm ci && npm run build && npx vitest run"
```

로컬 개발 서버: backend를 8080에서 실행(`SPRING_DATASOURCE_URL` 등 환경 변수, 기본값은 `localhost:5432/sipm`, `APP_STORAGE_ROOT` 기본 `/data/uploads`)한 뒤 `frontend`에서 `npm run dev` → http://localhost:5173 (`/api`는 8080으로 프록시).

## 디렉터리

```
backend/    Spring Boot (com.sipm: auth, user, project, assignment, metrics, dashboard, admin, issue, attachment, wbs, common, security, config)
            src/main/resources/db/migration: V1 스키마, V2 시드, V3 WBS 스키마, V4 WBS 시드
frontend/   Vue 3 SPA (src/api, router, stores, layouts, components, views, utils), nginx.conf, Dockerfile
docs/       requirements.md, specs.md, specs-wbs.md
docker-compose.yml
```

## 주요 규칙 요약

- 권한: `/api/admin/**`는 ADMIN만 접근 가능하다(URL 규칙 + `@PreAuthorize` 이중 방어). 이슈/첨부 삭제는 작성자 또는 ADMIN, WBS 쓰기는 ADMIN 또는 프로젝트 멤버만 할 수 있다.
- 첨부파일: 파일당 최대 2,097,152 bytes(정확히 2MB는 허용). 클라이언트 alert 차단 → nginx 10MB → Spring multipart 413 → 서비스 400 → DB CHECK. 저장 경로는 `{root}/issues/{issueId}/{UUID}.{ext}`이며 원본 파일명은 경로에 쓰지 않는다.
- MM 계산: 투입률 × 해당 월 투입 일수 ÷ 해당 월 달력 일수. 합계는 반올림 전 값으로 계산하고, 응답 값만 소수 2자리로 반올림한다.
- 에러 응답: 모든 4xx/5xx는 `{timestamp,status,error,code,message,path,fieldErrors}` JSON 형식이다.
