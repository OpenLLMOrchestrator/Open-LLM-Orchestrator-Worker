# engine-model

Execution tree and context data types, **serializable** and **deserializable** (JSON via Jackson).

## Contents

- **Execution tree** (`com.openllmorchestrator.worker.engine.capability`): `CapabilityPlan`, `CapabilityGroupSpec`, `CapabilityDefinition`, `CapabilityPlanBuilder`, `AsyncCompletionPolicy`, `CapabilityExecutionMode`, `CapabilityRetryOptions`.
- **Context / contract** (`com.openllmorchestrator.worker.engine.contract`): `ExecutionCommand`, `VersionedState`, `ExecutionMetadata`, `ExecutionSignal`, `ExecutionMode`, `ContextSnapshot`, `PlannerContextKeys`, `KernelExecutionOutcome`.

Runtime types that hold non-serializable references (e.g. `ExecutionContext`, `FeatureFlagsProvider`) remain in the main worker module and use these types.

## Serialization

Use `ExecutionModelSerde` for JSON round-trip:

```java
// Plan
String json = ExecutionModelSerde.serializePlan(plan);
CapabilityPlan plan = ExecutionModelSerde.deserializePlan(json);

// Command, VersionedState, ExecutionSignal, KernelExecutionOutcome
String cmdJson = ExecutionModelSerde.serializeCommand(command);
ExecutionCommand cmd = ExecutionModelSerde.deserializeCommand(cmdJson);
```

Shared `ObjectMapper`: `ExecutionModelSerde.objectMapper()` (Java 8 time module registered, no date-as-timestamp).

## Dependencies

- **plugin-contract** (api): `DeterminismPolicy`, `PluginContext`, `AgentContext` used by context DTOs.
- **Jackson** (databind, jsr310): JSON serialization.
