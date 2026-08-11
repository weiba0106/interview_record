package com.interviewrecord.defaults.infrastructure;

import com.interviewrecord.defaults.domain.DefaultJobType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaJobTypeRepository extends JpaRepository<DefaultJobType, Long> {
    @Query("select type.name from DefaultJobType type where type.user.id = :userId order by type.id")
    List<String> findNamesByUserId(long userId);
}
