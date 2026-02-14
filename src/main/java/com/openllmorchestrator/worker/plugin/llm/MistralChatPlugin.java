package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for mistral. Used in query-all-models ASYNC pipeline. */
public final class MistralChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "MistralChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "mistral:latest"; }
    @Override protected String getModelLabel() { return "mistral"; }
}
