package com.openllmorchestrator.worker;

import com.openllmorchestrator.worker.engine.activity.impl.KernelStageActivityImpl;
import com.openllmorchestrator.worker.engine.activity.impl.MergePolicyActivityImpl;
import com.openllmorchestrator.worker.engine.bootstrap.WorkerBootstrap;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.workflow.impl.CoreWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

public class WorkerApplication {

    public static void main(String[] args) {

        System.out.println("==========================================");
        System.out.println(" Starting Open LLM Orchestrator Worker");
        System.out.println("==========================================");

        try {

            // ----------------------------------------------------
            // 1️⃣  Bootstrap Configuration
            // ----------------------------------------------------
            EngineFileConfig config = WorkerBootstrap.initialize();

            if (config == null) {
                throw new IllegalStateException("Worker bootstrap failed. Config is null.");
            }

            EngineRuntime.CONFIG = config;

            String taskQueue = config.getWorker().getQueueName();

            System.out.println("Using Task Queue: " + taskQueue);

            // ----------------------------------------------------
            // 2️⃣  Connect to Temporal (env overrides config)
            // ----------------------------------------------------
            EnvConfig env = EnvConfig.fromEnvironment();
            String temporalTarget = env.getTemporalTarget() != null && !env.getTemporalTarget().isBlank()
                    ? env.getTemporalTarget()
                    : (config.getTemporal() != null && config.getTemporal().getTarget() != null
                            ? config.getTemporal().getTarget()
                            : "localhost:7233");
            String namespace = env.getTemporalNamespace() != null && !env.getTemporalNamespace().isBlank()
                    ? env.getTemporalNamespace()
                    : (config.getTemporal() != null && config.getTemporal().getNamespace() != null
                            ? config.getTemporal().getNamespace()
                            : "default");
            WorkflowServiceStubsOptions serviceOptions =
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget(temporalTarget)
                            .build();

            WorkflowServiceStubs service =
                    WorkflowServiceStubs.newServiceStubs(serviceOptions);

            WorkflowClient client =
                    WorkflowClient.newInstance(service);

            WorkerFactory factory =
                    WorkerFactory.newInstance(client);

            // ----------------------------------------------------
            // 3️⃣  Create Worker for Queue (pollers from env)
            // ----------------------------------------------------
            WorkerOptions workerOptions = WorkerOptions.newBuilder()
                    .setMaxConcurrentWorkflowTaskPollers(env.getMaxConcurrentWorkflowTaskPollers())
                    .setMaxConcurrentActivityTaskPollers(env.getMaxConcurrentActivityTaskPollers())
                    .build();
            Worker worker =
                    factory.newWorker(taskQueue, workerOptions);

            // Register Workflow
            worker.registerWorkflowImplementationTypes(CoreWorkflowImpl.class);

            // Register activities
            worker.registerActivitiesImplementations(
                    new KernelStageActivityImpl(),
                    new MergePolicyActivityImpl()
            );

            // ----------------------------------------------------
            // 4️⃣  Start Worker
            // ----------------------------------------------------
            factory.start();

            System.out.println("Worker started successfully.");
            System.out.println("Polling task queue: " + taskQueue);

        } catch (Exception e) {

            System.err.println("Worker failed to start.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
