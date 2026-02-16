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

/** Reads engine config JSON from mounted file. Path from env (default config/&lt;CONFIG_KEY&gt;.json). Write not supported. */
public final class FileConfigRepository implements ConfigRepository {
    private static final String DEFAULT_PATH = "config/default.json";

    private final String configFilePath;

    public FileConfigRepository(String configFilePath) {
        this.configFilePath = configFilePath != null && !configFilePath.isBlank() ? configFilePath : DEFAULT_PATH;
    }

    @Override
    public String get() {
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

    @Override
    public void set(String configJson) {
        // File is read-only; persistence goes to Redis/DB only
    }
}

