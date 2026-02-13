package com.openllmorchestrator.worker;

import com.openllmorchestrator.worker.engine.activity.impl.KernelStageActivityImpl;
import com.openllmorchestrator.worker.engine.bootstrap.WorkerBootstrap;
import com.openllmorchestrator.worker.engine.config.EngineFileConfig;
import com.openllmorchestrator.worker.engine.runtime.EngineRuntime;
import com.openllmorchestrator.worker.workflow.impl.CoreWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

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
            // 2️⃣  Connect to Temporal
            // ----------------------------------------------------
            WorkflowServiceStubsOptions serviceOptions =
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget("localhost:7233")
                            .build();

            WorkflowServiceStubs service =
                    WorkflowServiceStubs.newServiceStubs(serviceOptions);

            WorkflowClient client =
                    WorkflowClient.newInstance(service);

            WorkerFactory factory =
                    WorkerFactory.newInstance(client);

            // ----------------------------------------------------
            // 3️⃣  Create Worker for Queue
            // ----------------------------------------------------
            Worker worker =
                    factory.newWorker(taskQueue);

            // Register Workflow
            worker.registerWorkflowImplementationTypes(CoreWorkflowImpl.class);

            // Register Kernel Stage Activity
            worker.registerActivitiesImplementations(
                    new KernelStageActivityImpl()
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
