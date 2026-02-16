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
package com.openllmorchestrator.worker.plugin.folder;

import com.openllmorchestrator.worker.contract.ContractCompatibility;
import com.openllmorchestrator.worker.contract.PluginContext;
import com.openllmorchestrator.worker.contract.PlannerInputDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypeDescriptor;
import com.openllmorchestrator.worker.contract.PluginTypes;
import com.openllmorchestrator.worker.contract.StageHandler;
import com.openllmorchestrator.worker.contract.StageResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads all files from a folder (optionally filtered by extension) and outputs
 * them as tokenizedChunks for the vector DB plugin to store.
 * <p>
 * Input: "folderPath" (required), optional "fileExtensions" (e.g. ".txt,.md"),
 * optional "recursive" (boolean, default false).
 */
public final class FolderIngestionPlugin implements StageHandler, ContractCompatibility, PlannerInputDescriptor, PluginTypeDescriptor {

    private static final String CONTRACT_VERSION = "0.0.1";
    public static final String NAME = "com.openllmorchestrator.worker.plugin.folder.FolderIngestionPlugin";
    private static final String DEFAULT_EXTENSIONS = ".txt,.md";
    private static final Set<String> DEFAULT_EXTENSION_SET = Set.of(".txt", ".md");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public StageResult execute(PluginContext context) {
        Map<String, Object> input = context.getOriginalInput();
        String folderPath = (String) input.get("folderPath");
        if (folderPath == null || folderPath.isBlank()) {
            context.putOutput("error", "input.folderPath is required");
            context.putOutput("tokenizedChunks", List.<Map<String, Object>>of());
            return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
        }

        Set<String> extensions = parseExtensions((String) input.get("fileExtensions"));
        boolean recursive = Boolean.TRUE.equals(input.get("recursive"));

        Path base = Paths.get(folderPath);
        if (!base.isAbsolute()) {
            base = Paths.get(System.getProperty("user.dir", ".")).resolve(folderPath).normalize();
        }

        List<Map<String, Object>> chunks = new ArrayList<>();
        try {
            List<Path> files = listFiles(base, recursive, extensions);
            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                String relativePath = base.relativize(file).toString();
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("path", relativePath);
                chunk.put("text", text);
                chunk.put("index", i);
                chunks.add(chunk);
            }
        } catch (IOException e) {
            context.putOutput("error", "Failed to read folder: " + e.getMessage());
        }

        context.putOutput("tokenizedChunks", chunks);
        context.putOutput("fileCount", chunks.size());

        return StageResult.builder().stageName(NAME).data(new HashMap<>(context.getCurrentPluginOutput())).build();
    }

    @Override
    public String getRequiredContractVersion() {
        return CONTRACT_VERSION;
    }

    @Override
    public java.util.Set<String> getRequiredInputFieldsForPlanner() {
        return Set.of("folderPath", "fileExtensions", "recursive");
    }

    @Override
    public String getPlannerDescription() {
        return "Filter: ingest folder files into tokenizedChunks for vector store.";
    }

    @Override
    public String getPluginType() {
        return PluginTypes.FILTER;
    }

    private static Set<String> parseExtensions(String fileExtensions) {
        if (fileExtensions == null || fileExtensions.isBlank()) {
            return DEFAULT_EXTENSION_SET;
        }
        return Stream.of(fileExtensions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith(".") ? s : "." + s)
                .collect(Collectors.toSet());
    }

    private static List<Path> listFiles(Path base, boolean recursive, Set<String> extensions) throws IOException {
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        if (recursive) {
            try (Stream<Path> walk = Files.walk(base)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> extensions.stream().anyMatch(ext -> p.toString().toLowerCase().endsWith(ext)))
                        .forEach(out::add);
            }
        } else {
            try (Stream<Path> list = Files.list(base)) {
                list.filter(Files::isRegularFile)
                        .filter(p -> extensions.stream().anyMatch(ext -> p.toString().toLowerCase().endsWith(ext)))
                        .forEach(out::add);
            }
        }
        out.sort(Path::compareTo);
        return out;
    }
}


