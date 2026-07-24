package com.nut753908.dbdesigndrill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 生成AIが作成したお題(要件文) */
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirementText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Problem() {
    }

    public Problem(Genre genre, Difficulty difficulty, String requirementText) {
        this.genre = genre;
        this.difficulty = difficulty;
        this.requirementText = requirementText;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Genre getGenre() {
        return genre;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
