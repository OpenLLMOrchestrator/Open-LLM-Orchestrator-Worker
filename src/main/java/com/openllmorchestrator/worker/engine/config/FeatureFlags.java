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
package com.openllmorchestrator.worker.engine.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Set of enabled feature flags. Loaded at bootstrap from config; only enabled features execute.
 */
public final class FeatureFlags implements com.openllmorchestrator.worker.engine.contract.FeatureFlagsProvider {

    private final Set<FeatureFlag> enabled;

    public FeatureFlags(Set<FeatureFlag> enabled) {
        if (enabled == null || enabled.isEmpty()) {
            this.enabled = Collections.emptySet();
        } else {
            this.enabled = Collections.unmodifiableSet(EnumSet.copyOf(enabled));
        }
    }

    public boolean isEnabled(FeatureFlag flag) {
        return flag != null && enabled.contains(flag);
    }

    /** For contract layer: check by name so contract does not depend on config enum. */
    @Override
    public boolean isEnabled(String flagName) {
        if (flagName == null || flagName.isBlank()) return false;
        try {
            return isEnabled(FeatureFlag.valueOf(flagName.trim()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Set<FeatureFlag> getEnabled() {
        return enabled;
    }

    /** Build from config list of string names (e.g. from JSON enabledFeatures). */
    public static FeatureFlags fromNames(Iterable<String> names) {
        if (names == null) return new FeatureFlags(Collections.emptySet());
        Set<FeatureFlag> set = new java.util.LinkedHashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                set.add(FeatureFlag.valueOf(name.trim()));
            } catch (IllegalArgumentException ignored) {
                // skip unknown names
            }
        }
        return new FeatureFlags(set);
    }
}

