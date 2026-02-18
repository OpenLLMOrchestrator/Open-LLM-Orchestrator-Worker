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
package com.openllmorchestrator.worker.engine.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves the runtime plugins directory (env or default), scans for {@code *.zip} and {@code *.olo}
 * files, expands them using the same structure as the build (zip → optional .olo entries → JARs),
 * and returns paths to extracted JAR files for loading via {@link DynamicPluginLoader}.
 * <p>
 * Directory resolution: environment variable {@value #PLUGINS_DIR_ENV}, then system property
 * {@value #PLUGINS_DIR_PROPERTY}, then {@code user.dir/plugins}.
 */
@Slf4j
public final class RuntimePluginDirLoader {

    /** Environment variable to override the default plugins directory. */
    public static final String PLUGINS_DIR_ENV = "PLUGINS_DIR";
    /** System property to override the default plugins directory (used if env is not set). */
    public static final String PLUGINS_DIR_PROPERTY = "plugins.dir";
    /** Default subdirectory under user.dir when neither env nor property is set. */
    public static final String DEFAULT_PLUGINS_SUBDIR = "plugins";

    private RuntimePluginDirLoader() {}

    /**
     * Resolves the plugins directory: env {@value #PLUGINS_DIR_ENV}, then property {@value #PLUGINS_DIR_PROPERTY}, then user.dir/{@value #DEFAULT_PLUGINS_SUBDIR}.
     *
     * @return resolved path (may not exist)
     */
    public static Path resolvePluginsDir() {
        String fromEnv = System.getenv(PLUGINS_DIR_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Paths.get(fromEnv).normalize().toAbsolutePath();
        }
        String fromProp = System.getProperty(PLUGINS_DIR_PROPERTY);
        if (fromProp != null && !fromProp.isBlank()) {
            return Paths.get(fromProp).normalize().toAbsolutePath();
        }
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, DEFAULT_PLUGINS_SUBDIR).normalize().toAbsolutePath();
    }

    /**
     * Scans the resolved plugins directory for {@code *.zip} and {@code *.olo} files, expands them
     * (zip → .olo as zip → JARs), copies JARs into a temporary directory with unique names, and returns
     * their paths. The temp directory is not deleted so that classloaders can keep using the JARs.
     *
     * @return list of paths to JAR files to load; empty if the plugins dir does not exist or contains no zip/olo
     */
    public static List<Path> collectJarPathsFromPluginsDir() {
        Path pluginsDir = resolvePluginsDir();
        if (!Files.isDirectory(pluginsDir)) {
            log.debug("Runtime plugins dir not present or not a directory: {}; skipping.", pluginsDir);
            return List.of();
        }

        List<Path> zipAndOlo = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, p -> {
            if (!Files.isRegularFile(p)) return false;
            String name = p.getFileName().toString().toLowerCase();
            return name.endsWith(".zip") || name.endsWith(".olo");
        })) {
            stream.forEach(zipAndOlo::add);
        } catch (IOException e) {
            log.warn("Failed to list runtime plugins dir {}: {}; skipping.", pluginsDir, e.getMessage());
            return List.of();
        }

        if (zipAndOlo.isEmpty()) {
            log.debug("No *.zip or *.olo files in {}; skipping runtime plugin dir.", pluginsDir);
            return List.of();
        }

        Path extractRoot;
        try {
            extractRoot = Files.createTempDirectory("open-llm-orchestrator-plugins-");
        } catch (IOException e) {
            log.warn("Failed to create temp dir for runtime plugins: {}; skipping.", e.getMessage());
            return List.of();
        }

        Set<String> usedNames = new HashSet<>();
        List<Path> jarPaths = new ArrayList<>();

        for (Path zipOrOlo : zipAndOlo) {
            String zipBase = baseName(zipOrOlo.getFileName().toString(), ".zip", ".olo");
            try {
                collectJarsFromZipOrOlo(zipOrOlo, zipBase, extractRoot, usedNames, jarPaths);
            } catch (IOException e) {
                log.warn("Failed to expand runtime plugin archive {}: {}; skipping.", zipOrOlo.getFileName(), e.getMessage());
            }
        }

        if (jarPaths.isEmpty()) {
            log.debug("No JARs extracted from runtime plugins dir {}.", pluginsDir);
        } else {
            log.info("Runtime plugins dir: extracted {} JAR(s) from {} archive(s) in {} -> {}", jarPaths.size(), zipAndOlo.size(), pluginsDir, extractRoot);
        }
        return jarPaths;
    }

    private static String baseName(String fileName, String... suffixes) {
        String lower = fileName.toLowerCase();
        for (String suf : suffixes) {
            if (lower.endsWith(suf)) {
                return fileName.substring(0, fileName.length() - suf.length());
            }
        }
        return fileName;
    }

    /**
     * Expands the given zip/olo file: if it contains .olo entries, expands each and collects JARs;
     * otherwise looks for olo/ dir, olo.zip, or any .jar at root. Copies each JAR into extractRoot with a unique name.
     */
    private static void collectJarsFromZipOrOlo(Path zipPath, String zipBase, Path extractRoot, Set<String> usedNames, List<Path> jarPaths) throws IOException {
        Path expandedZip = extractRoot.resolve("expanded-" + zipBase);
        Files.createDirectories(expandedZip);
        extractZip(zipPath, expandedZip);

        List<Path> oloFiles = new ArrayList<>();
        try (var walk = Files.walk(expandedZip)) {
            walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".olo"))
                .forEach(oloFiles::add);
        }

        if (!oloFiles.isEmpty()) {
            for (Path oloFile : oloFiles) {
                String oloBase = baseName(oloFile.getFileName().toString(), ".olo");
                Path oloExpanded = expandedZip.resolve("olo-" + oloBase);
                Files.createDirectories(oloExpanded);
                extractZip(oloFile, oloExpanded);
                collectJarFilesInto(oloExpanded, zipBase + "-" + oloBase, extractRoot, usedNames, jarPaths);
            }
        } else {
            Path oloDir = expandedZip.resolve("olo");
            Path oloZip = expandedZip.resolve("olo.zip");
            if (Files.isDirectory(oloDir)) {
                collectJarFilesInto(oloDir, zipBase, extractRoot, usedNames, jarPaths);
            } else if (Files.isRegularFile(oloZip)) {
                Path oloExpanded = expandedZip.resolve("olo-expanded");
                Files.createDirectories(oloExpanded);
                extractZip(oloZip, oloExpanded);
                collectJarFilesInto(oloExpanded, zipBase, extractRoot, usedNames, jarPaths);
            } else {
                collectJarFilesInto(expandedZip, zipBase, extractRoot, usedNames, jarPaths);
            }
        }
    }

    private static void extractZip(Path zipPath, Path destDir) throws IOException {
        try (ZipFile zf = new ZipFile(zipPath.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                Path out = destDir.resolve(e.getName()).normalize();
                if (!out.startsWith(destDir)) continue; // avoid zip slip
                Files.createDirectories(out.getParent());
                try (InputStream in = zf.getInputStream(e)) {
                    Files.copy(in, out);
                }
            }
        }
    }

    private static void collectJarFilesInto(Path dir, String prefix, Path extractRoot, Set<String> usedNames, List<Path> jarPaths) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jar"))
                .forEach(jar -> {
                    try {
                        String baseName = prefix + "-" + jar.getFileName().toString();
                        String uniqueName = baseName;
                        int n = 0;
                        while (usedNames.contains(uniqueName)) {
                            uniqueName = prefix + "-" + (++n) + "-" + jar.getFileName().toString();
                        }
                        usedNames.add(uniqueName);
                        Path target = extractRoot.resolve(uniqueName);
                        Files.copy(jar, target);
                        jarPaths.add(target);
                    } catch (IOException ex) {
                        log.warn("Failed to copy JAR {} to extract dir: {}", jar, ex.getMessage());
                    }
                });
        }
    }
}
