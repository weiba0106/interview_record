package com.interviewrecord.defaults.infrastructure;

import com.interviewrecord.defaults.domain.DefaultPositionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaPositionStatusRepository extends JpaRepository<DefaultPositionStatus, Long> {
    @Query("select status.name from DefaultPositionStatus status where status.user.id = :userId order by status.sortOrder")
    List<String> findNamesByUserIdOrderBySortOrder(long userId);
}
