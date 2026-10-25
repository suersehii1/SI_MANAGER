# 전사 Project 관리 시스템

SI 프로젝트 인력 투입 현황 대시보드 · Admin 콘솔(프로젝트/할당/사용자/월별 타임시트/계획 vs 실제) · 이슈 관리(Jira-like, 첨부 2MB 제한) · 프로젝트별 WBS 작성/수정.

- 설계 문서: [`docs/specs.md`](docs/specs.md), WBS 부록 [`docs/specs-wbs.md`](docs/specs-wbs.md) (구현 차이는 specs.md 마지막 "Implementation notes")

## 기술 스택

> 정확한 버전의 기준은 [`frontend/package.json`](frontend/package.json), [`backend/pom.xml`](backend/pom.xml), 각 `Dockerfile`이다. 아래 표는 요약이다.

### 백엔드

| 구분 | 기술 | 버전 | 용도 |
|---|---|---|---|
| 언어 | Java | 17 | 서버 개발 언어 |
| 프레임워크 | Spring Boot | 3.3.5 | REST API 서버 (Spring Web, Validation, Actuator) |
| 보안 | Spring Security + jjwt | jjwt 0.12.6 | JWT 토큰 로그인, 역할(ADMIN/USER)별 권한 검사 |
| 데이터 접근 | Spring Data JPA (Hibernate) | Boot 3.3.5 기준 | DB 조회·저장, `@Version` 낙관적 잠금(동시 수정 덮어쓰기 방지) |
| 커넥션 풀 | HikariCP | Boot 3.3.5 기준 | DB 연결 재사용. 개수·대기 시간은 `docker-compose.yml`의 `DB_POOL_*`로 조정 |
| DB 마이그레이션 | Flyway | Boot 3.3.5 기준 | 테이블 생성·변경 이력 관리 (`backend/src/main/resources/db/migration`) |
| 데이터베이스 | PostgreSQL | 16 | 운영 DB |
| 테스트 | JUnit 5, MockMvc, H2 | Boot 3.3.5 기준 | 통합 테스트 (H2를 PostgreSQL 모드로 실행) |

### 프론트엔드

| 구분 | 기술 | 버전 | 용도 |
|---|---|---|---|
| 프레임워크 | Vue 3 (`<script setup>`, JavaScript) | 3.4 | 화면 개발 |
| 빌드 도구 | Vite | 5.4 | 개발 서버, 배포용 빌드 |
| 상태 관리 / 라우팅 | Pinia / Vue Router | 2.2 / 4.4 | 로그인 정보·화면 상태 보관, 화면 이동과 권한 가드 |
| UI 컴포넌트 | Element Plus (+ icons-vue) | 2.8 | 버튼, 입력 폼, 팝업, 탭, 메뉴, 낮/밤 스킨(다크 모드) |
| **데이터 그리드(표)** | **AG Grid Community** (`ag-grid-community`, `ag-grid-vue3`) | 32.3 | 모든 목록 표: 이슈, 프로젝트, 인원 할당, 사용자, 월별 타임시트, 계획 vs 실제, 대시보드 요약, WBS (테마 `ag-theme-quartz`) |
| **차트** | **Apache ECharts** (`echarts`, `vue-echarts`) | 5.5 / 6.7 | 대시보드 인원 차트, 계획 vs 실제 막대·선 차트 |
| **간트 차트** | ECharts custom series (직접 구현) | - | 간트 전용 라이브러리 없이 [`frontend/src/utils/gantt.js`](frontend/src/utils/gantt.js)에서 계획·실제 막대를 그림. 대시보드 타임라인, 월별 타임시트 Gantt, WBS Gantt 공용 |
| 날짜 처리 | dayjs | 1.11 | 날짜 계산·표시 |
| HTTP 통신 | axios | 1.7 | API 호출, 로그인 토큰 첨부, 공통 오류 처리 |
| 테스트 | Vitest, Vue Test Utils, jsdom | 2.1 / 2.4 / 25 | 화면 단위 테스트 |

### 실행 환경

| 구분 | 기술 | 버전 | 용도 |
|---|---|---|---|
| 컨테이너 | Docker Compose | - | DB·백엔드·화면 3개 컨테이너 실행 |
| 웹 서버 | nginx | 1.27 | 화면 파일 제공, `/api` 요청을 백엔드로 전달 |
| 빌드 이미지 | Node / Maven + Temurin JDK | Node 20 / Maven 3.9, JDK 17 | 컨테이너 안에서 화면·서버 빌드 (PC에 Node 20이 없어도 됨) |

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

## 초기 계정 / 데이터

| 아이디 | 비밀번호 | 권한 |
|---|---|---|
| admin | admin1234 | ROLE_ADMIN, ROLE_USER |

DB에는 권한 코드(ROLE_ADMIN, ROLE_USER)와 admin 계정만 들어 있다. 사용자·프로젝트·할당·이슈·WBS는 화면에서 직접 등록한다.
(예전 화면 확인용 샘플 데이터는 `V5__remove_sample_data.sql`에서 삭제. 통합 테스트용 샘플 데이터는 `backend/src/test/resources/db/testdata/`에만 있다.)

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
