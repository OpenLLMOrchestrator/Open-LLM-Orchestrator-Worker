package com.openllmorchestrator.worker.engine.stage.predefined;

import java.util.Set;

/** Predefined pipeline stage names. Others are custom and use custom bucket. */
public final class PredefinedStages {
    public static final String ACCESS = "ACCESS";
    public static final String MEMORY = "MEMORY";
    public static final String RETRIEVAL = "RETRIEVAL";
    public static final String MODEL = "MODEL";
    public static final String MCP = "MCP";
    public static final String TOOL = "TOOL";
    public static final String FILTER = "FILTER";
    public static final String POST_PROCESS = "POST_PROCESS";
    public static final String OBSERVABILITY = "OBSERVABILITY";
    public static final String CUSTOM = "CUSTOM";

    private static final Set<String> NAMES = Set.of(
            ACCESS, MEMORY, RETRIEVAL, MODEL, MCP, TOOL, FILTER, POST_PROCESS, OBSERVABILITY, CUSTOM);

    private PredefinedStages() {}

    public static boolean isPredefined(String stageName) {
        return stageName != null && NAMES.contains(stageName);
    }

    public static Set<String> all() {
        return NAMES;
    }
}
