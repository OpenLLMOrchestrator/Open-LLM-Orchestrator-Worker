package com.openllmorchestrator.worker.engine.stage;

import com.openllmorchestrator.worker.engine.kernel.merge.AsyncOutputMergePolicy;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One group in the plan: definitions + optional async completion policy and output merge policy. Immutable. */
@Getter
public class StageGroupSpec {
    private final List<StageDefinition> definitions;
    private final AsyncCompletionPolicy asyncPolicy;
    /** For ASYNC groups: how to merge multiple plugin outputs (FIRST_WINS, LAST_WINS, PREFIX_BY_ACTIVITY). */
    private final AsyncOutputMergePolicy asyncOutputMergePolicy;

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy) {
        this(definitions, asyncPolicy, AsyncOutputMergePolicy.LAST_WINS);
    }

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          AsyncOutputMergePolicy asyncOutputMergePolicy) {
        this.definitions = Collections.unmodifiableList(new ArrayList<>(definitions));
        this.asyncPolicy = asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL;
        this.asyncOutputMergePolicy = asyncOutputMergePolicy != null ? asyncOutputMergePolicy : AsyncOutputMergePolicy.LAST_WINS;
    }
}
