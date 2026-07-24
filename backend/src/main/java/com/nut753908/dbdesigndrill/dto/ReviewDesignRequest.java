package com.nut753908.dbdesigndrill.dto;

/** Lambda: action=review_design へのリクエスト */
public record ReviewDesignRequest(String action, String requirementText, String ddlText) {

    public ReviewDesignRequest(String requirementText, String ddlText) {
        this("review_design", requirementText, ddlText);
    }
}
