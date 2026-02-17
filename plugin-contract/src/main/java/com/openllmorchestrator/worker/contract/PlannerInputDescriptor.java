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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Optional interface for plugins to declare which input fields they need from
 * {@link PluginContext#getOriginalInput()} or {@link PluginContext#getAccumulatedOutput()}.
 * <p>
 * When building the planner phase LL (language model) query, the worker can collect
 * {@link #getRequiredInputFieldsForPlanner()} from all compatible plugins, take the union,
 * and render a data input payload that includes those keys so the planner LLM has the
 * context it needs to decide which plugins to invoke or how to structure the dynamic plan.
 * <p>
 * Implement this interface if your plugin consumes specific keys from context and you want
 * those keys to be included in the planner's input. Plugins that do not implement this
 * are ignored when aggregating required fields for the planner.
 */
public interface PlannerInputDescriptor {

    /**
     * Keys from originalInput or accumulatedOutput that this plugin reads.
     * The worker uses the union of these sets (across all plugins) to build the
     * data input for the planner phase LL query.
     * <p>
     * Use the same key names as in pipeline input (e.g. {@code "question"}, {@code "document"},
     * {@code "modelId"}, {@code "messages"}). Return an empty set if this plugin does not
     * declare required fields.
     */
    Set<String> getRequiredInputFieldsForPlanner();

    /**
     * Optional one-line description of this plugin for the planner (e.g. for inclusion in
     * the LLM system prompt or tool list). Return null or empty to omit.
     */
    default String getPlannerDescription() {
        return null;
    }

    /**
     * Empty descriptor: no required fields and no description.
     */
    static Set<String> noRequiredFields() {
        return Collections.emptySet();
    }

    /**
     * Collects the union of all required input fields from handlers that implement
     * {@link PlannerInputDescriptor}. Use when building the planner phase LL query
     * to know which keys to pull from context and include in the planner's data input.
     *
     * @param handlers stage handlers (e.g. compatible plugin registry values)
     * @return union of required field keys from all descriptors; never null
     */
    static Set<String> collectRequiredFields(Iterable<? extends CapabilityHandler> handlers) {
        Set<String> union = new LinkedHashSet<>();
        if (handlers == null) {
            return union;
        }
        for (CapabilityHandler h : handlers) {
            if (h instanceof PlannerInputDescriptor) {
                Set<String> fields = ((PlannerInputDescriptor) h).getRequiredInputFieldsForPlanner();
                if (fields != null) {
                    union.addAll(fields);
                }
            }
        }
        return union;
    }

    /**
     * Collects plugin name, required fields, description and optional type for each handler
     * that implements {@link PlannerInputDescriptor}. Type is set when the handler also
     * implements {@link PluginTypeDescriptor}.
     *
     * @param handlers stage handlers (e.g. compatible plugin registry values)
     * @return list of planner-facing plugin info; never null
     */
    static List<PlannerPluginInfo> collectPluginInfo(Iterable<? extends CapabilityHandler> handlers) {
        return collectPluginInfoOrAvailableTools(handlers, null);
    }

    /**
     * Collects "available tools" for the planner: handlers that implement
     * {@link PlannerInputDescriptor} and (when {@link PluginTypeDescriptor} is implemented)
     * have {@link PluginTypeDescriptor#getPluginType()} in {@code typesToInclude}.
     * Use this to send only plugins of a given type (e.g. {@link PluginTypes#TOOL}) as
     * available tools to the planner LLM.
     *
     * @param handlers       stage handlers (e.g. compatible plugin registry values)
     * @param typesToInclude if non-null and non-empty, only include handlers that implement
     *                       {@link PluginTypeDescriptor} and whose type is in this set.
     *                       If null or empty, include all handlers that implement {@link PlannerInputDescriptor}.
     * @return list of planner-facing plugin info to send as available tools; never null
     */
    static List<PlannerPluginInfo> collectAvailableTools(Iterable<? extends CapabilityHandler> handlers,
                                                         Set<String> typesToInclude) {
        return collectPluginInfoOrAvailableTools(handlers, typesToInclude);
    }

    private static List<PlannerPluginInfo> collectPluginInfoOrAvailableTools(
            Iterable<? extends CapabilityHandler> handlers, Set<String> typesToInclude) {
        List<PlannerPluginInfo> out = new ArrayList<>();
        if (handlers == null) {
            return out;
        }
        boolean filterByType = typesToInclude != null && !typesToInclude.isEmpty();
        for (CapabilityHandler h : handlers) {
            if (!(h instanceof PlannerInputDescriptor)) {
                continue;
            }
            if (filterByType) {
                if (!(h instanceof PluginTypeDescriptor)) {
                    continue;
                }
                String type = ((PluginTypeDescriptor) h).getPluginType();
                if (type == null || !typesToInclude.contains(type)) {
                    continue;
                }
            }
            PlannerInputDescriptor d = (PlannerInputDescriptor) h;
            String desc = d.getPlannerDescription();
            if (desc != null && desc.isBlank()) {
                desc = null;
            }
            String pluginType = (h instanceof PluginTypeDescriptor) ? ((PluginTypeDescriptor) h).getPluginType() : null;
            out.add(new PlannerPluginInfo(h.name(), d.getRequiredInputFieldsForPlanner(), desc, pluginType));
        }
        return out;
    }

    /**
     * Snapshot of a plugin's planner-facing info: name, required input keys, optional description and type.
     * When sending "available tools", use {@link #collectAvailableTools(Iterable, Set)} so only
     * plugins of the desired type (e.g. {@link PluginTypes#TOOL}) are included.
     */
    record PlannerPluginInfo(String pluginName, Set<String> requiredInputFields, String description, String pluginType) {
        public PlannerPluginInfo {
            pluginName = pluginName != null ? pluginName : "";
            requiredInputFields = requiredInputFields != null ? Collections.unmodifiableSet(new LinkedHashSet<>(requiredInputFields)) : Set.of();
            pluginType = (pluginType != null && pluginType.isBlank()) ? null : pluginType;
        }

        /** Constructor without pluginType (backward compatibility). */
        public PlannerPluginInfo(String pluginName, Set<String> requiredInputFields, String description) {
            this(pluginName, requiredInputFields, description, null);
        }
    }
}
