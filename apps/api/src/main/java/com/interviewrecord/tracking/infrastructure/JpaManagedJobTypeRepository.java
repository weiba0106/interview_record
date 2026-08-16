package com.interviewrecord.tracking.infrastructure;

import com.interviewrecord.tracking.domain.JobType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaManagedJobTypeRepository extends JpaRepository<JobType, Long> {
    Optional<JobType> findByIdAndUserId(Long id, Long userId);

    List<JobType> findAllByUserIdOrderByIdAsc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
