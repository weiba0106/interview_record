package com.interviewrecord.export.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.export.application.ExportService;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/export")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ExportController {
    private final CurrentUser currentUser;
    private final ExportService exports;

    public ExportController(CurrentUser currentUser, ExportService exports) {
        this.currentUser = currentUser;
        this.exports = exports;
    }

    /** 生成 CSV ZIP（companies/positions/rounds/questions/schedules/statuses 等 CSV，UTF-8 BOM）。 */
    @PostMapping("/csv")
    @ResponseStatus(HttpStatus.CREATED)
    ExportDtos.ExportCreatedResponse csv() {
        return exports.createCsvExport(currentUser.require().id());
    }

    /** 生成 JSON 完整备份，与 CSV 相同的一次性下载语义。 */
    @PostMapping("/json")
    @ResponseStatus(HttpStatus.CREATED)
    ExportDtos.ExportCreatedResponse json() {
        return exports.createJsonExport(currentUser.require().id());
    }

    /** 一次性下载：仅限生成者本人，30 分钟有效，下载一次后失效。 */
    @GetMapping("/download/{token}")
    ResponseEntity<byte[]> download(@PathVariable String token) {
        ExportService.ExportDownload download = exports.download(currentUser.require().id(), token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(download.bytes());
    }
}
