package com.interviewrecord.sharing.infrastructure;

import com.interviewrecord.sharing.domain.ShareRound;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaShareRoundRepository extends JpaRepository<ShareRound, Long> {
    List<ShareRound> findAllByShareIdOrderByIdAsc(Long shareId);
}
