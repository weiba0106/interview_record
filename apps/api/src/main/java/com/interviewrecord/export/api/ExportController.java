package com.interviewrecord.export.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.export.application.ExportService;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

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

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExportDtos.ExportData> json() {
        String filename = "interview-record-export-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(exports.export(currentUser.require().id()));
    }
}
