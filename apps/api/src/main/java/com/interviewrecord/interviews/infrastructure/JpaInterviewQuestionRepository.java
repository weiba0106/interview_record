package com.interviewrecord.interviews.infrastructure;

import com.interviewrecord.interviews.domain.InterviewQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findAllByUserId(Long userId);

    List<InterviewQuestion> findAllByUserIdAndRoundIdOrderBySortOrderAsc(Long userId, Long roundId);

    long countByUserIdAndRoundId(Long userId, Long roundId);

    @Modifying
    @Query("DELETE FROM InterviewQuestion q WHERE q.userId = :userId AND q.roundId = :roundId")
    void deleteByUserIdAndRoundId(@Param("userId") Long userId, @Param("roundId") Long roundId);
}
