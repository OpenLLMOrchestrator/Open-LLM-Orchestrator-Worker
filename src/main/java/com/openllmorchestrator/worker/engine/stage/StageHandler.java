package com.openllmorchestrator.worker.engine.stage;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;

/**
 * Pluggable stage contract. Implementations are registered by name at bootstrap;
 * no reflection — direct map lookup and invocation for performance.
 * <p>
 * Every plugin receives: {@code context.getOriginalInput()} (read-only), {@code context.getAccumulatedOutput()}
 * (output from previous stages), and writes its output via {@code context.putOutput(key, value)} or
 * {@code context.getCurrentPluginOutput().put(...)}. After each SYNC stage the current output is merged
 * into accumulated; for ASYNC groups the merge policy (FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY) applies.
 * <p>
 * <b>No retained execution state:</b> Do not retain {@code context} after execute() returns.
 */
public interface StageHandler {

    /**
     * Unique stage name (must match pipeline config). Return a constant.
     */
    String name();

    /**
     * Execute the stage. Read from originalInput/accumulatedOutput; write to putOutput/currentPluginOutput.
     * Do not retain context after this method returns.
     */
    StageResult execute(ExecutionContext context);
}
