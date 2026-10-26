"""pandoc으로 만든 Word(.docx) 파일 다듬기.

1) 글꼴: 본문·제목 글꼴을 '맑은 고딕'으로 지정 (코드용 Consolas는 그대로)
2) 표: 기본 표 스타일에 테두리와 머리글 행 배경색을 넣는다 (pandoc 기본 표는 테두리가 없음)

사용법: python -I postprocess-docx.py <입력.docx> [<입력2.docx> ...]   (파일을 직접 고친다)
"""
import re
import shutil
import sys
import tempfile
import zipfile

FONT = '맑은 고딕'
RFONTS = f'<w:rFonts w:ascii="{FONT}" w:hAnsi="{FONT}" w:eastAsia="{FONT}" w:cs="{FONT}" />'

# 표 테두리(얇은 회청색 선)
BORDERS = (
    '<w:tblBorders>'
    + ''.join(f'<w:{side} w:val="single" w:sz="4" w:space="0" w:color="B8C4D6" />'
              for side in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'))
    + '</w:tblBorders>'
)
HEADER_RPR = '<w:rPr><w:b /><w:color w:val="1D2D4A" /></w:rPr>'
HEADER_SHD = '<w:shd w:val="clear" w:color="auto" w:fill="E8EEF7" />'


def fix_styles(xml: str) -> str:
    # 1) Consolas(코드)가 아닌 글꼴 지정은 모두 맑은 고딕으로
    def repl(m):
        return m.group(0) if 'Consolas' in m.group(0) else RFONTS
    xml = re.sub(r'<w:rFonts\b[^>]*/>', repl, xml)

    # 2) 기본 표 스타일(styleId="Table") 찾기 — 속성 순서는 pandoc 버전마다 다를 수 있다
    m = re.search(r'(<w:style\b[^>]*w:styleId="Table"[^>]*>)(.*?)(</w:style>)', xml, re.S)
    if not m:
        raise SystemExit('표 스타일(Table)을 찾지 못했습니다.')
    body = m.group(2)
    if '<w:tblBorders>' in body:
        return xml  # 이미 다듬은 파일

    # 2-1) 표 전체 테두리: Word 규칙상 tblPr 안에서 tblInd 다음, tblCellMar 앞에 와야 한다
    main_tblpr = body.split('<w:tblStylePr', 1)[0]
    if '<w:tblCellMar>' in main_tblpr:
        new_main = main_tblpr.replace('<w:tblCellMar>', BORDERS + '<w:tblCellMar>', 1)
    else:
        new_main = main_tblpr.replace('</w:tblPr>', BORDERS + '</w:tblPr>', 1)
    body = new_main + body[len(main_tblpr):]

    # 2-2) 머리글 행: 기존 firstRow 블록에 굵은 글씨(rPr, 맨 앞)와 배경색(tcPr 끝)을 넣는다
    fr = re.search(r'(<w:tblStylePr w:type="firstRow">)(.*?)(</w:tblStylePr>)', body, re.S)
    if fr:
        inner = fr.group(2)
        if '<w:tcPr>' in inner:
            inner = inner.replace('</w:tcPr>', HEADER_SHD + '</w:tcPr>', 1)
        else:
            inner += '<w:tcPr>' + HEADER_SHD + '</w:tcPr>'
        body = body[:fr.start()] + fr.group(1) + HEADER_RPR + inner + fr.group(3) + body[fr.end():]
    else:
        body += '<w:tblStylePr w:type="firstRow">' + HEADER_RPR + '<w:tcPr>' + HEADER_SHD + '</w:tcPr></w:tblStylePr>'
    return xml[:m.start()] + m.group(1) + body + m.group(3) + xml[m.end():]


def process(path: str) -> None:
    tmp = tempfile.mktemp(suffix='.docx')
    with zipfile.ZipFile(path) as src, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            data = src.read(item.filename)
            if item.filename == 'word/styles.xml':
                data = fix_styles(data.decode('utf-8')).encode('utf-8')
            dst.writestr(item, data)
    shutil.move(tmp, path)
    print('다듬기 완료:', path)


if __name__ == '__main__':
    for p in sys.argv[1:]:
        process(p)
