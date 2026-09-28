# Repository Pattern

This guide defines the repository API conventions used by the project. It supports
[ADR-0010](../engineering/adr/0010-adopt-project-wide-repository-pattern.md).

## Core concepts

A **repository contract** describes the domain data and operations available to a caller. In Kotlin, it is usually an
interface. The contract defines what an operation needs, what it returns, and which domain failures a caller can handle,
but not whether the data comes from a database, a network service, or another source.

A **repository implementation** is a class that fulfills that contract. It contains the implementation details and may
coordinate local storage, remote services, mapping, and synchronization. Callers depend on the contract rather than the
implementation, so these details can change without changing the callers.

A **focused contract** provides the operations for one coherent domain responsibility. Focused contracts are independent
APIs. Callers use one or more focused contracts directly, according to their responsibilities.

In this guide, **API** usually refers to an area's `:api` module. It contains contracts and the types needed to use them.
The corresponding `:internal` module contains their implementations and other details that callers must not depend on.

## Responsibility and ownership

A repository is domain-facing and hides how data is read, stored, or synchronized. UI state and UI-specific business
logic belong elsewhere.

Define contracts by domain responsibility. Do not create one interface per database table or one interface per SQL
operation. A caller depends on the narrowest focused contracts it needs.

Expose a repository contract from an `:api` module only when another area needs that stable contract. Place the shared
contract, criteria, errors, and domain models in that module. Keep repository implementations, data sources, mappers,
and storage models in the area's `:internal` module.

Bind the contract to its implementation in `:app-common` or an app-specific composition module. Dependency injection
then provides the implementation when a caller requests the contract, without exposing the implementation to that
caller. This maintains the API/internal boundary defined by
[ADR-0009](../engineering/adr/0009-api-internal-split.md).

## Scope and identifiers

Make the identifiers that scope every operation explicit. For example, an operation on a folder within an account takes
both `AccountId` and `FolderId`. An account-scoped operation continues to take `AccountId`, even if a folder identifier
is also supplied. Do not create a repository instance that is permanently bound to one account, folder, or other entity.

Use domain-specific identifier types such as `AccountId` and `FolderId` in new contracts rather than raw primitives.
This makes the scope visible to callers and supports a consolidated database with one connection for all accounts.

## Asynchronous work and results

Repository I/O uses coroutines:

- A one-shot operation is a `suspend fun`.
- A reactive operation returns a `Flow`.

Choose the return type based on the guarantees and failures the repository contract exposes to callers, rather than the
behavior of its current data source or a legacy implementation. Use a fallible return type when an operation can
encounter an expected, recoverable condition that the caller needs to handle, such as unavailable storage, a network
failure, a rejected operation, or a missing required entity. Return a value directly only when the contract guarantees
a meaningful result, possibly by using a default.

Use [`net.thunderbird.core.outcome.Outcome`](../../core/outcome/src/commonMain/kotlin/net/thunderbird/core/outcome/Outcome.kt)
for fallible repository results. `ERROR` is a domain-specific sealed type. Do not expose database, HTTP, or platform
exceptions from a contract.

Fallible contracts use `Outcome`:

|    Contract    |         Return type          |
|----------------|------------------------------|
| Required value | `Outcome<Type, Error>`       |
| Optional value | `Outcome<Type?, Error>`      |
| Collection     | `Outcome<List<Type>, Error>` |
| No value       | `Outcome<Unit, Error>`       |
| Reactive value | `Flow<Outcome<Type, Error>>` |

Non-fallible contracts return values directly:

|    Contract    | Return type  |
|----------------|--------------|
| Required value | `Type`       |
| Optional value | `Type?`      |
| Collection     | `List<Type>` |
| No value       | `Unit`       |
| Reactive value | `Flow<Type>` |

For fallible operations, use `Outcome.Success(Unit)` when an operation completes successfully and has no value to
return. Use `Outcome.Success(null)` when a lookup completes successfully but no matching entity exists. Use
`Outcome.Failure(...)` when the operation cannot produce the required result. An API that requires an entity represents
a missing entity as a domain failure.

For example, this repository always returns settings by falling back to defaults, while an update can fail:

```kotlin
interface DisplaySettingsRepository {
    suspend fun getById(
        accountId: AccountId,
    ): DisplaySettings

    fun observeById(
        accountId: AccountId,
    ): Flow<DisplaySettings>

    suspend fun update(
        accountId: AccountId,
        settings: DisplaySettings,
    ): Outcome<Unit, DisplaySettingsError>
}
```

## Read method naming

Use these names for read methods:

- Reactive reads:
  - `observeById()` reads one entity by its main identifier.
  - `observeBy<Identifier>()` reads one entity by another identifier, such as `observeByServerId()`.
  - `observeAll()` reads all entities in scope.
  - `observeByCriteria()` reads at most one entity for an immutable, domain-specific criteria type.
  - `observeAllByCriteria()` reads multiple entities for an immutable, domain-specific criteria type.
