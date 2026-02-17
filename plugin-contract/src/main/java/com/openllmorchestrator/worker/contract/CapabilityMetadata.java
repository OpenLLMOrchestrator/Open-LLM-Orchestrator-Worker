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

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CapabilityMetadata {
    @JsonAlias("stageName")
    private final String capabilityName;
    private final long stepId;
    private final String executionId;
    @JsonAlias("stageBucketName")
    private final String capabilityBucketName;

    /** @deprecated Use {@link #getCapabilityName()}. */
    @Deprecated
    public String getStageName() { return capabilityName; }
    /** @deprecated Use {@link #getCapabilityBucketName()}. */
    @Deprecated
    public String getStageBucketName() { return capabilityBucketName; }
}
