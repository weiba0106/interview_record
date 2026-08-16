package com.interviewrecord.common.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * 富文本白名单清洗（PRD §7.5 / §10）：
 * 允许少量排版标签与 http/https/mailto 链接；移除脚本、事件属性、
 * javascript: 协议与一切白名单之外的标签，链接强制附加 noopener noreferrer。
 * 写入与读出都经过清洗，输出端不再信任数据库中的历史内容。
 */
@Component
public class RichTextSanitizer {
    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li",
                    "h2", "h3", "blockquote", "code", "pre", "a")
            .addAttributes("a", "href", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    /** 空白输入归一为 null；输出为仅含白名单标签的 HTML。 */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) return null;
        return Jsoup.clean(html, "", SAFELIST, new Document.OutputSettings().prettyPrint(false));
    }
}
