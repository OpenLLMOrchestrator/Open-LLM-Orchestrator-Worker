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

import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.contract.FeatureExecutionContext;
import com.openllmorchestrator.worker.contract.FeatureExecutionPlugin;
import com.openllmorchestrator.worker.engine.config.FeatureFlag;
import com.openllmorchestrator.worker.engine.kernel.interceptor.CapabilityContext;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a {@link FeatureHandler} with a list of {@link FeatureExecutionPlugin}s.
 * At each capability: runs all plugins' beforeFeature (pre), then the handler, then all plugins' afterFeature (post).
 * All plugins receive the same {@link FeatureExecutionContext} with context data.
 */
@Slf4j
public final class FeatureHandlerWithPlugins implements FeatureHandler {

    private final FeatureHandler delegate;
    private final List<FeatureExecutionPlugin> plugins;

    public FeatureHandlerWithPlugins(FeatureHandler delegate, List<FeatureExecutionPlugin> plugins) {
        this.delegate = delegate;
        this.plugins = plugins != null && !plugins.isEmpty()
                ? List.copyOf(plugins)
                : Collections.emptyList();
    }

    @Override
    public FeatureFlag getFeature() {
        return delegate.getFeature();
    }

    @Override
    public void beforeCapability(CapabilityContext ctx) {
        if (!plugins.isEmpty()) {
            FeatureExecutionContext featureCtx = new FeatureExecutionContextImpl(getFeature().name(), ctx);
            for (FeatureExecutionPlugin plugin : plugins) {
                try {
                    plugin.beforeFeature(featureCtx);
                } catch (Exception e) {
                    log.warn("FeatureExecutionPlugin {} beforeFeature failed: {}", plugin.name(), e.getMessage(), e);
                }
            }
        }
        delegate.beforeCapability(ctx);
    }

    @Override
    public void afterCapability(CapabilityContext ctx, CapabilityResult result) {
        delegate.afterCapability(ctx, result);
        if (!plugins.isEmpty()) {
            FeatureExecutionContext featureCtx = new FeatureExecutionContextImpl(getFeature().name(), ctx);
            for (FeatureExecutionPlugin plugin : plugins) {
                try {
                    plugin.afterFeature(featureCtx);
                } catch (Exception e) {
                    log.warn("FeatureExecutionPlugin {} afterFeature failed: {}", plugin.name(), e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void onError(CapabilityContext ctx, Exception e) {
        delegate.onError(ctx, e);
    }
}
