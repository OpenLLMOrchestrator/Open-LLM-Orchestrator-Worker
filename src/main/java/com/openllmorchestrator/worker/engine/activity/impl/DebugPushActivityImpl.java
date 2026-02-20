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
package com.openllmorchestrator.worker.engine.activity.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openllmorchestrator.worker.engine.activity.DebugPushActivity;
import com.openllmorchestrator.worker.engine.config.env.EnvConfig;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.UUID;

/**
 * Pushes serialized execution tree and context to Redis when DEBUGGER is enabled and command has debug=true, debugID (same level as tenantId, userId, operation).
 * Keys: olo:debug:&lt;uuid&gt;:ExecutionTree, olo:debug:&lt;uuid&gt;:context.
 * Each stored value is a JSON object with a unique recordId (UUID) so every object has a different uuid.
 */
@Slf4j
public class DebugPushActivityImpl implements DebugPushActivity {

    private static final String KEY_PREFIX = "olo:debug:";
    private static final String SUFFIX_EXECUTION_TREE = ":ExecutionTree";
    private static final String SUFFIX_CONTEXT = ":context";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void push(String debugId, String executionNodeId, String executionTreeJson, String contextJson) {
        if (debugId == null || debugId.isBlank()) {
            log.warn("DebugPushActivity: debugId is null or blank; skipping push.");
            return;
        }
        EnvConfig env = EnvConfig.fromEnvironment();
        if (env == null || env.getRedis() == null) {
            log.warn("DebugPushActivity: no Redis config; skipping push.");
            return;
        }
        try (Jedis jedis = new Jedis(env.getRedis().getHost(), env.getRedis().getPort())) {
            if (env.getRedis().getPassword() != null && !env.getRedis().getPassword().isBlank()) {
                jedis.auth(env.getRedis().getPassword());
            }
            String executionTreeValue = wrapWithRecordId(debugId, executionNodeId, "executionTree", executionTreeJson);
            String contextValue = wrapWithRecordId(debugId, executionNodeId, "context", contextJson);
            String keyExecutionTree = KEY_PREFIX + debugId + SUFFIX_EXECUTION_TREE;
            String keyContext = KEY_PREFIX + debugId + SUFFIX_CONTEXT;
            jedis.set(keyExecutionTree, executionTreeValue);
            jedis.set(keyContext, contextValue);
            log.info("DebugPushActivity: pushed execution tree and context to Redis (debugId={})", debugId);
        } catch (Exception e) {
            log.error("DebugPushActivity: failed to push to Redis (debugId={})", debugId, e);
            throw new RuntimeException("Debug push to Redis failed: " + e.getMessage(), e);
        }
    }

    /** Build a JSON object with unique recordId, debugId, and executionNodeId (unique per execution node/leaf). */
    private static String wrapWithRecordId(String debugId, String executionNodeId, String payloadKey, String payloadJson) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("recordId", UUID.randomUUID().toString());
        root.put("debugId", debugId);
        if (executionNodeId != null && !executionNodeId.isBlank()) {
            root.put("executionNodeId", executionNodeId);
        }
        if (payloadJson != null && !payloadJson.isBlank()) {
            root.set(payloadKey, MAPPER.readTree(payloadJson));
        } else {
            root.putNull(payloadKey);
        }
        return MAPPER.writeValueAsString(root);
    }
}
