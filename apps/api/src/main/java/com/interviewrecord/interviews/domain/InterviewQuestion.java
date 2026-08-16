package com.interviewrecord.interviews.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "round_id", nullable = false) private Long roundId;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, length = 2000) private String question;
    @Column(length = 4000) private String answer;
    @Column(length = 40) private String category;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected InterviewQuestion() {}

    public InterviewQuestion(Long userId, Long roundId, int sortOrder, String question, String answer,
            String category, Instant now) {
        this.userId = userId; this.roundId = roundId; this.sortOrder = sortOrder;
        this.question = question; this.answer = answer; this.category = category;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long roundId() { return roundId; }
    public int sortOrder() { return sortOrder; }
    public String question() { return question; }
    public String answer() { return answer; }
    public String category() { return category; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
