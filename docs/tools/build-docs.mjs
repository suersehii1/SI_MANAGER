// 문서 배포본 생성 스크립트 (Docker 안 Playwright 이미지에서 실행)
// 1) docs/*.md 의 Mermaid 그림을 PNG로 그린다 → docs/images/diagrams/{문서}-{번호}.png
// 2) Mermaid 블록을 그 PNG 이미지로 바꾼 "변환용 원고"를 만든다 → docs/export/build/{문서}.md (pandoc이 Word로 변환)
// 3) 변환용 원고를 HTML로 바꿔 PDF로 인쇄한다 → docs/export/{문서}.pdf
// 실행 방법은 docs/tools/README.md 참고
import { chromium } from 'playwright'
import { marked } from 'marked'
import fs from 'node:fs'
import path from 'node:path'

const DOCS = process.env.DOCS_DIR || '/repo/docs'
const DOCUMENTS = [
  { name: 'architecture', title: '전사 Project 관리 시스템 — 아키텍처 문서' },
  { name: 'user-manual', title: '전사 Project 관리 시스템 — 사용 매뉴얼' }
]
const MERMAID_JS = '/tmp/node_modules/mermaid/dist/mermaid.min.js'
// 한국어 글꼴: 컨테이너에 fonts-nanum(나눔고딕)을 설치해서 쓴다 (설치하지 않으면 중국어 글꼴로 대체되어 글자 간격이 어색해짐)
const FONT = "'NanumGothic', 'Nanum Gothic', 'Malgun Gothic', sans-serif"

fs.mkdirSync(path.join(DOCS, 'images/diagrams'), { recursive: true })
fs.mkdirSync(path.join(DOCS, 'export/build'), { recursive: true })

const browser = await chromium.launch()

// ---------- 1) Mermaid 그림을 PNG로 ----------
const diagramPage = await browser.newPage({ deviceScaleFactor: 3 }) // 3배 해상도로 선명하게
await diagramPage.setContent(`<html><body style="margin:0;background:#fff"><div id="box" style="display:inline-block;padding:16px;background:#fff"></div></body></html>`)
await diagramPage.addScriptTag({ path: MERMAID_JS })
await diagramPage.evaluate((font) => {
  window.mermaid.initialize({
    startOnLoad: false, theme: 'default', fontFamily: font, securityLevel: 'loose',
    themeVariables: { fontFamily: font, fontSize: '15px' },
    // 글자 폭을 SVG 글자로 재서 한글 간격이 어긋나지 않게 한다
    flowchart: { htmlLabels: false, useMaxWidth: false },
    sequence: { useMaxWidth: false },
    er: { useMaxWidth: false }
  })
}, FONT)

async function renderMermaid(code, outFile, index) {
  const svg = await diagramPage.evaluate(async ({ code, index }) => {
    const { svg } = await window.mermaid.render(`d${index}`, code)
    return svg
  }, { code, index })
  await diagramPage.evaluate((svg) => { document.getElementById('box').innerHTML = svg }, svg)
  await diagramPage.locator('#box').screenshot({ path: outFile })
}

// ---------- 2) 변환용 원고 + 3) PDF ----------
const CSS = `
  @page { size: A4; margin: 18mm 16mm 20mm 16mm; }
  body { font-family: ${FONT}; font-size: 10.5pt; line-height: 1.6; color: #222; }
  h1 { font-size: 20pt; color: #1d2d4a; border-bottom: 3px solid #4a6fa5; padding-bottom: 6px; }
  h2 { font-size: 15pt; color: #1d2d4a; border-bottom: 1px solid #c9d3e3; padding-bottom: 4px; margin-top: 26px; page-break-after: avoid; }
  h3 { font-size: 12.5pt; color: #2b4570; margin-top: 18px; page-break-after: avoid; }
  table { border-collapse: collapse; width: 100%; margin: 8px 0 14px; font-size: 9.5pt; page-break-inside: auto; }
  th, td { border: 1px solid #b8c4d6; padding: 5px 7px; vertical-align: top; text-align: left; }
  th { background: #e8eef7; color: #1d2d4a; }
  tr { page-break-inside: avoid; }
  code { font-family: Consolas, 'D2Coding', monospace; background: #f1f3f6; padding: 1px 4px; border-radius: 3px; font-size: 9pt; }
  pre { background: #f6f8fa; border: 1px solid #dfe3e8; padding: 10px; border-radius: 4px; white-space: pre-wrap; font-size: 9pt; page-break-inside: avoid; }
  pre code { background: none; padding: 0; }
  img { max-width: 100%; border: 1px solid #d5dbe5; margin: 6px 0; page-break-inside: avoid; }
  p:has(> img) { page-break-inside: avoid; text-align: center; }
  blockquote { margin: 10px 0; padding: 8px 12px; background: #f3f6fb; border-left: 4px solid #4a6fa5; color: #333; }
  hr { border: none; border-top: 1px solid #dfe3e8; margin: 18px 0; }
  a { color: #2f5a9e; text-decoration: none; }
`

let counter = 0
for (const doc of DOCUMENTS) {
  const source = fs.readFileSync(path.join(DOCS, `${doc.name}.md`), 'utf-8')

  // Mermaid 블록 → PNG 이미지 링크
  let n = 0
  const blocks = []
  const exportMd = source.replace(/```mermaid\n([\s\S]*?)```/g, (_, code) => {
    n += 1
    const file = `images/diagrams/${doc.name}-${n}.png`
    blocks.push({ code, file })
    return `![그림 ${n}](${file})`
  })
  for (const b of blocks) {
    await renderMermaid(b.code, path.join(DOCS, b.file), counter++)
  }
  // 변환용 원고는 docs/export/build/ 에 두되, 이미지 경로는 docs/ 기준(../../images/...)으로 맞춘다
  const buildMd = exportMd.replace(/\]\((images\/[^)]+)\)/g, '](../../$1)')
  fs.writeFileSync(path.join(DOCS, 'export/build', `${doc.name}.md`), buildMd, 'utf-8')
  console.log(`원고 생성: export/build/${doc.name}.md (그림 ${blocks.length}개)`)

  // PDF: docs/ 를 기준 경로로 HTML을 만들어 인쇄
  const html = `<!doctype html><html lang="ko"><head><meta charset="utf-8">
    <base href="file://${DOCS}/"><title>${doc.title}</title><style>${CSS}</style></head>
    <body>${marked.parse(exportMd, { gfm: true })}</body></html>`
  const htmlFile = path.join(DOCS, 'export/build', `${doc.name}.html`)
  fs.writeFileSync(htmlFile, html, 'utf-8')
  const page = await browser.newPage()
  await page.goto(`file://${htmlFile}`, { waitUntil: 'load' })
  await page.pdf({
    path: path.join(DOCS, 'export', `${doc.name}.pdf`),
    format: 'A4',
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: `<div style="font-size:8px;width:100%;text-align:right;padding-right:16mm;color:#888;font-family:${FONT}">${doc.title}</div>`,
    footerTemplate: `<div style="font-size:8px;width:100%;text-align:center;color:#888;"><span class="pageNumber"></span> / <span class="totalPages"></span></div>`,
    margin: { top: '18mm', bottom: '20mm', left: '16mm', right: '16mm' }
  })
  await page.close()
  console.log(`PDF 생성: export/${doc.name}.pdf`)
}

await browser.close()
