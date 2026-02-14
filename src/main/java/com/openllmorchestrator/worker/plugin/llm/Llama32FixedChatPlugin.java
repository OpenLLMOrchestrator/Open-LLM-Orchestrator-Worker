package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for llama3.2. Used in query-all-models ASYNC pipeline. */
public final class Llama32FixedChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "Llama32FixedChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "llama3.2:latest"; }
    @Override protected String getModelLabel() { return "llama3.2"; }
}
