package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for qwen2:1.5b. Used in query-all-models ASYNC pipeline. */
public final class Qwen2_1_5bChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "Qwen2_1_5bChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "qwen2:1.5b"; }
    @Override protected String getModelLabel() { return "qwen2:1.5b"; }
}
