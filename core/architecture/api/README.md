# Core Architecture

High‑level primitives shared across modules in this project.

## ID

Small, cross‑module primitives for strongly‑typed identifiers.

### Core concepts

- Principles:
  - Keep IDs opaque and strongly typed (no raw strings at call sites).
  - Centralize generation/parsing via factories to ensure consistency.
  - Keep the core generic; domain modules extend via typealiases and small factories.
- Building blocks:
  - `BaseIdentifier<T>`: base class for strongly-typed identifiers.
  - `BaseUuidIdentifier`: specialized base for UUID-based identifiers.
  - `IdentifierFactory<T>`: contract for creating/parsing typed IDs.
  - `BaseUuidIdentifierFactory<T>`: abstract UUID-based implementation of `IdentifierFactory<T>`.

Implement custom factories if you need non-UUID schemes; otherwise prefer `BaseUuidIdentifierFactory`.

### Usage

Create a typed ID and factory:

```kotlin
// Typed ID
class ProjectId(value: Uuid) : BaseUuidIdentifier(value)

// Factory
object ProjectIdFactory : BaseUuidIdentifierFactory<ProjectId>(::ProjectId)

// Domain type
data class Project(val id: ProjectId)

// Create new ID
val id: ProjectId = ProjectIdFactory.create()

// Persist/restore
val raw: String = id.toString()
val parsed: ProjectId = ProjectIdFactory.of(raw)
```

