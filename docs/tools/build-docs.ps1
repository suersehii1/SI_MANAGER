# 문서 배포본(PDF·Word) 한 번에 만들기 (Windows PowerShell, Docker Desktop 필요)
# 사용법: 저장소 최상위 폴더에서  powershell -File docs\tools\build-docs.ps1
#
# 1) Mermaid 그림 → PNG, 변환용 원고, PDF   (Playwright 이미지 + 나눔고딕 글꼴)
# 2) 변환용 원고 → Word(.docx)              (pandoc 이미지)
# 3) Word 글꼴(맑은 고딕)·표 테두리 다듬기   (Python)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot\..\..").Path

Write-Output '[1/3] 그림·PDF 생성'
docker run --rm -v "${repo}:/repo" mcr.microsoft.com/playwright:v1.48.2-jammy sh -c `
  "apt-get update -qq >/dev/null 2>&1 && apt-get install -y -qq fonts-nanum >/dev/null 2>&1 && fc-cache -f >/dev/null && cd /tmp && npm init -y >/dev/null && npm i -s playwright@1.48.2 marked@14.1.3 mermaid@11.4.1 >/dev/null 2>&1 && cp /repo/docs/tools/build-docs.mjs /tmp/b.mjs && node /tmp/b.mjs"
if ($LASTEXITCODE -ne 0) { throw '그림·PDF 생성 실패' }

Write-Output '[2/3] Word 생성'
foreach ($name in 'architecture', 'user-manual') {
  docker run --rm -v "${repo}:/repo" -w /repo/docs/export/build pandoc/core:3.5 `
    "$name.md" -f gfm -t docx -o "../$name.docx" --resource-path=.
  if ($LASTEXITCODE -ne 0) { throw "Word 생성 실패: $name" }
}

Write-Output '[3/3] Word 다듬기'
python -I "$repo\docs\tools\postprocess-docx.py" "$repo\docs\export\architecture.docx" "$repo\docs\export\user-manual.docx"
if ($LASTEXITCODE -ne 0) { throw 'Word 다듬기 실패' }

Write-Output "완료: $repo\docs\export"
