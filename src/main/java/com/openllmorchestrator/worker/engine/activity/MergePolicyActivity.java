package com.openllmorchestrator.worker.engine.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;
import java.util.Map;

import com.openllmorchestrator.worker.engine.contract.AsyncGroupResultEntry;

/**
 * Activity invoked before exiting an ASYNC group: runs the configured merge policy plugin
 * (by name) and returns the new accumulated output.
 */
@ActivityInterface
public interface MergePolicyActivity {

    /**
     * Run the merge policy plugin with the given context. The plugin is resolved by name
     * (same registry as other stage plugins). Returns the merged map to use as accumulated output.
     */
    @ActivityMethod
    Map<String, Object> merge(String mergePolicyName,
                              Map<String, Object> originalInput,
                              Map<String, Object> accumulatedOutput,
                              List<AsyncGroupResultEntry> asyncResults);
}
