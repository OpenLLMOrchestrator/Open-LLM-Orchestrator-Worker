package com.openllmorchestrator.worker.engine.bootstrap;

/** One bootstrap step. Add new steps without changing existing ones (OCP). */
public interface BootstrapStep {
    void run(BootstrapContext ctx);
}
