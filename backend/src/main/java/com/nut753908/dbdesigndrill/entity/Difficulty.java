package com.nut753908.dbdesigndrill.entity;

/** お題の難易度 */
public enum Difficulty {
    BEGINNER("初級"),
    INTERMEDIATE("中級"),
    ADVANCED("上級");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
