package com.interviewrecord.interviews.infrastructure;

import com.interviewrecord.interviews.domain.InterviewQuestion;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findAllByUserId(Long userId);

    List<InterviewQuestion> findAllByUserIdAndRoundIdOrderBySortOrderAsc(Long userId, Long roundId);

    long countByUserIdAndRoundId(Long userId, Long roundId);

    long countByUserId(Long userId);

    /** 题库检索：仅限当前用户，按分类/关键词过滤，分页返回。 */
    @Query("""
            SELECT q FROM InterviewQuestion q WHERE q.userId = :userId
            AND (:category IS NULL OR q.category = :category)
            AND (:keyword IS NULL OR LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<InterviewQuestion> searchQuestions(@Param("userId") Long userId,
            @Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("DELETE FROM InterviewQuestion q WHERE q.userId = :userId AND q.roundId = :roundId")
    void deleteByUserIdAndRoundId(@Param("userId") Long userId, @Param("roundId") Long roundId);

    @Query("SELECT DISTINCT q.category FROM InterviewQuestion q WHERE q.userId = :userId"
            + " AND q.category IS NOT NULL AND q.category <> '' ORDER BY q.category ASC")
    List<String> findCategoriesByUserId(@Param("userId") Long userId);
}
