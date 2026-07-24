package com.nut753908.dbdesigndrill.dto;

/** Lambda: action=generate_problem へのリクエスト */
public record GenerateProblemRequest(String action, String genre, String difficulty) {

    public GenerateProblemRequest(String genre, String difficulty) {
        this("generate_problem", genre, difficulty);
    }
}
