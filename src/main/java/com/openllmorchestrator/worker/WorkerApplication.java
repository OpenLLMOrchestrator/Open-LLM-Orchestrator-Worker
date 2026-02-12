package com.openllmorchestrator.worker;

import com.openllmorchestrator.worker.activity.impl.EmbeddingActivityImpl;
import com.openllmorchestrator.worker.activity.impl.ModelActivityImpl;
import com.openllmorchestrator.worker.activity.impl.RetrievalActivityImpl;
import com.openllmorchestrator.worker.workflow.impl.RagWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class WorkerApplication {

    public static void main(String[] args) {

        System.out.println("Starting Temporal Worker...");

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

        Worker worker =
                factory.newWorker("rag-task-queue");

        // ✅ Register workflow
        worker.registerWorkflowImplementationTypes(RagWorkflowImpl.class);

        // ✅ Register activities
        worker.registerActivitiesImplementations(
                new EmbeddingActivityImpl(),
                new RetrievalActivityImpl(),
                new ModelActivityImpl()
        );

        factory.start();

        System.out.println("Worker started and polling rag-task-queue.");
    }
}
