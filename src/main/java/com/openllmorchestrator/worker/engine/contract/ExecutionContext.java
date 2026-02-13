package com.openllmorchestrator.worker.engine.contract;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution context for one workflow run. Every plugin gets a fair chance to read
 * original input and accumulated output from previous stages, and to write its output
 * into the current-plugin map (which is then merged into accumulated after the stage).
 */
@Getter
public class ExecutionContext {

    private final ExecutionCommand command;
    /** General state; use for backward compatibility. */
    private final Map<String, Object> state = new HashMap<>();

    /** Read-only: initial input for the pipeline (same for all plugins). */
    private final Map<String, Object> originalInput;
    /** Mutable in workflow: output accumulated from previous stages. In activity, read-only snapshot. */
    private final Map<String, Object> accumulatedOutput;
    /** Mutable: current plugin's output (merged into accumulated after SYNC; for ASYNC see merge policy). */
    private final Map<String, Object> currentPluginOutput;

    public ExecutionContext(ExecutionCommand command) {
        this.command = command;
        Map<String, Object> input = command != null && command.getInput() != null ? command.getInput() : null;
        this.originalInput = input != null ? Collections.unmodifiableMap(new HashMap<>(input)) : Collections.emptyMap();
        this.accumulatedOutput = new HashMap<>();
        this.currentPluginOutput = new HashMap<>();
    }

    /** For activity: originalInput and accumulatedOutput are read-only snapshots; currentPluginOutput is mutable. */
    public ExecutionContext(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput) {
        this.command = null;
        this.originalInput = originalInput != null ? Collections.unmodifiableMap(new HashMap<>(originalInput)) : Collections.emptyMap();
        this.accumulatedOutput = accumulatedOutput != null ? Collections.unmodifiableMap(new HashMap<>(accumulatedOutput)) : Collections.emptyMap();
        this.currentPluginOutput = new HashMap<>();
    }

    /** For workflow: from command; originalInput from command.getInput(), accumulatedOutput empty. */
    public static ExecutionContext from(ExecutionCommand command) {
        return new ExecutionContext(command);
    }

    /** For activity: context with given read-only maps and empty currentPluginOutput for plugin to fill. */
    public static ExecutionContext forActivity(Map<String, Object> originalInput, Map<String, Object> accumulatedOutput) {
        return new ExecutionContext(originalInput, accumulatedOutput);
    }

    public void put(String key, Object value) {
        state.put(key, value);
    }

    public Object get(String key) {
        return state.get(key);
    }

    /** Write current plugin output (convenience). */
    public void putOutput(String key, Object value) {
        currentPluginOutput.put(key, value);
    }
}
