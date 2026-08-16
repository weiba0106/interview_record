package com.interviewrecord.tracking.infrastructure;

import com.interviewrecord.tracking.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByIdAndUserId(Long id, Long userId);

    List<Company> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Company> findAllByUserIdOrderByNameAsc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    Optional<Company> findFirstByUserIdAndNameIgnoreCase(Long userId, String name);
}
