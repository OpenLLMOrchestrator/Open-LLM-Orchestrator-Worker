/*
 * Copyright 2026 Open LLM Orchestrator contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openllmorchestrator.worker.engine.bootstrap.steps;

import com.openllmorchestrator.worker.contract.CapabilityHandler;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapContext;
import com.openllmorchestrator.worker.engine.bootstrap.BootstrapStep;
import com.openllmorchestrator.worker.engine.capability.activity.ActivityRegistry;
import com.openllmorchestrator.worker.engine.capability.handler.merge.AllModelsResponseFormatMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.FirstWinsMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.LastWinsMergeHandler;
import com.openllmorchestrator.worker.engine.capability.handler.merge.PrefixByActivityMergeHandler;
import com.openllmorchestrator.worker.plugin.folder.FolderIngestionPlugin;
import com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin;
import com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin;
import com.openllmorchestrator.worker.plugin.llm.Llama32ChatPlugin;
import com.openllmorchestrator.worker.plugin.llm.MistralChatPlugin;
import com.openllmorchestrator.worker.plugin.llm.Llama32FixedChatPlugin;
import com.openllmorchestrator.worker.plugin.llm.Phi3ChatPlugin;
import com.openllmorchestrator.worker.plugin.llm.Gemma2_2bChatPlugin;
import com.openllmorchestrator.worker.plugin.llm.Qwen2_1_5bChatPlugin;
import com.openllmorchestrator.worker.plugin.tokenizer.DocumentTokenizerPlugin;
import com.openllmorchestrator.worker.plugin.output.AnswerFormatPlugin;
import com.openllmorchestrator.worker.plugin.access.AllowAllAccessControlPlugin;
import com.openllmorchestrator.worker.plugin.tool.EchoToolPlugin;
import com.openllmorchestrator.worker.plugin.memory.ContextMemoryPlugin;
import com.openllmorchestrator.worker.plugin.guardrail.SimpleGuardrailPlugin;
import com.openllmorchestrator.worker.plugin.prompt.SimplePromptBuilderPlugin;
import com.openllmorchestrator.worker.plugin.caching.InMemoryCachingPlugin;
import com.openllmorchestrator.worker.plugin.observability.PassThroughObservabilityPlugin;
import com.openllmorchestrator.worker.sample.SampleEchoPlugin;
import com.openllmorchestrator.worker.sample.StubConditionPlugin;
import com.openllmorchestrator.worker.sample.StubDatasetBuildPlugin;
import com.openllmorchestrator.worker.sample.StubEvaluationPlugin;
import com.openllmorchestrator.worker.sample.StubFeedbackPlugin;
import com.openllmorchestrator.worker.sample.StubFilterPlugin;
import com.openllmorchestrator.worker.sample.StubLearningPlugin;
import com.openllmorchestrator.worker.sample.StubModelPlugin;
import com.openllmorchestrator.worker.sample.StubModelRegistryPlugin;
import com.openllmorchestrator.worker.sample.StubRefinementPlugin;
import com.openllmorchestrator.worker.sample.StubRetrievalPlugin;
import com.openllmorchestrator.worker.sample.StubTrainTriggerPlugin;

/** Registers plugin handlers by FQCN so pipeline config can reference them by class name. */
public final class BuildActivityRegistryStep implements BootstrapStep {

    public static final String FQCN_VECTOR_STORE = "com.openllmorchestrator.worker.plugin.vectordb.VectorStoreRetrievalPlugin";
    public static final String FQCN_LLM = "com.openllmorchestrator.worker.plugin.llm.Llama32ModelPlugin";
    /** Non-RAG chat: messages only, no retrieval. Use for llama-oss, openai-oss, both pipelines. */
    public static final String FQCN_LLM_CHAT = "com.openllmorchestrator.worker.plugin.llm.Llama32ChatPlugin";
    public static final String FQCN_TOKENIZER = "com.openllmorchestrator.worker.plugin.tokenizer.DocumentTokenizerPlugin";
    public static final String FQCN_FOLDER_INGESTION = "com.openllmorchestrator.worker.plugin.folder.FolderIngestionPlugin";
    public static final String FQCN_ANSWER_FORMAT = "com.openllmorchestrator.worker.plugin.output.AnswerFormatPlugin";

