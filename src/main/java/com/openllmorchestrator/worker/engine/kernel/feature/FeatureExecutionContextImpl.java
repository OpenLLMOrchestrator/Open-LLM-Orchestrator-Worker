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
package com.openllmorchestrator.worker.engine.kernel.feature;

import com.openllmorchestrator.worker.contract.FeatureExecutionContext;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of {@link FeatureExecutionContext} built from {@link CapabilityContext} and feature name.
 * Uses optional {@link ExecutionContext} on capability context for accumulated output and plugin output.
 */
public final class FeatureExecutionContextImpl implements FeatureExecutionContext {

    private final String featureName;
    private final CapabilityContext capabilityContext;

    public FeatureExecutionContextImpl(String featureName, CapabilityContext capabilityContext) {
        this.featureName = featureName != null ? featureName : "";
        this.capabilityContext = capabilityContext;
    }

    @Override
    public String getFeatureName() {
        return featureName;
    }

    @Override
    public long getStepId() {
        return capabilityContext != null ? capabilityContext.getStepId() : 0L;
    }

    @Override
    public String getExecutionId() {
        return capabilityContext != null ? capabilityContext.getExecutionId() : "";
    }

    @Override
    public int getGroupIndex() {
        return capabilityContext != null ? capabilityContext.getGroupIndex() : 0;
    }

    @Override
    public String getCapabilityName() {
        if (capabilityContext == null || capabilityContext.getCapabilityDefinition() == null) {
            return "";
        }
        return capabilityContext.getCapabilityDefinition().getName();
    }

    @Override
    public Map<String, Object> getStateBefore() {
        return capabilityContext != null ? capabilityContext.getStateBefore() : Map.of();
    }

    @Override
    public String getPipelineName() {
        return capabilityContext != null ? capabilityContext.getPipelineName() : "";
    }

    @Override
    public Map<String, Object> getOriginalInput() {
        ExecutionContext ec = capabilityContext != null ? capabilityContext.getExecutionContext() : null;
        return ec != null && ec.getOriginalInput() != null ? ec.getOriginalInput() : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getAccumulatedOutput() {
        ExecutionContext ec = capabilityContext != null ? capabilityContext.getExecutionContext() : null;
        return ec != null && ec.getAccumulatedOutput() != null ? ec.getAccumulatedOutput() : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getCurrentPluginOutput() {
        ExecutionContext ec = capabilityContext != null ? capabilityContext.getExecutionContext() : null;
        return ec != null && ec.getCurrentPluginOutput() != null ? ec.getCurrentPluginOutput() : Collections.emptyMap();
    }

    @Override
    public void putOutput(String key, Object value) {
        ExecutionContext ec = capabilityContext != null ? capabilityContext.getExecutionContext() : null;
        if (ec != null) {
            ec.putOutput(key, value);
        }
    }
}
