# SI Project Manager — QA 리포트

- 작성: QA Engineer, 2026-10-23
- 기준 문서: `docs/requirements.md` §3(QA 체크리스트) + 추가 요구사항(AG Grid Community, WBS), `docs/specs.md`(§3.6 Worked Example, §10 테스트 매핑, Implementation notes), `docs/specs-wbs.md`(W3.1, W7)
- 결론: **백엔드 114/114, 프론트 73/73 통과, `npm run build` 성공, docker compose 스모크 전 항목 통과.** 남아 있는 실패 테스트(known failure)는 없습니다. 결함 2건은 운영 코드 수정 금지 지시 이전에 QA가 직접 수정했고(아래 "QA가 이미 수정한 항목"), 그 이후 발견한 사항은 수정하지 않고 "개발자 확인 요청"에 기록했습니다.

---

## 1. 테스트 환경

| 항목 | 내용 |
|---|---|
| Host | Windows 11, Docker Desktop (호스트 Node v14 → npm은 컨테이너에서만 실행) |
| Backend | Spring Boot 3.3.5 / Java 17, `maven:3.9-eclipse-temurin-17` 컨테이너, 테스트 프로필 H2(PostgreSQL 모드) + Flyway V1~V4 |
| Frontend | Vue 3 + Vite 5, Vitest 2 + jsdom + @vue/test-utils, `node:20-alpine` 컨테이너. element-plus는 lock 기준 **2.14.7** |
| 통합 실행 | `docker compose up -d --build` (postgres:16, backend 8080, nginx frontend 3000) → 스모크 → `docker compose down -v` |
| 브라우저 | **사용하지 않음** (수동 UI 확인 미실시, 6절 참고) |

---

## 2. 체크리스트 결과

