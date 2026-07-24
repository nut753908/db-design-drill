package com.nut753908.dbdesigndrill.dto;

/** Lambda: action=review_implementation へのリクエスト */
public record ReviewImplementationRequest(
        String action, String requirementText, String ddlText, String codeText) {

    public ReviewImplementationRequest(String requirementText, String ddlText, String codeText) {
        this("review_implementation", requirementText, ddlText, codeText);
    }
}
