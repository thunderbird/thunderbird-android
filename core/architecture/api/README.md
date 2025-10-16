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
  - `Id<T>`: a tiny value type wrapping a UUID (kotlin.uuid.Uuid).
  - `IdFactory<T>`: contract for creating/parsing typed IDs.
  - `BaseIdFactory<T>`: abstract UUID‑based implementation of `IdFactory<T>` for standardized creation and parsing.

Implement custom factories if you need non‑UUID schemes; otherwise prefer BaseIdFactory.

### Usage

Create a typed ID and parse from storage:

```kotlin
// Domain type
data class Project(val id: ProjectId)

// Typed alias
typealias ProjectId = Id<Project>

// Factory
object ProjectIdFactory : BaseIdFactory<Project>()

// Create new ID
val id: ProjectId = ProjectIdFactory.create()

// Persist/restore
val raw: String = id.asRaw()
val parsed: ProjectId = ProjectIdFactory.of(raw)
```

