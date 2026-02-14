package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for gemma2:2b. Used in query-all-models ASYNC pipeline. */
public final class Gemma2_2bChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "Gemma2_2bChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "gemma2:2b"; }
    @Override protected String getModelLabel() { return "gemma2:2b"; }
}
