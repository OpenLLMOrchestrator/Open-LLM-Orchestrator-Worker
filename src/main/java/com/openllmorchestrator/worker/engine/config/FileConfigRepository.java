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
package com.openllmorchestrator.worker.engine.config;

import com.openllmorchestrator.worker.engine.config.EngineConfigMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileConfigRepository {

    private static final EngineConfigMapper MAPPER = EngineConfigMapper.getInstance();

    public QueueConfig find(String queueName) {

        try {
            File file =
                    new File("config/" +
                            queueName + ".json");

            if (!file.exists())
                return null;

            try (InputStream in = new FileInputStream(file)) {
                return MAPPER.queueConfigFromJson(in);
            }

        } catch (Exception e) {
            return null;
        }
    }
}

