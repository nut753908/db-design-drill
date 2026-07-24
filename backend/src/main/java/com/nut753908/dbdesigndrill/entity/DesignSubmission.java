package com.nut753908.dbdesigndrill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** ユーザーが提出したDDL(テーブル設計)とそのAIレビュー結果 */
@Entity
@Table(name = "design_submissions")
public class DesignSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddlText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reviewComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected DesignSubmission() {
    }

    public DesignSubmission(Problem problem, String ddlText, String reviewComment, String modelAnswer) {
        this.problem = problem;
        this.ddlText = ddlText;
        this.reviewComment = reviewComment;
        this.modelAnswer = modelAnswer;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Problem getProblem() {
        return problem;
    }

    public String getDdlText() {
        return ddlText;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getModelAnswer() {
        return modelAnswer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
