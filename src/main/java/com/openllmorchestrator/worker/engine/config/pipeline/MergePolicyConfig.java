package com.openllmorchestrator.worker.engine.config.pipeline;

import lombok.Getter;
import lombok.Setter;

/**
 * Merge policy hook: identifies the merge policy plugin (activity) to run before exiting an ASYNC group.
 * Use in pipeline or group config as: { "type": "MERGE_POLICY", "pluginType": "MergePolicy", "name": "com.example.plugin.RankedMerge" }
 * The "name" is the activity/plugin name (e.g. LAST_WINS, or FQCN) resolved at runtime.
 */
@Getter
@Setter
public class MergePolicyConfig {
    /** Should be "MERGE_POLICY". */
    private String type;
    /** Plugin type, e.g. "MergePolicy". */
    private String pluginType;
    /** Activity/plugin name or FQCN (e.g. "LAST_WINS", "com.example.plugin.RankedMerge"). */
    private String name;
}
