package com.openllmorchestrator.worker.engine.stage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One group in the plan: definitions + optional async completion policy and merge policy plugin name. Immutable. */
@Getter
public class StageGroupSpec {
    private final List<StageDefinition> definitions;
    private final AsyncCompletionPolicy asyncPolicy;
    /** For ASYNC groups: merge policy activity name (plugin), invoked before exiting the group. Default LAST_WINS. */
    private final String asyncOutputMergePolicyName;

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy) {
        this(definitions, asyncPolicy, "LAST_WINS");
    }

    public StageGroupSpec(List<StageDefinition> definitions, AsyncCompletionPolicy asyncPolicy,
                          String asyncOutputMergePolicyName) {
        this.definitions = Collections.unmodifiableList(new ArrayList<>(definitions));
        this.asyncPolicy = asyncPolicy != null ? asyncPolicy : AsyncCompletionPolicy.ALL;
        this.asyncOutputMergePolicyName = asyncOutputMergePolicyName != null && !asyncOutputMergePolicyName.isBlank()
                ? asyncOutputMergePolicyName : "LAST_WINS";
    }
}
