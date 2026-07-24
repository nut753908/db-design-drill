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

/** ユーザーが提出したJPA実装コードとそのAIレビュー結果 */
@Entity
@Table(name = "implementation_submissions")
public class ImplementationSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_submission_id", nullable = false)
    private DesignSubmission designSubmission;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codeText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reviewComment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ImplementationSubmission() {
    }

    public ImplementationSubmission(DesignSubmission designSubmission, String codeText, String reviewComment) {
        this.designSubmission = designSubmission;
        this.codeText = codeText;
        this.reviewComment = reviewComment;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public DesignSubmission getDesignSubmission() {
        return designSubmission;
    }

    public String getCodeText() {
        return codeText;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
