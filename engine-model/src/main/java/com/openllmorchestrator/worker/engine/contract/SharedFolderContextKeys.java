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
package com.openllmorchestrator.worker.engine.contract;

/**
 * Well-known keys for shared resources available to any plugin.
 * When the worker sets {@link #SHARED_FOLDER_PATH}, plugins can resolve file/folder paths relative to this
 * so upload and file-read requests are scoped to the mounted shared directory.
 */
public final class SharedFolderContextKeys {

    /** ExecutionContext key: root path for a shared folder (mount in container). Any plugin may resolve paths relative to this. */
    public static final String SHARED_FOLDER_PATH = "sharedFolderPath";

    private SharedFolderContextKeys() {}
}
