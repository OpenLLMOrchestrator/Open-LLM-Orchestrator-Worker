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
package com.openllmorchestrator.worker.engine.security;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Security hardening gate when {@link com.openllmorchestrator.worker.engine.config.FeatureFlag#SECURITY_HARDENING} is enabled.
 * Covers: prompt injection defense, tool allowlist enforcement, output content scanning pre-persist, context poisoning detection.
 */
public interface SecurityHardeningGate {

    /** Run before model stage: detect prompt injection; throw or flag if detected. */
    void checkPromptInjection(ExecutionContext context, String promptKey);

    /** Run before tool stage: enforce tool allowlist for this user/tenant; throw if tool not allowed. */
    void checkToolAllowlist(ExecutionContext context, String toolName);

    /** Run before persisting/returning output: scan content (PII, toxicity, policy); throw or redact if needed. */
    void scanOutputPrePersist(ExecutionContext context, Map<String, Object> output);

    /** Run on context state: detect context poisoning (e.g. injected instructions); throw or flag if detected. */
    void checkContextPoisoning(ExecutionContext context);
}

