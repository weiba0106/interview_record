package com.interviewrecord.tracking.infrastructure;

import com.interviewrecord.tracking.domain.PositionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaManagedPositionStatusRepository extends JpaRepository<PositionStatus, Long> {
    Optional<PositionStatus> findByIdAndUserId(Long id, Long userId);

    List<PositionStatus> findAllByUserIdOrderBySortOrderAsc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    @Query("SELECT COALESCE(MAX(s.sortOrder), 0) FROM PositionStatus s WHERE s.userId = :userId")
    int maxSortOrder(@Param("userId") Long userId);
}
