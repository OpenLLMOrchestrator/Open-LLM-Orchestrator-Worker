package com.openllmorchestrator.worker.engine.stage;

/** How to complete an async group. From config; no hardcoded behavior. */
public enum AsyncCompletionPolicy {
    ALL,
    FIRST_SUCCESS,
    FIRST_FAILURE,
    ALL_SETTLED;

    public static AsyncCompletionPolicy fromConfig(String value) {
        if (value == null || value.isBlank()) return ALL;
        switch (value.toUpperCase()) {
            case "FIRST_SUCCESS": return FIRST_SUCCESS;
            case "FIRST_FAILURE": return FIRST_FAILURE;
            case "ALL_SETTLED": return ALL_SETTLED;
            default: return ALL;
        }
    }
}
