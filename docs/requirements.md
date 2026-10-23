Role: Lead Software Architect & System Planner

Goal:
아래 비즈니스 요구사항을 분석하여 PostgreSQL, Spring Boot, Vue 3 기반 웹 애플리케이션의 상세 설계 문서를 작성하라.

Tech Stack:
- Backend: Java, Spring Boot (Spring Security + JWT, Spring Data JPA)
- Database: PostgreSQL
- Frontend: Vue 3 (Composition API, Pinia, Vue Router, Tailwind CSS 또는 Element Plus/Vuetify 택1)

Requirements:
1. SI 인력 투입 현황 대시보드
   - 프로젝트별 투입 인원 수, 투입 시작일~종료일 요약 시각화.
2. Admin 관리 콘솔 (ADMIN 전용)
   - 프로젝트 CRUD 및 RBAC 기반 접근 제어.
   - 프로젝트별 인원 할당(Assign), 투입/철수 일자 관리, 리소스 추가/제외.
   - 전체 프로젝트 공기 월별 타임시트(Gantt/Grid 구조).
   - 계획 공기(Plan) vs 실제 투입 공기(Actual) 시계열 비교 뷰.
3. 이슈 관리 시스템 (Jira-like)
   - 프로젝트별 이슈 등록, 수정, 삭제, 상세 조회.
   - 메인 Grid 화면 (프로젝트별 필터링, 정렬, 페이지네이션).
   - 상태(Status), 우선순위(Priority), 담당자(Assignee), 진척률(Progress).
   - 신규 등록 및 수정 팝업(Modal).
   - 첨부파일 업로드 (파일당 최대 2MB 제한, 유효성 검증).

Deliverables:
1. DB ERD 및 DDL 초안 (users, roles, projects, project_assignments, issues, issue_attachments 등 제약조건/FK 포함).
2. REST API Specification (엔드포인트, HTTP Method, Request/Response JSON Schema).
3. 프론트엔드 라우팅 및 컴포넌트 계층 구조도.
4. 파일 저장 및 2MB 검증 아키텍처.
5. 산출물은 프로젝트 루트의 `/docs/specs.md` 파일로 저장하라.


------------------------------------------------------------------------------------------

Role: Full-stack Software Engineer

Goal:
`/docs/specs.md`의 설계를 기반으로 PostgreSQL, Spring Boot, Vue 3 애플리케이션의 동작 가능한 전체 코드를 구현하라.

Constraints & Instructions:
1. Project Structure:
   - `/backend`: Spring Boot 프로젝트
   - `/frontend`: Vue 3 (Vite 기반) 프로젝트
   - `/docker-compose.yml`: PostgreSQL 및 로컬 실행 환경 정의

2. Backend Implementation (Spring Boot):
   - JPA Entity 및 Spring Data JPA Repository 작성.
   - Security: JWT 기반 인증, ADMIN 권한 검증(@PreAuthorize("hasRole('ADMIN')")).
   - Dashboard API: 프로젝트별 인원 수, 기간 집계 쿼리.
   - Admin API:
     * 프로젝트 CRUD.
     * 프로젝트-사용자 매핑 및 투입 기간 관리.
     * 계획 vs 실제 공기 집계 API (시계열 데이터 반환).
   - Issue API:
     * 메인 Grid 페이징/필터링 조회.
     * 이슈 CRUD.
     * 첨부파일 업로드/다운로드: `spring.servlet.multipart.max-file-size=2MB`, 파일 크기 초과 시 400/413 에러 처리 및 스토리지 격리.

3. Frontend Implementation (Vue 3):
   - 대시보드 화면: 프로젝트 투입 현황 요약 카드 및 차트.
   - Admin 화면:
     * ADMIN 가드 적용 라우터.
     * 프로젝트 관리 그리드 및 인원 할당/일정 수정 폼.
     * 월별 타임시트 및 계획 vs 실제 시계열 비교 뷰.
   - Issue 화면:
     * 프로젝트 선택 필터가 포함된 메인 Data Grid.
     * 행 클릭 시 상세 모달/사이드 패널.
     * 우측 상단 '신규 이슈' 버튼 -> 신규 등록 모달.
     * 목록 내 수정 클릭 -> 수정 모달.
     * 파일 첨부 컴포넌트 (클라이언트단 2MB 초과 차단 Alert).

4. 코드는 스텁(Mock)이 아닌 DB와 연동되는 실제 코드로 완성하고 빌드 에러가 없도록 구성하라.



------------------------------------------------------------------------------------------

Role: Senior QA Engineer & Code Auditor

Goal:
구현된 코드베이스를 검증하고 단위/통합 테스트를 작성 및 실행하여 요구사항 충족 여부를 확인하라.

Validation Checklist:
1. 보안 및 권한:
   - 일반 USER 토큰으로 Admin API 및 Admin 라우트 접근 차단 여부 (403 Forbidden 검증).
2. 파일 업로드 제한:
   - 2MB 이하 파일 업로드 성공 확인.
   - 2MB 초과 파일 업로드 시 백엔드 및 프론트엔드 차단 검증.
3. 공기 및 일정 로직:
   - 프로젝트 투입일이 종료일보다 늦은 경우 예외 처리 검증.
   - 계획 공기 vs 실제 투입 공기 계산 결과의 정합성 검증.
4. CRUD & UI 상호작용:
   - 이슈 신규 생성 팝업 -> 저장 -> 메인 Grid 즉각 반영 여부.
   - 수정 팝업을 통한 변경 사항 저장 검증.

Tasks:
1. 백엔드 JUnit 5 / MockMvc 통합 테스트 작성 (`IssueControllerTest`, `AdminControllerTest`, `FileUploadTest`).
2. 프론트엔드 컴포넌트 단위 테스트(Vitest) 작성.
3. `docker-compose up` 기준 빌드 및 실행 테스트 수행.
4. 테스트 결과 및 발견된 결함 리포트를 `/docs/qa-report.md`에 작성하고, 결함 발견 시 코드를 직접 수정하라.

------------------------------------------------------------------------------------------

추가 요구사항 (2026-10-23, 사용자 요청)

1. 모든 데이터 그리드는 무료 AG Grid Community(ag-grid-community + ag-grid-vue3)를 사용한다. Enterprise 기능은 사용하지 않는다.
2. WBS 관리 화면: 프로젝트별로 WBS(작업 분해 구조)를 입력·작성·수정하는 화면을 추가한다.
   - 계층형 작업 트리(작업명, 산출물, 담당자, 상태, 가중치, 계획/실제 기간, 진척률)
   - 추가/수정/삭제, 순서 이동·들여쓰기·내어쓰기, 상위 항목 진척률 자동 집계
   - 상세 설계: docs/specs-wbs.md