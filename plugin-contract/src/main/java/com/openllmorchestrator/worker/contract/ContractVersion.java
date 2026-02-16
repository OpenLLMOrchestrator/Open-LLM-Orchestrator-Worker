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
package com.openllmorchestrator.worker.contract;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Contract version and compatibility check for plugins.
 * Plugins can implement {@link ContractCompatibility} to declare the contract version they were built for;
 * the worker (or loader) should call {@link #isCompatible(String, String)} before using the plugin.
 */
public final class ContractVersion {

    private static final String FALLBACK_VERSION = "0.0.1";
    private static final Pattern SEMVER = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(-[\\w.]+)?$");

    private ContractVersion() {}

    /**
     * Current contract version (read from contract-version.properties at load time, or fallback).
     */
    public static String getCurrent() {
        return Holder.CURRENT;
    }

    /**
     * Check if a plugin built for {@code requiredByPlugin} is compatible with the runtime contract version {@code current}.
     * Uses same-major version rule: compatible if major versions match and current is not older than required (by version order).
     *
     * @param requiredByPlugin version the plugin declares it was built for (e.g. "0.0.1")
     * @param current          runtime contract version (e.g. from {@link #getCurrent()})
     * @return true if the plugin is considered compatible
     */
    public static boolean isCompatible(String requiredByPlugin, String current) {
        if (requiredByPlugin == null || requiredByPlugin.isBlank()) {
            return true;
        }
        if (current == null || current.isBlank()) {
            return false;
        }
        requiredByPlugin = requiredByPlugin.trim();
        current = current.trim();
        int[] required = parseSemver(requiredByPlugin);
        int[] curr = parseSemver(current);
        if (required == null || curr == null) {
            return requiredByPlugin.equalsIgnoreCase(current);
        }
        if (required[0] != curr[0]) {
            return false;
        }
        if (curr[1] != required[1]) {
            return curr[1] > required[1];
        }
        return curr[2] >= required[2];
    }

    /**
     * Check if the given handler is compatible with the current contract version.
     * If the handler implements {@link ContractCompatibility}, its required version is checked;
     * otherwise the handler is considered compatible.
     */
    public static boolean isCompatible(StageHandler handler) {
        if (handler == null) return true;
        if (!(handler instanceof ContractCompatibility c)) return true;
        String required = c.getRequiredContractVersion();
        return isCompatible(required, getCurrent());
    }

    /**
     * Throws {@link ContractVersionException} if the handler declares a contract version that is not compatible.
     */
    public static void requireCompatible(StageHandler handler) {
        if (handler == null) return;
        if (!(handler instanceof ContractCompatibility c)) return;
        String required = c.getRequiredContractVersion();
        if (!isCompatible(required, getCurrent())) {
            throw new ContractVersionException(
                    "Plugin '" + handler.name() + "' requires contract version " + required
                            + " but runtime contract version is " + getCurrent());
        }
    }

    private static int[] parseSemver(String v) {
        if (v == null) return null;
        java.util.regex.Matcher m = SEMVER.matcher(v.trim());
        if (!m.matches()) return null;
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        return new int[]{major, minor, patch};
    }

    private static final class Holder {
        static final String CURRENT = loadCurrent();
    }

    private static String loadCurrent() {
        try (InputStream in = ContractVersion.class.getResourceAsStream("/contract-version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("version");
                if (v != null && !v.isBlank()) return v.trim();
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_VERSION;
    }
}
