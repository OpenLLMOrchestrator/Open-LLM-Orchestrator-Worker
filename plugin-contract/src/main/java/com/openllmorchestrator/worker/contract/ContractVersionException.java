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
 * Thrown when a plugin declares a required contract version that is not compatible
 * with the runtime contract version (see {@link ContractVersion#requireCompatible(StageHandler)}).
 */
public class ContractVersionException extends IllegalStateException {

    public ContractVersionException(String message) {
        super(message);
    }

    public ContractVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
