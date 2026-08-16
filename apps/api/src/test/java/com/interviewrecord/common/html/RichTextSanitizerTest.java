package com.interviewrecord.common.html;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RichTextSanitizerTest {
    private final RichTextSanitizer sanitizer = new RichTextSanitizer();

    @Test
    void stripsScriptsAndEventHandlers() {
        String cleaned = sanitizer.sanitize(
                "<p onclick=\"steal()\">面试过程<script>alert(1)</script><b>重点</b></p>");

        assertThat(cleaned).doesNotContain("script", "onclick", "alert");
        assertThat(cleaned).contains("<b>重点</b>");
    }

    @Test
    void keepsWhitelistedFormattingAndLists() {
        String cleaned = sanitizer.sanitize("<h3>复盘</h3><ul><li>表现</li><li><em>改进</em></li></ul>");

        assertThat(cleaned).contains("<h3>复盘</h3>", "<li>表现</li>", "<li><em>改进</em></li>");
    }

    @Test
    void dropsJavascriptUrlsButKeepsSafeLinksWithNoopener() {
        String cleaned = sanitizer.sanitize(
                "<a href=\"javascript:alert(1)\">坏链接</a><a href=\"https://example.com\" target=\"_blank\">官网</a>");

        assertThat(cleaned).doesNotContain("javascript:");
        assertThat(cleaned).contains("href=\"https://example.com\"", "rel=\"noopener noreferrer\"");
    }

    @Test
    void blankOrNullInputBecomesNull() {
        assertThat(sanitizer.sanitize(null)).isNull();
        assertThat(sanitizer.sanitize("   ")).isNull();
    }

    @Test
    void unknownTagsAreRemovedButTheirTextIsKept() {
        String cleaned = sanitizer.sanitize("<custom-widget>保留文本</custom-widget>");

        assertThat(cleaned).doesNotContain("custom-widget");
        assertThat(cleaned).contains("保留文本");
    }
}
