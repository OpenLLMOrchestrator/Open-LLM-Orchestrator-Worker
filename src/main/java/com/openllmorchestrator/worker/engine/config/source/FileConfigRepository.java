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
package com.openllmorchestrator.worker.engine.config.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Reads engine config JSON from mounted file. Per-queue: basePath/queueName.json; if not found, fallback to default path. */
public final class FileConfigRepository implements ConfigRepository {
    private static final String DEFAULT_PATH = "config/default.json";
    private static final String DEFAULT_QUEUE_KEY = "default";

    private final String configFilePath;
    private final String queueName;

    public FileConfigRepository(String configFilePath) {
        this(configFilePath, null);
    }

    /** @param queueName when set, load basePath/queueName.json first; if not available, load default (config/default.json or configFilePath). */
    public FileConfigRepository(String configFilePath, String queueName) {
        this.configFilePath = configFilePath != null && !configFilePath.isBlank() ? configFilePath : DEFAULT_PATH;
        this.queueName = queueName != null && !queueName.isBlank() ? queueName.trim() : null;
    }

    @Override
    public String get() {
        if (queueName != null) {
            Path queuePath = resolveQueuePath();
            if (queuePath != null && Files.isRegularFile(queuePath)) {
                try {
                    String content = Files.readString(queuePath, StandardCharsets.UTF_8);
                    if (content != null && !content.isBlank()) {
                        return content;
                    }
                } catch (IOException ignored) {
                    // fall through to default
                }
            }
            Path defaultPath = resolveDefaultPath();
            if (defaultPath != null && Files.isRegularFile(defaultPath)) {
                try {
                    return Files.readString(defaultPath, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }
        Path path = Paths.get(configFilePath);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveQueuePath() {
        if (queueName == null) return null;
        Path base = Paths.get(configFilePath);
        if (configFilePath.endsWith(".json") && Files.isRegularFile(base)) {
            base = base.getParent();
        }
        return base.resolve(queueName + ".json");
    }

    private Path resolveDefaultPath() {
        Path base = Paths.get(configFilePath);
        if (configFilePath.endsWith(".json") && Files.isRegularFile(base)) {
            return base;
        }
        return base.resolve(DEFAULT_QUEUE_KEY + ".json");
    }

    @Override
    public void set(String configJson) {
        // File is read-only; persistence goes to Redis/DB only
    }
}

