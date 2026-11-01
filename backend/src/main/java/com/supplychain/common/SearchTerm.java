package com.supplychain.common;

import java.util.Locale;

/** Builds LIKE patterns for queries declared with: LIKE :q ESCAPE '!' */
public final class SearchTerm {

    private SearchTerm() {}

    /** Returns null for a blank search, which the queries read as "no filter". */
    public static String contains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
