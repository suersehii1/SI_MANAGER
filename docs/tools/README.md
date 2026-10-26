# 문서 도구 (docs/tools)

아키텍처 문서와 사용 매뉴얼의 **원본은 Markdown**(`docs/architecture.md`, `docs/user-manual.md`)이고, **배포본(Word·PDF)은 이 폴더의 스크립트로 만든다.** 원본을 고친 뒤에는 아래 순서로 배포본을 다시 만든다.

## 파일

| 파일 | 하는 일 |
|---|---|
| `build-docs.ps1` | 배포본 전체를 한 번에 만든다 (아래 3개 단계 실행) |
| `build-docs.mjs` | Mermaid 그림 → PNG, 변환용 원고, PDF 생성 (Docker의 Playwright 이미지에서 실행, 나눔고딕 글꼴 사용) |
| `postprocess-docx.py` | pandoc이 만든 Word 파일의 글꼴(맑은 고딕)·표 테두리·머리글 행 배경 다듬기 |
| `capture-manual-screens.mjs` | 사용 매뉴얼용 화면 캡처 (실행 중인 시스템에 admin으로 접속, 데이터는 바꾸지 않음) |

## 배포본 다시 만들기

준비: Docker Desktop 실행, Python 3 설치 (PC에 Node·pandoc은 없어도 됨 — Docker 안에서 실행)

```powershell
# 저장소 최상위 폴더에서
powershell -ExecutionPolicy Bypass -File docs\tools\build-docs.ps1
```

결과:

| 위치 | 내용 |
|---|---|
| `docs/export/architecture.pdf`, `docs/export/architecture.docx` | 아키텍처 문서 배포본 |
| `docs/export/user-manual.pdf`, `docs/export/user-manual.docx` | 사용 매뉴얼 배포본 |
| `docs/images/diagrams/*.png` | Mermaid 그림을 그린 이미지 (Word·PDF에 들어감) |
| `docs/export/build/` | 변환 중간 파일 (git에 올리지 않음) |

## 화면 캡처 다시 하기

화면이 바뀌어 사용 매뉴얼의 캡처를 새로 찍어야 할 때 쓴다. 시스템이 실행 중(`http://localhost:3000`)이어야 한다.

```powershell
docker run --rm -v "${PWD}\docs\tools:/work:ro" -v "${PWD}\docs\images\manual:/out" `
  mcr.microsoft.com/playwright:v1.48.2-jammy sh -c `
  "cd /tmp && npm init -y >/dev/null && npm i -s playwright@1.48.2 >/dev/null 2>&1 && cp /work/capture-manual-screens.mjs /tmp/c.mjs && node /tmp/c.mjs"
```

- 캡처에 쓸 프로젝트는 환경 변수 `PROJECT_ID`(기본 `3`)로 바꾼다. WBS와 인원 할당 데이터가 있는 프로젝트를 고른다.
- 등록 팝업은 열어서 찍기만 하고 저장하지 않는다. 밤 스킨과 일반 사용자 화면은 브라우저 안에서만 상태를 바꿔 찍는다.

## 주의

- **Markdown 굵은 글씨**: `**...)**에`처럼 굵은 글씨가 기호(`)`, `"` 등)로 끝나고 바로 뒤에 한글이 붙으면 굵게 표시되지 않는다(GitHub도 같음). `**nginx**(웹 서버)에`처럼 낱말에서 굵은 글씨를 끝낸다.
- **PowerShell 스크립트 인코딩**: `build-docs.ps1`은 한글이 있어 **UTF-8(BOM 포함)**으로 저장해야 Windows PowerShell 5.1에서 깨지지 않는다.
