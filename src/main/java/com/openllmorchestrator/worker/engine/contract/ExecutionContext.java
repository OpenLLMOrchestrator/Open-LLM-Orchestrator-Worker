package com.openllmorchestrator.worker.engine.contract;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ExecutionContext {

    private final ExecutionCommand command;
    private final Map<String, Object> state = new HashMap<>();

    public ExecutionContext(ExecutionCommand command) {
        this.command = command;
    }

    public static ExecutionContext from(ExecutionCommand command) {
        return new ExecutionContext(command);
    }

    public void put(String key, Object value) {
        state.put(key, value);
    }

    public Object get(String key) {
        return state.get(key);
    }
}
