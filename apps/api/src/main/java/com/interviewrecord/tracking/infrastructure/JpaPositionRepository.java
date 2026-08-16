package com.interviewrecord.tracking.infrastructure;

import com.interviewrecord.tracking.domain.Position;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByIdAndUserId(Long id, Long userId);

    List<Position> findAllByUserId(Long userId);

    List<Position> findAllByUserIdAndArchivedOrderByUpdatedAtDesc(Long userId, boolean archived);

    long countByUserIdAndArchived(Long userId, boolean archived);

    long countByUserIdAndCompanyId(Long userId, Long companyId);

    long countByUserIdAndStatusId(Long userId, Long statusId);

    long countByUserIdAndJobTypeId(Long userId, Long jobTypeId);

    long countByUserIdAndStatusIdInAndArchived(Long userId, Collection<Long> statusIds, boolean archived);

    @Query("SELECT p.companyId, COUNT(p) FROM Position p WHERE p.userId = :userId GROUP BY p.companyId")
    List<Object[]> countByUserIdGroupedByCompany(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM Position p WHERE p.userId = :userId
            AND (:companyId IS NULL OR p.companyId = :companyId)
            AND (:jobTypeId IS NULL OR p.jobTypeId = :jobTypeId)
            AND (:statusId IS NULL OR p.statusId = :statusId)
            AND (:archived IS NULL OR p.archived = :archived)
            AND (:appliedFrom IS NULL OR p.appliedAt >= :appliedFrom)
            AND (:appliedTo IS NULL OR p.appliedAt <= :appliedTo)
            AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR EXISTS (SELECT c FROM Company c WHERE c.id = p.companyId
                            AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))))
            """)
    Page<Position> search(@Param("userId") Long userId, @Param("companyId") Long companyId,
            @Param("jobTypeId") Long jobTypeId, @Param("statusId") Long statusId,
            @Param("archived") Boolean archived, @Param("appliedFrom") java.time.LocalDate appliedFrom,
            @Param("appliedTo") java.time.LocalDate appliedTo, @Param("keyword") String keyword, Pageable pageable);
}
