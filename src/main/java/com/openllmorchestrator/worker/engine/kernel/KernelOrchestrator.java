package com.openllmorchestrator.worker.engine.kernel;

import com.openllmorchestrator.worker.engine.contract.ExecutionContext;
import com.openllmorchestrator.worker.engine.contract.StageResult;
import com.openllmorchestrator.worker.engine.stage.StageDefinition;
import com.openllmorchestrator.worker.engine.stage.StageExecutionMode;
import com.openllmorchestrator.worker.engine.stage.StagePlan;
import io.temporal.workflow.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KernelOrchestrator {

    private final StageInvoker stageInvoker;

    public KernelOrchestrator(StageInvoker stageInvoker) {
        this.stageInvoker = stageInvoker;
    }

    public void execute(StagePlan plan, ExecutionContext context) {

        for (List<StageDefinition> group : plan.getGroups()) {

            log.info("---- Executing Stage Group ----");

            if (group.get(0).getExecutionMode()
                    == StageExecutionMode.ASYNC) {

                List<Promise<StageResult>> promises =
                        new ArrayList<>();

                for (StageDefinition def : group) {
                    log.info("Scheduling ASYNC stage: {}",
                            def.getName());
                    promises.add(stageInvoker.invokeAsync(def));
                }

                Promise.allOf(promises).get();

                for (Promise<StageResult> p : promises) {
                    log.info("Completed ASYNC stage: {}",
                            p.get().getStageName());
                }

            } else {

                for (StageDefinition def : group) {
                    log.info("Executing SYNC stage: {}",
                            def.getName());

                    StageResult result =
                            stageInvoker.invokeSync(def);

                    log.info("Completed SYNC stage: {}",
                            result.getStageName());
                }
            }
        }
    }
}
