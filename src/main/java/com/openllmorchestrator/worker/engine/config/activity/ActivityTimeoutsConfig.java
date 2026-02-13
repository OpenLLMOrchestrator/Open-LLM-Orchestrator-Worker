package com.openllmorchestrator.worker.engine.config.activity;

import lombok.Getter;

/** Activity timeout defaults from config. All in seconds. */
@Getter
public class ActivityTimeoutsConfig {
    private Integer scheduleToStartSeconds;
    private Integer startToCloseSeconds = 30;
    private Integer scheduleToCloseSeconds;
}
