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
package com.openllmorchestrator.worker.contract;

/**
 * Optional interface for plugins to declare which contract version they were built for.
 * The worker (or plugin loader) can call {@link ContractVersion#isCompatible(CapabilityHandler)}
 * or {@link ContractVersion#requireCompatible(CapabilityHandler)} before invoking the plugin.
 * <p>
 * Return a semantic version string (e.g. "0.0.1") that matches the plugin-contract dependency
 * version you compiled against.
 */
public interface ContractCompatibility {

    /**
     * The contract version this plugin was built for (e.g. "0.0.1").
     * Should match the plugin-contract dependency version used at compile time.
     */
    String getRequiredContractVersion();
}
