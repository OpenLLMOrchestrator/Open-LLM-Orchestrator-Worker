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

import com.openllmorchestrator.worker.engine.config.FeatureFlag;

/**
 * Default factory: returns null for all features so the registry is empty until concrete handlers are registered.
 * Replace or extend with real implementations per feature (e.g. STAGE_RESULT_ENVELOPE → envelope builder handler).
 */
public final class DefaultFeatureHandlerFactory implements FeatureHandlerFactory {

    private static final DefaultFeatureHandlerFactory INSTANCE = new DefaultFeatureHandlerFactory();

    public static DefaultFeatureHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public FeatureHandler createHandler(FeatureFlag feature) {
        return null;
    }
}
