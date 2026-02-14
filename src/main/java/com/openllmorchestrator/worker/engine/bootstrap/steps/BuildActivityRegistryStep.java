package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.stage.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.stage.StageHandler;
import com.openllmorchestrator.worker.engine.stage.handler.merge.FirstWinsMergeHandler;
import com.openllmorchestrator.worker.engine.stage.handler.merge.LastWinsMergeHandler;
import com.openllmorchestrator.worker.engine.stage.handler.merge.PrefixByActivityMergeHandler;
import com.openllmorchestrator.worker.plugin.folder.FolderIngestionPlugin;
import com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin;
import com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin;
import com.openllmorchestrator.worker.plugin.tokenizer.DocumentTokenizerPlugin;

/** Registers plugin handlers by FQCN so pipeline config can reference them by class name. */
public final class BuildActivityRegistryStep implements BootstrapStep {

    public static final String FQCN_VECTOR_STORE = "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin";
    public static final String FQCN_LLM = "com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin";
    public static final String FQCN_TOKENIZER = "com.openllmorchestrator.worker.plugin.tokenizer.DocumentTokenizerPlugin";
    public static final String FQCN_FOLDER_INGESTION = "com.openllmorchestrator.worker.plugin.folder.FolderIngestionPlugin";

    @Override
    public void run(BootstrapContext ctx) {
        StageHandler vectorStore = new VectorStoreRetrievalPlugin();
        StageHandler llm = new Llama32ModelPlugin();
        StageHandler tokenizer = new DocumentTokenizerPlugin();
        StageHandler folderIngestion = new FolderIngestionPlugin();
        StageHandler lastWins = new LastWinsMergeHandler();
        StageHandler firstWins = new FirstWinsMergeHandler();
        StageHandler prefixByActivity = new PrefixByActivityMergeHandler();

        ActivityRegistry registry = ActivityRegistry.builder()
                .register(FQCN_VECTOR_STORE, vectorStore)
                .register(FQCN_LLM, llm)
                .register(FQCN_TOKENIZER, tokenizer)
                .register(FQCN_FOLDER_INGESTION, folderIngestion)
                .register(LastWinsMergeHandler.NAME, lastWins)
                .register(FirstWinsMergeHandler.NAME, firstWins)
                .register(PrefixByActivityMergeHandler.NAME, prefixByActivity)
                .build();

        ctx.setActivityRegistry(registry);
    }
}
