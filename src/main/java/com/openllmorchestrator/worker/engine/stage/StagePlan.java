/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
