package com.openllmorchestrator.worker.engine.stage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Execution hierarchy: ordered list of stage groups. Built once at bootstrap from config
 * and reused for the container lifecycle. Immutable; holds no transactional or request-scoped data.
 */
@Getter
public class StagePlan {
    private final List<StageGroupSpec> groups;

    StagePlan(List<StageGroupSpec> groups) {
        this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
    }

    public static StagePlanBuilder builder() {
        return new StagePlanBuilder();
    }
}
