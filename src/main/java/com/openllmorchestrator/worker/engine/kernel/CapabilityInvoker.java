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
package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.activity.MergePolicyActivity;
import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;
import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.contract.CapabilityResult;
import com.openllmorchestrator.worker.engine.capability.CapabilityDefinition;
import com.openllmorchestrator.worker.engine.capability.CapabilityRetryOptions;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Passes pipeline data context to capability activities; no hardcoded timeouts/retries. */
public class CapabilityInvoker {

    public Promise<CapabilityResult> invokeAsync(CapabilityDefinition definition, ExecutionContext context) {
        String activityType = activityTypeFor(definition);
        ActivityStub stub = Workflow.newUntypedActivityStub(toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        String queueName = context != null ? context.getQueueName() : null;
        return stub.executeAsync(activityType, CapabilityResult.class, queueName, definition.getName(), orig, acc);
    }

    public CapabilityResult invokeSync(CapabilityDefinition definition, ExecutionContext context) {
        String activityType = activityTypeFor(definition);
        ActivityStub stub = Workflow.newUntypedActivityStub(toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        String queueName = context != null ? context.getQueueName() : null;
        return stub.execute(activityType, CapabilityResult.class, queueName, definition.getName(), orig, acc);
    }

    public Map<String, Object> invokeMerge(String mergePolicyName, String taskQueue, Duration timeout,
                                           ExecutionContext context, List<String> names, List<CapabilityResult> results) {
        ActivityOptions.Builder mergeOptions = ActivityOptions.newBuilder()
                .setTaskQueue(taskQueue != null ? taskQueue : "default")
                .setStartToCloseTimeout(timeout != null && !timeout.isNegative() ? timeout : Duration.ofSeconds(30));
        setSummaryOnBuilder(mergeOptions, "Merge::" + (mergePolicyName != null && !mergePolicyName.isBlank() ? mergePolicyName : "LAST_WINS"));
        MergePolicyActivity activity = Workflow.newActivityStub(MergePolicyActivity.class, mergeOptions.build());
        List<AsyncGroupResultEntry> entries = new ArrayList<>();
        if (names != null && results != null && names.size() == results.size()) {
            for (int i = 0; i < names.size(); i++) {
                entries.add(new AsyncGroupResultEntry(names.get(i), results.get(i)));
            }
        }
        String queueName = context != null ? context.getQueueName() : null;
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return activity.merge(queueName, mergePolicyName, orig, acc, entries);
    }

    private static ActivityOptions toActivityOptions(CapabilityDefinition d) {
        ActivityOptions.Builder b = ActivityOptions.newBuilder()
                .setTaskQueue(d.getTaskQueue())
                .setStartToCloseTimeout(d.getTimeout());
        if (d.getScheduleToStartTimeout() != null) {
            b.setScheduleToStartTimeout(d.getScheduleToStartTimeout());
        }
        if (d.getScheduleToCloseTimeout() != null) {
            b.setScheduleToCloseTimeout(d.getScheduleToCloseTimeout());
        }
        if (d.getRetryOptions() != null) {
            b.setRetryOptions(toRetryOptions(d.getRetryOptions()));
        }
        setActivitySummaryIfSupported(b, d);
        return b.build();
    }

    private static String activityTypeFor(CapabilityDefinition d) {
        String summary = buildActivitySummary(d);
        if (summary != null && !summary.isEmpty()) return summary;
        String bucket = d.getCapabilityBucketName();
        if (bucket != null && !bucket.isBlank()) return bucket + "::Unknown";
        return "Capability::Unknown";
    }

    private static String buildActivitySummary(CapabilityDefinition d) {
        String pluginName = d.getName();
        if (pluginName == null || pluginName.isBlank()) return null;
        String shortName = pluginName.contains(".") ? pluginName.substring(pluginName.lastIndexOf('.') + 1) : pluginName;
        String bucket = d.getCapabilityBucketName();
        if (bucket != null && !bucket.isBlank()) {
            return bucket + "::" + shortName;
        }
        return shortName;
    }

    private static void setActivitySummaryIfSupported(ActivityOptions.Builder b, CapabilityDefinition d) {
        setSummaryOnBuilder(b, buildActivitySummary(d));
    }

    private static void setSummaryOnBuilder(ActivityOptions.Builder b, String summary) {
        if (summary == null || summary.isEmpty()) return;
        if (summary.length() > 200) summary = summary.substring(0, 200);
        try {
            Method setSummary = b.getClass().getMethod("setSummary", String.class);
            setSummary.invoke(b, summary);
        } catch (Exception ignored) {
        }
    }

    private static RetryOptions toRetryOptions(CapabilityRetryOptions r) {
        RetryOptions.Builder b = RetryOptions.newBuilder()
                .setMaximumAttempts(r.getMaximumAttempts())
                .setInitialInterval(Duration.ofSeconds(r.getInitialIntervalSeconds()))
                .setBackoffCoefficient(r.getBackoffCoefficient())
                .setMaximumInterval(Duration.ofSeconds(r.getMaximumIntervalSeconds()));
        List<String> nonRetryable = r.getNonRetryableErrors();
        if (nonRetryable != null && !nonRetryable.isEmpty()) {
            b.setDoNotRetry(nonRetryable.toArray(new String[0]));
        }
        return b.build();
    }
}
