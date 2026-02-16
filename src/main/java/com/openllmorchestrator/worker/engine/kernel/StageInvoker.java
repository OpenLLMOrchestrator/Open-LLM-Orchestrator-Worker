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
import com.openllmorchestrator.worker.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageRetryOptions;
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

/** Passes pipeline data context to activities; no hardcoded timeouts/retries. */
public class StageInvoker {

    public Promise<StageResult> invokeAsync(StageDefinition definition, ExecutionContext context) {
        String activityType = activityTypeFor(definition);
        ActivityStub stub = Workflow.newUntypedActivityStub(toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return stub.executeAsync(activityType, StageResult.class, definition.getName(), orig, acc);
    }

    public StageResult invokeSync(StageDefinition definition, ExecutionContext context) {
        String activityType = activityTypeFor(definition);
        ActivityStub stub = Workflow.newUntypedActivityStub(toActivityOptions(definition));
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return stub.execute(activityType, StageResult.class, definition.getName(), orig, acc);
    }

    /**
     * Invoke the merge policy activity (plugin) before exiting an ASYNC group. Returns the merged map.
     */
    public Map<String, Object> invokeMerge(String mergePolicyName, String taskQueue, Duration timeout,
                                           ExecutionContext context, List<String> names, List<StageResult> results) {
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
        Map<String, Object> orig = context != null ? context.getOriginalInput() : Map.of();
        Map<String, Object> acc = context != null ? context.getAccumulatedOutput() : Map.of();
        return activity.merge(mergePolicyName, orig, acc, entries);
    }

    private static ActivityOptions toActivityOptions(StageDefinition d) {
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

    /** Activity type for Temporal UI: "Stage::PluginName" or "PluginName"; fallback "Execute" for typed handler. */
    private static String activityTypeFor(StageDefinition d) {
        String summary = buildActivitySummary(d);
        return summary != null && !summary.isEmpty() ? summary : "Execute";
    }

    /** Builds "Stage::PluginName" or "PluginName" for Temporal UI. */
    private static String buildActivitySummary(StageDefinition d) {
        String pluginName = d.getName();
        if (pluginName == null || pluginName.isBlank()) return null;
        String shortName = pluginName.contains(".") ? pluginName.substring(pluginName.lastIndexOf('.') + 1) : pluginName;
        String bucket = d.getStageBucketName();
        if (bucket != null && !bucket.isBlank()) {
            return bucket + "::" + shortName;
        }
        return shortName;
    }

    /** Calls setSummary on the builder when supported by the SDK (e.g. Temporal 1.26+). */
    private static void setActivitySummaryIfSupported(ActivityOptions.Builder b, StageDefinition d) {
        setSummaryOnBuilder(b, buildActivitySummary(d));
    }

    private static void setSummaryOnBuilder(ActivityOptions.Builder b, String summary) {
        if (summary == null || summary.isEmpty()) return;
        if (summary.length() > 200) summary = summary.substring(0, 200);
        try {
            Method setSummary = b.getClass().getMethod("setSummary", String.class);
            setSummary.invoke(b, summary);
        } catch (Exception ignored) {
            // SDK does not support setSummary (e.g. 1.24.x)
        }
    }

    private static RetryOptions toRetryOptions(StageRetryOptions r) {
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