- One-shot reads:
  - Optional singular reads use `findById()`, `findBy<Identifier>()`, or `findByCriteria()`. They always return a
    nullable entity, either as `Type?` or `Outcome<Type?, Error>`. Return `null` directly or `Outcome.Success(null)`
    when no entity exists.
  - Collection reads use `findAll()` or `findAllByCriteria()`. Return an empty list directly or
    `Outcome.Success(emptyList())` when no entities exist.
  - Required singular reads use `getById()` or `getBy<Identifier>()`. They always return a non-null entity, either as
    `Type` or `Outcome<Type, Error>`. A direct return guarantees a meaningful value. A fallible return represents absence
    as a domain failure.
  - Criteria methods accept an immutable, domain-specific criteria type.

Reactive reads use the same absence semantics as one-shot reads. An optional singular read emits `null` directly or
`Outcome.Success(null)`. A required singular read emits a value directly or a domain failure. A collection read emits an
empty list directly or `Outcome.Success(emptyList())` when no entities exist.

## Mutation methods

Use `create()`, `update()`, and `delete()` for lifecycle operations. Do not add a generic `save()` method to new
contracts. Use `clear<Scope>()` only for explicitly scoped bulk removal, such as `clearCache()`.

Within a focused repository contract, method names describe the operation and must not repeat the domain concern named
by the repository. For example, use `DraftRepository.create()`, `FolderQueryRepository.findById()`, and
`FolderQueryRepository.findByServerId()`, not `createDraft()`, `findByFolderId()`, or `findFolderByServerId()`. Use
`FolderQueryRepository.getById()` or `FolderQueryRepository.getByServerId()` for required lookups. Method names are
unambiguous within their focused contract. The repository contract name identifies the concern and is not repeated in
its method names.

Avoid repository methods whose names start with `set`. Multiple independent setters expose invalid intermediate state
and usually mean the API is acting as a mutable state holder. Group values that change together in an immutable value
and update them atomically. A setup draft or UI-only mutable state belongs in a `Store` or `StateHolder`, not
a repository.

```kotlin
data class FolderSettings(
    val includeInUnifiedInbox: Boolean,
    val visible: Boolean,
    val syncEnabled: Boolean,
    val notificationsEnabled: Boolean,
)

interface FolderSettingsRepository {
    fun observeById(
        accountId: AccountId,
        folderId: FolderId,
    ): Flow<Outcome<FolderSettings, FolderError>>

    suspend fun update(
        accountId: AccountId,
        folderId: FolderId,
        settings: FolderSettings,
    ): Outcome<Unit, FolderError>
}
```

Creation is deliberately different from a generic write. A draft has its own creation lifecycle and identifier, while
changes to an existing draft use `update()`:

```kotlin
interface DraftRepository {
    suspend fun create(
        accountId: AccountId,
    ): Outcome<Draft, DraftError>

    suspend fun update(
        accountId: AccountId,
        draftId: DraftId,
        draft: Draft,
    ): Outcome<Unit, DraftError>

    suspend fun delete(
        accountId: AccountId,
        draftId: DraftId,
    ): Outcome<Unit, DraftError>
}
```

## Folder contract example

The following example shows independent, focused folder contracts. Consumers depend directly on the contracts matching
their responsibilities.

```kotlin
enum class UnifiedInboxFilter {
    ANY,
    INCLUDED,
    EXCLUDED,
}

data class FolderCriteria(
    val unifiedInbox: UnifiedInboxFilter = UnifiedInboxFilter.ANY,
    val excludeLocalOnly: Boolean = false,
)

sealed interface FolderError {
    data object NotFound : FolderError
    data object Unavailable : FolderError
}

interface FolderQueryRepository {
    fun observeById(
        accountId: AccountId,
        folderId: FolderId,
    ): Flow<Outcome<Folder?, FolderError>>

    fun observeAllByCriteria(
        accountId: AccountId,
        criteria: FolderCriteria,
    ): Flow<Outcome<List<FolderDetails>, FolderError>>

    suspend fun findById(
        accountId: AccountId,
        folderId: FolderId,
    ): Outcome<Folder?, FolderError>

    suspend fun getById(
        accountId: AccountId,
        folderId: FolderId,
    ): Outcome<Folder, FolderError>

    suspend fun findByServerId(
        accountId: AccountId,
        folderServerId: String,
    ): Outcome<Folder?, FolderError>
}

interface RemoteFolderRepository {
    suspend fun findAll(
        accountId: AccountId,
    ): Outcome<List<RemoteFolder>, FolderError>
}

interface FolderPushTrackingRepository {
    fun observeEnabled(
        accountId: AccountId,
    ): Flow<Outcome<Boolean, FolderError>>

    suspend fun disable(
        accountId: AccountId,
    ): Outcome<Unit, FolderError>
}
```

In `FolderCriteria`, `UnifiedInboxFilter.ANY` means that the query does not filter on unified inbox inclusion. The
criteria contains filters only. Scope identifiers such as `AccountId` and `FolderId` remain explicit parameters rather
than becoming part of the criteria.

For example, a screen that renders folder preferences depends only on `FolderSettingsRepository`. The implementation
may use any storage technology, as long as it maintains the contract.

## Testing repository consumers

Test consumers with fakes that implement the focused contract. Cover successful values, nullable absence, empty
collections, and each expected domain failure. Repository implementations must test account filtering and the mapping
of expected data-source failures to domain errors.
