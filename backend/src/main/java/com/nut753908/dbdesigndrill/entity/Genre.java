package com.nut753908.dbdesigndrill.entity;

/** お題のジャンル(固定リスト) */
public enum Genre {
    EC("ECサイト"),
    RESERVATION("予約システム"),
    INVENTORY("在庫管理"),
    LIBRARY("図書館システム"),
    BLOG("ブログ・CMS");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