    @Override
    public void run(BootstrapContext ctx) {
        CapabilityHandler vectorStore = new VectorStoreRetrievalPlugin();
        CapabilityHandler llm = new Llama32ModelPlugin();
        CapabilityHandler llmChat = new Llama32ChatPlugin();
        CapabilityHandler tokenizer = new DocumentTokenizerPlugin();
        CapabilityHandler folderIngestion = new FolderIngestionPlugin();
        CapabilityHandler answerFormat = new AnswerFormatPlugin();
        CapabilityHandler allowAllAccess = new AllowAllAccessControlPlugin();
        CapabilityHandler echoTool = new EchoToolPlugin();
        CapabilityHandler contextMemory = new ContextMemoryPlugin();
        CapabilityHandler simpleGuardrail = new SimpleGuardrailPlugin();
        CapabilityHandler simplePromptBuilder = new SimplePromptBuilderPlugin();
        CapabilityHandler inMemoryCaching = new InMemoryCachingPlugin();
        CapabilityHandler passThroughObservability = new PassThroughObservabilityPlugin();
        CapabilityHandler sampleEcho = new SampleEchoPlugin();
        CapabilityHandler stubFilter = new StubFilterPlugin();
        CapabilityHandler stubRetrieval = new StubRetrievalPlugin();
        CapabilityHandler stubModel = new StubModelPlugin();
        CapabilityHandler stubRefinement = new StubRefinementPlugin();
        CapabilityHandler stubEvaluation = new StubEvaluationPlugin();
        CapabilityHandler stubFeedback = new StubFeedbackPlugin();
        CapabilityHandler stubLearning = new StubLearningPlugin();
        CapabilityHandler stubDatasetBuild = new StubDatasetBuildPlugin();
        CapabilityHandler stubTrainTrigger = new StubTrainTriggerPlugin();
        CapabilityHandler stubModelRegistry = new StubModelRegistryPlugin();
        CapabilityHandler stubCondition = new StubConditionPlugin();
        CapabilityHandler lastWins = new LastWinsMergeHandler();
        CapabilityHandler firstWins = new FirstWinsMergeHandler();
        CapabilityHandler prefixByActivity = new PrefixByActivityMergeHandler();
        CapabilityHandler allModelsFormat = new AllModelsResponseFormatMergeHandler();

        ActivityRegistry registry = ActivityRegistry.builder()
                .register(FQCN_VECTOR_STORE, vectorStore)
                .register(FQCN_LLM, llm)
                .register(FQCN_LLM_CHAT, llmChat)
                .register(FQCN_TOKENIZER, tokenizer)
                .register(FQCN_FOLDER_INGESTION, folderIngestion)
                .register(FQCN_ANSWER_FORMAT, answerFormat)
                .register(AllowAllAccessControlPlugin.NAME, allowAllAccess)
                .register(EchoToolPlugin.NAME, echoTool)
                .register(ContextMemoryPlugin.NAME, contextMemory)
                .register(SimpleGuardrailPlugin.NAME, simpleGuardrail)
                .register(SimplePromptBuilderPlugin.NAME, simplePromptBuilder)
                .register(InMemoryCachingPlugin.NAME, inMemoryCaching)
                .register(PassThroughObservabilityPlugin.NAME, passThroughObservability)
                .register(SampleEchoPlugin.NAME, sampleEcho)
                .register(StubFilterPlugin.NAME, stubFilter)
                .register(StubRetrievalPlugin.NAME, stubRetrieval)
                .register(StubModelPlugin.NAME, stubModel)
                .register(StubRefinementPlugin.NAME, stubRefinement)
                .register(StubEvaluationPlugin.NAME, stubEvaluation)
                .register(StubFeedbackPlugin.NAME, stubFeedback)
                .register(StubLearningPlugin.NAME, stubLearning)
                .register(StubDatasetBuildPlugin.NAME, stubDatasetBuild)
                .register(StubTrainTriggerPlugin.NAME, stubTrainTrigger)
                .register(StubModelRegistryPlugin.NAME, stubModelRegistry)
                .register(StubConditionPlugin.NAME, stubCondition)
                .register(LastWinsMergeHandler.NAME, lastWins)
                .register(FirstWinsMergeHandler.NAME, firstWins)
                .register(PrefixByActivityMergeHandler.NAME, prefixByActivity)
                .register(AllModelsResponseFormatMergeHandler.NAME, allModelsFormat)
                .register("com.openllmorchestrator.worker.plugin.llm.MistralChatPlugin", new MistralChatPlugin())
                .register("com.openllmorchestrator.worker.plugin.llm.Llama32FixedChatPlugin", new Llama32FixedChatPlugin())
                .register("com.openllmorchestrator.worker.plugin.llm.Phi3ChatPlugin", new Phi3ChatPlugin())
                .register("com.openllmorchestrator.worker.plugin.llm.Gemma2_2bChatPlugin", new Gemma2_2bChatPlugin())
                .register("com.openllmorchestrator.worker.plugin.llm.Qwen2_1_5bChatPlugin", new Qwen2_1_5bChatPlugin())
                .build();

        ctx.setActivityRegistry(registry);
    }
}