| # | 체크 항목 | 검증 테스트 | 결과 |
|---|---|---|---|
| 1-1 | USER 토큰으로 Admin API(#7~#20, 14개) 접근 → 403 `FORBIDDEN` | `AdminControllerTest.userTokenIsForbiddenOnEveryAdminEndpoint` (parameterized 14건), 스모크 #2 | PASS |
| 1-2 | 토큰 없음/위조 → 401 `UNAUTHORIZED` | `AdminControllerTest.noTokenIsUnauthorized`(14건), `forgedTokenIsUnauthorized`, 스모크 #2b | PASS |
| 1-3 | 비활성화된 사용자의 기존 토큰 즉시 차단 | `AdminControllerTest.tokenOfDisabledUserIsRejectedOnNextRequest` | PASS |
| 1-4 | Admin 컨트롤러 5개 클래스 레벨 `@PreAuthorize("hasRole('ADMIN')")`(2차 방어) | `AdminControllerTest.adminControllersHaveClassLevelPreAuthorize` | PASS |
| 1-5 | ADMIN CRUD (프로젝트/할당/사용자) | `adminProjectCrudRoundTrip`, `adminAssignmentCrudRoundTrip`, `adminUserCreateUpdateAndRules`, `deletingProjectCascadesAssignmentsIssuesAndWbs` | PASS |
| 1-6 | FE: USER가 `/admin/*` 진입 → `forbidden`, 미로그인 → `login?redirect=` | `router.guard.spec.js` (6개 admin 경로, 실제 네비게이션 포함), `AppSidebar` Admin 메뉴 노출 | PASS |
| 2-1 | 2MB 이하 업로드 성공 (1KB, 정확히 2,097,152 bytes) + 다운로드 바이트 동일 | `FileUploadTest.oneKilobyteUploadSucceedsAndDownloadsSameBytes`, `exactlyTwoMegabytesSucceedsAndIsStoredUnderIsolatedRoot`, 스모크 #4(1MB·2MB 201, 다운로드 cmp 동일) | PASS |
| 2-2 | 2MB 초과 차단 — BE 서비스 계층(MockMvc, 2,097,153 bytes) → 400 `FILE_TOO_LARGE` | `FileUploadTest.twoMegabytesPlusOneIsRejectedByServiceLayer` | PASS |
| 2-3 | 2MB 초과 차단 — 실제 Tomcat 멀티파트 한도 → **413** `FILE_TOO_LARGE` | `FileUploadTest.twoMegabytesPlusOneIsRejectedWith413ByRealTomcat` (RANDOM_PORT + TestRestTemplate), 스모크 #4(2MB+1·3MB → 413, nginx 경유) | PASS |
| 2-4 | 2MB 초과 차단 — FE (alert, 요청 미전송) | `file.spec.js`(경계값), `AttachmentUploader.spec.js`(alert 문구, `uploadAttachment` 미호출, `rejected` emit, 서버 413도 동일 문구), `http.spec.js`(413 메시지) | PASS |
| 2-5 | 빈 파일/파트 누락 400 `FILE_EMPTY`, 금지 확장자 400 `FILE_TYPE_NOT_ALLOWED`, `..` → `INVALID_FILENAME` | `FileUploadTest.emptyOrMissingFileIsRejected`, `disallowedExtensionsAreRejected`, `invalidFilenameIsRejected`, 스모크 #4c | PASS |
| 2-6 | 저장 격리: `{root}/issues/{id}/{UUID}.{ext}`, `../../evil.txt` → `evil.txt`, 루트 밖 파일 미생성, Content-Type은 서버가 확장자로 결정 | `pathTraversalFilenameIsSanitizedAndStaysInsideRoot`, `exactlyTwoMegabytes…`, 스모크 #4d(컨테이너 내부 확인) | PASS |
| 2-7 | 첨부 삭제 권한(업로더/ADMIN), 이슈 삭제 후 업로드 디렉터리 정리 | `attachmentDeleteIsAllowedOnlyForUploaderOrAdmin`, 스모크 #9(삭제 후 `/data/uploads/issues/{id}` 없음) | PASS |
| 3-1 | 프로젝트 종료일 < 시작일 → 400 `plannedEndDate/DATE_RANGE`, 실제 종료만 입력 → `REQUIRED_WITH_END` | `projectEndBeforeStartIsRejected`, `projectActualEndWithoutStartOrBeforeStartIsRejected`, 같은 날 허용 `projectSameDayPeriodIsAllowed` | PASS |
| 3-2 | 할당(투입) 종료 < 시작 → 400 `plannedEnd/DATE_RANGE`, 철수 < 투입 → `actualEnd/DATE_RANGE`, 프로젝트 기간 밖 → `ASSIGNMENT_OUT_OF_PROJECT_PERIOD`, 기간 축소 충돌 409 | `assignmentPlannedEndBeforeStartIsRejected`(§5.1 기대값 그대로), `assignmentReleaseBeforeDeployIsRejected`, `assignmentOutsideProjectPeriodIsRejected`, `assignmentOnProjectBoundariesIsAllowed`, `shrinkingProjectPeriodBelowAssignmentsIsConflict` | PASS |
| 3-3 | FE 날짜 검증 (할당 폼, WBS 폼) → 폼 에러, API 미호출 | `AssignmentFormModal.spec.js`(7건), `WbsComponents.spec.js` WbsFormModal 날짜 역전/시작일 누락 | PASS |
| 3-4 | 계획 vs 실제 정합성 — §3.6(a) 전 셀 | `ManMonthCalculatorTest.monthlyMmTable`(6행×4개월) + 반올림 전 정확값 | PASS |
| 3-5 | §3.6(b) plan-vs-actual(P1, 전체) 전 셀·summary, (c) timesheet PLAN/ACTUAL, (d) dashboard | `planVsActualForProjectMatchesWorkedExample`, `planVsActualForAllProjectsMatchesWorkedExample`, `timesheetPlanMatchesWorkedExample`, `timesheetActualMatchesWorkedExample`, `dashboardMatchesWorkedExample`, 스모크 #5·#11·#12 | PASS |
| 4-1 | 신규 이슈 팝업 → 저장 → Grid 즉시 반영 | BE `IssueControllerTest.createdIssueAppearsAtTopOfProjectGrid`; FE `IssueListView.spec.js`(신규 버튼 → 모달 → 제목 입력 → 저장 → `createIssue` 호출 → `listIssues` 재조회 → IssueTable `rows` 첫 행 = 새 이슈), 스모크 #3 | PASS |
| 4-2 | 수정 팝업 pre-fill → 저장 반영 | BE `updatePersistsAllFields`; FE `IssueFormModal.spec.js`(pre-fill, update(id, 전체 payload), 서버 fieldErrors 표시), `IssueListView.spec.js` 수정 흐름 | PASS |
| 4-3 | 정렬(rank 기준)/페이징/size clamp/필터/잘못된 sort/검증 오류/삭제 권한 | `IssueControllerTest` 나머지 8건 | PASS |
| W-1 | WBS 집계: W3.1(1=100.0, 2=90.0, 3=25.0, overall 53.8), 가중치 85.0/52.5, 상태·actualEnd·지연 경계 | `WbsRollupCalculatorTest`(8건), `WbsControllerTest.seedTreeMatchesWorkedExample`, `leafWeightChangeUpdatesParentAndOverall`, 스모크 #6 | PASS |
| W-2 | WBS 권한: 비멤버 USER 쓰기 403 / 멤버 USER 201 / 조회는 누구나 200(`canEdit`) | `nonMemberUserCannotWrite`, `memberUserCanCreate`, `readIsAllowedForEveryone…`, 스모크 #10 | PASS |
| W-3 | WBS 생성(루트·하위·insertAfter), 부모 수정 시 집계 필드 무시, 삭제 cascade+재번호, move(indent/outdent/위·아래), 순환 400, 깊이 6 400, 기간 밖 400, 비멤버 담당자 400, 기간 축소 시 WBS 리프 충돌 409 | `WbsControllerTest` 나머지 12건 | PASS |
| W-4 | WBS FE: visibleRows 접기, 이동 헬퍼, 들여쓰기 렌더러(level×20px), canEdit=false 버튼 미노출, 폼 날짜 역전 차단 | `wbsStore.spec.js`(12), `wbsRenderers.spec.js`(7), `WbsComponents.spec.js`(8) | PASS |
| G-1 | 모든 그리드 AG Grid Community | 코드 감사: `ag-grid-vue3`만 사용, `treeData`/row grouping/serverSide/Enterprise import 없음 | PASS (코드 리뷰) |

---

## 3. 테스트 실행 요약

### 3.1 백엔드 (`mvn -B test`) — 114 / 114 통과

| 클래스 | 테스트 수 | 결과 | 비고 |
|---|---|---|---|
| `AdminControllerTest` (신규) | 52 | 52 PASS | parameterized 403/401 각 14건 포함 |
| `IssueControllerTest` (신규) | 10 | 10 PASS | |
| `FileUploadTest` (신규) | 10 | 10 PASS | `RANDOM_PORT` 컨텍스트 + MockMvc, 413은 실제 Tomcat |
| `WbsControllerTest` (신규) | 16 | 16 PASS | |
| `WbsRollupCalculatorTest` (신규) | 8 | 8 PASS | |
| `ManMonthCalculatorTest` (신규) | 10 | 10 PASS | §3.6(a) 전 셀 |
| `SmokeIntegrationTest` (기존) | 5 | 5 PASS | |
| `CalculatorSmokeTest` (기존) | 3 | 3 PASS | |

공통 헬퍼: `backend/src/test/java/com/sipm/ApiTestSupport.java` (로그인, 코드로 ID 조회, JSON 요청 빌더. 응답은 UTF-8로 읽음).

### 3.2 프론트엔드 (`npx vitest run`) — 73 / 73 통과

| 스펙 | 테스트 수 | 내용 |
|---|---|---|
| `file.spec.js` | 7 | 2MB 경계, 형식/빈 파일, allow-list |
| `AttachmentUploader.spec.js` | 5 | 2MB+1 alert & 업로드 미호출, 정확히 2MB 업로드, 대기열 모드, 형식 alert, 서버 413 alert |
| `router.guard.spec.js` | 9 | USER → admin 6경로 forbidden, ADMIN 통과, 미로그인 redirect, fetchMe/만료, 실제 네비게이션, 사이드바 Admin 메뉴 |
| `http.spec.js` | 6 | Bearer 헤더, 401/403/413/400 처리 |
| `IssueListView.spec.js` | 4 | 생성 → Grid 반영, 다른 페이지에서 생성 시 1페이지 복귀, 빈 제목 차단, 수정 흐름 |
| `IssueFormModal.spec.js` | 5 | edit pre-fill, update payload, 서버 fieldErrors, 프로젝트 미선택 차단, 대기열 첨부 업로드 |
| `AssignmentFormModal.spec.js` | 7 | 계획/실제 날짜 역전, 투입일 누락, 프로젝트 기간 밖, 경계 허용, 사용자 필수, 서버 fieldErrors |
| `wbsStore.spec.js` | 12 | visibleRows, collapse/expand, moveUp/Down/indent/outdent, API 연동 |
| `wbsRenderers.spec.js` | 7 | 들여쓰기, ▶/▼ 토글, XSS 이스케이프, 액션 버튼 헬퍼 |
| `WbsComponents.spec.js` | 8 | 툴바 canEdit, WbsFormModal 검증/pre-fill/rolledUp 비활성/create payload/기간 경고 |
| `smoke.spec.js` (기존) | 3 | |

공통 헬퍼: `frontend/tests/unit/helpers.js`.

### 3.3 빌드 / 통합 스모크

- `npm run build` (node:20-alpine): **성공** (`chunkSizeWarningLimit` 설정으로 경고 없음).
- `docker compose up -d --build`: 3개 컨테이너 기동, `/actuator/health` 200.
- 스모크 (nginx `http://localhost:3000/api` 경유, curl):

| # | 항목 | 결과 |
|---|---|---|
| 1 | admin/user 로그인 200, 잘못된 비밀번호 401 | PASS |
| 2 | user 토큰 → `GET /api/admin/projects` 403 `FORBIDDEN`, 토큰 없음 401 | PASS |
| 3 | 이슈 생성(한글 제목) → 프로젝트 필터 목록 첫 행 | PASS |
| 4 | 1MB 201, 정확히 2MB 201, **2MB+1 → 413**, **3MB → 413** (`FILE_TOO_LARGE`), 다운로드 바이트 동일 + nosniff/no-store 헤더, `.exe` 400, `../../evil.txt` → `evil.txt` & 컨테이너 내 UUID 파일명만 존재 | PASS |
| 5 | plan-vs-actual(P1, 2026-01~04, asOf 03-15) = §3.6(b) 전 값 | PASS |
| 6 | WBS 시드 overall 53.8, 10개, user canEdit=false / kim canEdit=true | PASS |
| 7 | SPA 딥링크 `/admin/projects`, `/projects/1/wbs` → index.html 200 | PASS |
| 8 | kim이 user의 이슈 삭제 → 403, user 본인 삭제 → 204, 업로드 디렉터리 정리됨 | PASS |
| 9 | timesheet PLAN grandTotal 4.76 / kim overAllocated ["2026-03"], dashboard §3.6(d) | PASS |

- 확인 후 `docker compose down -v` 로 컨테이너·볼륨 삭제(시드 초기 상태 유지).

---

## 4. 결함

### 4.1 QA가 이미 수정한 항목 (운영 코드 수정 금지 지시 이전 — 개발자 리뷰 요청)

| ID | 심각도 | 내용 | 수정 | 파일 | 검증 테스트 |
|---|---|---|---|---|---|
| D-01 | Low | 첨부파일이 DB 커밋 **전에** 디스크에 쓰이는데, 보상 삭제가 서비스 메서드 내부 `RuntimeException`에만 걸려 있어 **커밋 단계 실패/외부 롤백** 시 DB 행 없는 고아 파일이 남음(테스트의 `@Transactional` 롤백에서도 재현: 임시 저장소에 파일 누적). | `FileStorageService.deleteOnRollback(Path)` 추가 — `TransactionSynchronization.afterCompletion(STATUS_ROLLED_BACK)`에서 파일 삭제. `AttachmentService.upload`의 try/catch 보상을 이 훅으로 교체. | `backend/src/main/java/com/sipm/attachment/FileStorageService.java:221`, `AttachmentService.java:47-49` | `FileUploadTest` 전체(업로드 전/후 디렉터리 비교), 백엔드 114건 재실행 PASS |
| D-02 | Medium | **체크리스트 4-1 관련.** 이슈 목록 2페이지 이상을 보는 중 "신규 이슈"로 등록하면 `fetchList()`가 현재 페이지를 그대로 다시 읽어 새 이슈(createdAt desc 기준 1페이지 최상단)가 Grid에 나타나지 않음. | `IssueListView.onSaved`에서 create 모드면 `issueStore.page = 0` 후 재조회. | `frontend/src/views/issues/IssueListView.vue:85-90` | `IssueListView.spec.js` "다른 페이지를 보고 있어도 신규 등록 후 첫 페이지로 돌아와 새 행이 보인다" |

### 4.2 테스트 설정 변경 (운영 동작 영향 없음)

| 항목 | 내용 | 파일 |
|---|---|---|
| T-01 | Vitest에서 element-plus가 외부 모듈로 로드되면 Node가 `async-validator`의 **CJS** 빌드를 불러와 `new AsyncValidator()`가 실패하고, Element Plus가 이 오류를 삼켜 **모든 `el-form` 검증이 통과(true)** 처리됨 → 클라이언트 검증 테스트가 거짓 양성/음성. `test.server.deps.inline: ['element-plus']` 추가로 해결. Vite 빌드(브라우저 번들)는 ESM 진입점을 쓰므로 운영에는 해당 없음(빌드 산출물은 `test` 블록 영향 없음). | `frontend/vite.config.js:27` |

### 4.3 개발자 확인 요청 (수정하지 않음, 실패 테스트 없음)

| ID | 심각도 | 내용 | 의심 위치 | 재현 | 기대 vs 실제 | 제안 |
|---|---|---|---|---|---|---|
| O-01 | Low | 다운로드 `Content-Disposition`의 `filename=` 파라미터가 ASCII fallback이 아니라 RFC 2047 인코딩 문자열로 나감 | `backend/src/main/java/com/sipm/attachment/AttachmentController.java:51` (`ContentDisposition.filename(name, UTF_8)`의 Spring 6 동작) | 스모크: `s2.pdf` 업로드 후 다운로드 헤더 확인 | 기대(§4.13): `filename="s2.pdf"; filename*=UTF-8''s2.pdf` / 실제: `filename="=?UTF-8?Q?s2.pdf?="; filename*=UTF-8''s2.pdf` | 최신 브라우저는 `filename*`를 우선하고 FE는 blob + `a.download`로 저장하므로 실사용 영향은 작음. 필요 시 ASCII로 치환한 fallback을 `filename`에 직접 넣고 `filename*`는 별도 헤더 문자열로 구성. |
| O-02 | Info | `WbsFormModal`의 open watch에 `immediate: true`가 없어, `modelValue=true` 상태로 **처음 마운트**되면 pre-fill이 되지 않음(IssueFormModal/AssignmentFormModal은 immediate 사용) | `frontend/src/components/wbs/WbsFormModal.vue:75` | 현재 `WbsView`는 항상 닫힌 상태로 마운트하므로 앱에서는 재현 안 됨. 컴포넌트를 열린 상태로 마운트하면 재현. | 기대: 열릴 때 항상 pre-fill / 실제: 최초 마운트 시 빈 폼 | 다른 모달과 같이 `{ immediate: true }` 추가 |
| O-03 | Info | axios 인터셉터가 **모든 403**을 `/forbidden` 화면으로 이동시킴. 현재는 UI가 권한 없는 버튼(타인 이슈 삭제, 비멤버 WBS 편집)을 숨겨 도달하지 않지만, 화면 상태가 오래되면(예: 할당 해제 직후 WBS 편집) 작업 중 화면이 통째로 바뀜 | `frontend/src/api/http.js:46` | kim으로 WBS 화면을 연 상태에서 ADMIN이 kim 할당을 삭제 → kim이 인라인 편집 | 기대: 메시지 표시 후 현 화면 유지 / 실제: forbidden 화면 이동 | 라우트 진입이 아닌 액션 요청의 403은 `ElMessage`로만 처리하는 옵션 검토 (사양 §6.4와 다르므로 사양 결정 필요) |
| O-04 | Info | WBS 리프 status가 DONE인 상태에서 그리드 인라인으로 진척률만 낮추면 서버 규칙(W3 리프 규칙 1: DONE → 100 강제)으로 100으로 되돌아감 — 사양대로지만 사용자에게는 "저장이 안 됨"으로 보일 수 있음 | `backend/src/main/java/com/sipm/wbs/WbsService.java:250` | DONE 리프의 진척률 셀을 80으로 수정 | 사양상 정상 | UX 안내(툴팁) 또는 진척률 < 100 입력 시 상태를 IN_PROGRESS로 바꾸는 규칙 추가 여부를 사양에서 결정 |

> 4.3 항목에 대응하는 실패 테스트는 두지 않았습니다(사양 위반이 아니거나 앱 흐름에서 재현되지 않음). **known failure: 없음.**

---

## 5. 코드 감사 메모 (결함 아님)

- 보안: URL 레벨 `/api/admin/**` hasRole + 클래스 레벨 `@PreAuthorize` 이중 방어, `AccessDeniedException`/`AuthenticationException` 전용 핸들러로 500 변환 버그 없음, JWT 필터가 매 요청 DB 재조회(비활성 즉시 차단) 확인.
- 업로드: 원본 파일명은 경로에 쓰지 않고 UUID 저장명 + 정규식 재검증 + `startsWith(rootReal)` 확인. 서버가 확장자로 MIME 결정(클라이언트 Content-Type 무시) 확인.
- MM 계산: 반올림 전 합산 후 마지막에만 scale 2 (`cumulativeActualMM` 2.77 ≠ 표시 합 2.78) 확인.
- 이슈 정렬: `status`/`priority`는 rank 컬럼으로 매핑(알파벳 순 아님), tie-breaker `id desc` 확인.
- WBS: 트리를 프로젝트 단위 1회 조회 후 메모리 조립(N+1 없음), 이동 시 "새 부모 레벨 + 서브트리 높이" 깊이 검사 확인.
- AG Grid: 모든 그리드가 `ag-grid-vue3` Community, Enterprise 기능 미사용.

---

## 6. 남은 위험 / 한계 (정직한 범위 표시)

1. **실제 브라우저 UI는 확인하지 않았습니다.** 모달/Grid 상호작용은 jsdom 컴포넌트 테스트로 검증했습니다. 특히 AG Grid의 실제 행 DOM 렌더링, 인라인 편집(`cellValueChanged`), 정렬 헤더 클릭 → 서버 sort 연동, ECharts 차트/간트 렌더링, el-select 드롭다운 선택은 jsdom에서 신뢰성 있게 재현되지 않아 **컴포넌트 props/스토어/이벤트 수준까지만** 검증했습니다(예: IssueListView 테스트는 IssueTable에 전달된 `rows`로 "새 행 표시"를 확인).
2. 프로젝트 선택(el-select) 상호작용 대신 테스트에서는 필터의 기본 프로젝트(`defaultProjectId`) 경로를 사용했습니다.
3. 실제 Tomcat(RANDOM_PORT) 테스트는 413만 수행했습니다. JDK `HttpURLConnection`이 스트리밍 업로드 중 401 응답을 읽지 못해(“cannot retry due to server authentication”) 인증 없는 실서버 업로드 테스트는 제외했습니다(인증 없는 401은 MockMvc와 스모크에서 확인).
4. 백엔드 통합 테스트는 H2(PostgreSQL 모드)에서 실행됩니다. PostgreSQL 고유 동작은 compose 스모크로만 확인했습니다.
5. 동시성(동시 업로드/동시 WBS 이동에 의한 sort_order 경합), 성능, 대용량 데이터는 범위 밖입니다.
6. 첨부/이슈 삭제의 "커밋 후 파일 삭제"는 `@Transactional` 테스트에서는 커밋이 일어나지 않으므로 스모크(실 스택)로만 확인했습니다.
7. `package.json`의 `element-plus: ^2.8.6`가 lock 기준 2.14.7로 설치됩니다. T-01과 같은 버전 의존 이슈가 있으니 업그레이드 시 프론트 테스트를 반드시 재실행하세요.

---

## 7. 재실행 방법

```bash
# 백엔드 (Maven 캐시 볼륨 sipm-m2 사용)
docker run --rm -v "<abs>/backend:/app" -v sipm-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B test

# 프론트엔드 테스트 / 빌드 (호스트 Node v14 사용 금지)
docker run --rm -v "<abs>/frontend:/app" -w /app node:20-alpine sh -c "npm ci && npx vitest run"
docker run --rm -v "<abs>/frontend:/app" -w /app node:20-alpine sh -c "npm run build"

# 통합 스모크
docker compose up -d --build
#   → http://localhost:3000/api 로 §3.3 항목 확인 (Git Bash에서 한글 JSON은 UTF-8 파일로 --data-binary 전송)
docker compose down -v
```

Git Bash에서 Docker 볼륨 경로를 넘길 때는 `MSYS_NO_PATHCONV=1`을 앞에 붙이세요.

---

## 8. 개발자 수정 내역 (QA 후속)

- 작성: Full-stack Engineer, 2026-10-23
- QA 추가 테스트는 약화하지 않았습니다. 변경은 기존 단언을 더 엄격하게 바꾸거나 새 케이스를 추가하는 방향만 있습니다.

### 8.1 QA가 수정한 운영 코드 리뷰

| ID | 결정 | 내용 | 파일 |
|---|---|---|---|
| D-01 | **유지 + 개선** | 롤백 시 파일 삭제 훅(`deleteOnRollback`)은 적절해서 유지했습니다. 개선점: 훅을 디스크 **쓰기 전에** 등록하도록 순서를 바꿨습니다. 그래야 쓰기 도중 예외로 롤백될 때 부분적으로 쓰인 파일도 정리됩니다(없는 파일 삭제는 무시). | `backend/src/main/java/com/sipm/attachment/AttachmentService.java` (upload) |
| D-02 | **유지** | 신규 등록 후 page=0으로 돌아가는 처리는 체크리스트 4-1 의도와 맞습니다. 수정 모드는 현재 페이지를 유지합니다. | `frontend/src/views/issues/IssueListView.vue` (onSaved) |
| T-01 | **유지** | `test.server.deps.inline: ['element-plus']`는 vitest의 `test` 블록에만 적용되고 `vite build`의 입력·설정에는 관여하지 않으므로 운영 번들에 영향이 없습니다(사용자 지시에 따라 이번에는 `npm run build`를 다시 돌리지 않았습니다. QA 3.3절 빌드 성공은 이 설정이 들어간 상태 기준입니다). | `frontend/vite.config.js` |

### 8.2 개발자 확인 요청 항목 처리

| ID | 처리 | 수정 내용 | 파일 | 테스트 |
|---|---|---|---|---|
| O-01 | 수정 | `Content-Disposition`을 직접 만든다: `attachment; filename="<ASCII 대체>"; filename*=UTF-8''<퍼센트 인코딩>`. ASCII 대체는 출력 가능한 ASCII만 남기고 비ASCII·`"`·`\`를 `_`로 바꾼다. `filename*`는 RFC 5987 attr-char 외 바이트를 `%XX`로 인코딩한다. RFC 2047(`=?UTF-8?Q?…?=`) 출력은 없어졌다. | `backend/src/main/java/com/sipm/attachment/AttachmentController.java` (`contentDisposition`) | `FileUploadTest.oneKilobyteUploadSucceedsAndDownloadsSameBytes`: 헤더 **전체 문자열 일치**로 강화(`filename="___.txt"; filename*=UTF-8''%EB%B3%B4%EA%B3%A0%EC%84%9C.txt`). 신규 `ContentDispositionTest`(3건: ASCII 그대로, 한글/공백, 따옴표·역슬래시·`*()` 이스케이프) |
| O-02 | 수정 | 모달 pre-fill watch에 `{ immediate: true }`를 추가해 열린 상태로 처음 마운트돼도 값이 채워지게 했다. 같은 패턴을 점검한 결과 IssueFormModal·AssignmentFormModal은 이미 immediate였고, **ProjectFormModal·UserFormModal**에도 같은 문제가 있어 함께 고쳤다. | `frontend/src/components/wbs/WbsFormModal.vue`, `frontend/src/components/admin/ProjectFormModal.vue`, `frontend/src/components/admin/UserFormModal.vue` | `WbsComponents.spec.js` "O-02: 열린 상태(modelValue=true)로 처음 마운트돼도 pre-fill" 추가 |
| O-03 | 수정 | 403 처리: **GET**(method 미지정 = axios 기본 GET 포함)이면 `/forbidden`으로 이동한다. **POST/PUT/PATCH/DELETE**이면 `ElMessage.error(서버 message ?? '접근 권한이 없습니다.')`만 표시하고 화면을 유지한다. GET도 `config.skipForbiddenRedirect: true`로 이동을 끌 수 있다. specs.md §6.4와 Implementation notes C-2도 함께 개정했다. | `frontend/src/api/http.js` | `http.spec.js`: 기존 403 케이스를 GET 기준으로 바꾸고(forbidden 이동 + 메시지 없음), method 미지정 GET, 변경 요청 5종(post/put/patch/delete/대문자 PUT) 메시지만·이동 없음, 기본 문구, `skipForbiddenRedirect` 케이스를 추가했다 |
| O-04 | 수정 (리드 결정) | 리프 규칙: **이전 상태가 DONE**이고 요청도 `status=DONE`인데 `progress < 100`이면 → **IN_PROGRESS로 전환하고 progress는 유지**한다. 상태를 DONE으로 **바꾸는** 요청(신규 포함)은 progress 100으로 강제한다(기존과 같음). NOT_STARTED 자동 전환 규칙은 그대로다. `docs/specs-wbs.md` W3 "리프 입력 규칙" 1번을 개정했다. | `backend/src/main/java/com/sipm/wbs/WbsService.java` (`applyLeafFields`), `docs/specs-wbs.md` | `WbsControllerTest.loweringProgressOfDoneLeafSwitchesToInProgress` 추가(DONE 리프 진척률 80 → IN_PROGRESS/80.0, 부모 '분석' 90.0, 다시 DONE+60 → DONE/100.0). 기존 `leafInputRulesForStatusAndProgress`(신규 DONE+10 → 100)도 그대로 통과 |

### 8.3 주석 보강 (엔지니어 보고서의 known gap)
코딩 스타일 요청 이전에 작성된 클래스 중 로직이 자명하지 않은 곳에만 짧은 한국어 주석을 추가했습니다(동작 변경 없음): `SortResolver`(쉼표 분리 주의, clamp), `Issue`(rank 컬럼 이유, `@Formula`), `AssignmentResponse.state`, `UserService.update`(자기 수정 금지, 비밀번호 미변경 규칙), `IssueService.apply`, `IssueController` SORTS 매핑, `LikePatterns`(와일드카드 이스케이프), `AttachmentService.delete`, `DateRange`.

### 8.4 실행한 테스트 (변경 범위 한정)
사용자 지시에 따라 **전체 스위트(백엔드 `mvn test` 전체, 프론트 `vitest run` 전체), `npm run build`, docker compose 스모크는 의도적으로 다시 실행하지 않았습니다.** 변경과 관련된 테스트만 실행했습니다. 스택은 내려가 있는 상태입니다(이번 작업에서 기동하지 않음).

| 영역 | 명령 (컨테이너) | 결과 |
|---|---|---|
| Backend | `mvn -B test -Dtest=FileUploadTest,WbsControllerTest,ContentDispositionTest` (maven:3.9-eclipse-temurin-17) | **30 / 30 통과** (FileUploadTest 10, WbsControllerTest 17, ContentDispositionTest 3). 주석만 바꾼 클래스도 이 실행에서 함께 컴파일됨 |
| Frontend | `npx vitest run tests/unit/http.spec.js tests/unit/WbsComponents.spec.js` (node:20-alpine) | **18 / 18 통과** (http.spec 9, WbsComponents.spec 9) |

- ProjectFormModal·UserFormModal은 전용 spec 파일이 없어서 O-02 변경(immediate 추가)은 WbsFormModal 테스트와 같은 패턴으로만 확인했습니다.
- 다른 화면(IssueListView, IssueFormModal, AssignmentFormModal)은 코드를 변경하지 않아 해당 spec은 실행하지 않았습니다.
