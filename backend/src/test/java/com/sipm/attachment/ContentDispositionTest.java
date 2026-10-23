package com.sipm.attachment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** O-01: 다운로드 Content-Disposition 헤더 형식 (ASCII fallback + RFC 5987 filename*). */
class ContentDispositionTest {

    @Test
    void asciiNameIsKeptAsIs() {
        assertThat(AttachmentController.contentDisposition("s2.pdf"))
                .isEqualTo("attachment; filename=\"s2.pdf\"; filename*=UTF-8''s2.pdf");
    }

    @Test
    void nonAsciiIsReplacedInFallbackAndPercentEncodedInExtendedValue() {
        assertThat(AttachmentController.contentDisposition("보고서 v1.pdf"))
                .isEqualTo("attachment; filename=\"___ v1.pdf\"; filename*=UTF-8''%EB%B3%B4%EA%B3%A0%EC%84%9C%20v1.pdf");
    }

    @Test
    void quotesBackslashAndNonAttrCharsAreEscaped() {
        String header = AttachmentController.contentDisposition("a\"b\\c*(1).txt");
        assertThat(header).startsWith("attachment; filename=\"a_b_c*(1).txt\";");
        assertThat(header).endsWith("filename*=UTF-8''a%22b%5Cc%2A%281%29.txt");
        assertThat(header).doesNotContain("=?UTF-8?");
    }
}
