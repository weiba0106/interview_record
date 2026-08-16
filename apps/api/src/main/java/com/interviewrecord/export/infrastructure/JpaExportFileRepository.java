package com.interviewrecord.export.infrastructure;

import com.interviewrecord.export.domain.ExportFile;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaExportFileRepository extends JpaRepository<ExportFile, Long> {
    Optional<ExportFile> findByTokenHashAndUserId(byte[] tokenHash, Long userId);

    /** 条件更新：只有尚未下载过的文件可以被标记为已下载（一次性语义）。 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ExportFile f set f.downloadedAt = :now where f.id = :id and f.downloadedAt is null")
    int markDownloaded(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("delete from ExportFile f where f.expiresAt < :now")
    long deleteAllByExpiresAtBefore(@Param("now") Instant now);
}
