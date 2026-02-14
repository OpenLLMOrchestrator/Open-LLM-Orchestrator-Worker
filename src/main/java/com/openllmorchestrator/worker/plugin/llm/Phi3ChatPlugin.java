package com.openllmorchestrator.worker.plugin.llm;

/** Fixed-model chat plugin for phi3. Used in query-all-models ASYNC pipeline. */
public final class Phi3ChatPlugin extends FixedModelChatPlugin {
    public static final String NAME = "Phi3ChatPlugin";
    @Override public String name() { return NAME; }
    @Override protected String getModelId() { return "phi3:latest"; }
    @Override protected String getModelLabel() { return "phi3"; }
}
