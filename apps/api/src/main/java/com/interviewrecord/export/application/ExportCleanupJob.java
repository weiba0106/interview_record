package com.interviewrecord.export.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 定期清理已过期的导出文件，避免一次性下载数据无限堆积。 */
@Component
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ExportCleanupJob {
    private final ExportService exports;

    public ExportCleanupJob(ExportService exports) {
        this.exports = exports;
    }

    @Scheduled(fixedDelayString = "${app.export.cleanup-delay:PT1H}", initialDelayString = "PT1H")
    @Transactional
    public void removeExpiredExports() {
        exports.removeExpired();
    }
}
