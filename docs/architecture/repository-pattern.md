# Repository Pattern

This guide defines the repository API conventions used by the project. It supports
[ADR-0010](../engineering/adr/0010-adopt-project-wide-repository-pattern.md).

## Responsibility and ownership

A repository is a domain-facing contract that hides how data is read, stored, or synchronized. Its implementation may
coordinate local storage and remote services, but UI state and UI-specific business logic belong elsewhere.

Split contracts by responsibility before grouping them by aggregate. Do not create one interface per database table or
one interface per SQL operation. An optional aggregate-named facade may compose focused contracts, but it must not add
methods. A caller depends on the narrowest contract it needs.

An aggregate is a group of related domain data that changes together. A facade is an interface that combines focused
contracts for callers that need all of them.

Expose a repository contract from an `:api` module only when another area needs that stable contract. Keep
implementations, data sources, mappers, and storage details in `:internal`. Bind them in an application composition
module as required by [ADR-0009](../engineering/adr/0009-api-internal-split.md).

Place shared repository contracts, criteria, errors, and domain models in the area's `:api` module. Place repository
implementations, data sources, mappers, and storage models in its `:internal` module. Bind contracts to implementations
in `:app-common` or an app-specific composition module.

## Scope and identifiers

Make the identifiers that scope every operation explicit. For example, an operation on a folder within an account takes
both `AccountId` and `FolderId`. An account-scoped operation continues to take `AccountId`, even if a folder identifier
is also supplied. Do not create a repository instance that is permanently bound to one account, folder, or other entity.

Use domain-specific identifier types such as `AccountId` and `FolderId` in new contracts rather than raw primitives.
This makes the scope visible to callers and supports a consolidated database with one connection for all accounts.

## Asynchronous work and results

Repository I/O uses coroutines:

- A one-shot fallible operation is a `suspend fun` returning `Outcome<VALUE, ERROR>`.
- A reactive fallible operation returns `Flow<Outcome<VALUE, ERROR>>`.
- `ERROR` is a domain-specific sealed type. Do not expose database, HTTP, or platform exceptions from a contract.

Use [`net.thunderbird.core.outcome.Outcome`](../../core/outcome/src/commonMain/kotlin/net/thunderbird/core/outcome/Outcome.kt)
for repository results.

Use `Outcome.Success(Unit)` when an operation completes successfully and has no value to return. Use
`Outcome.Success(null)` when a lookup completes successfully but no matching entity exists. Use `Outcome.Failure(...)`
when the operation cannot produce the required result. An API that requires an entity represents a missing entity as a
domain failure. A private in-memory helper with no expected failure may return a concrete value directly.

## Read method naming

Use these names for read methods:

- Reactive reads:
  - `observeById()` reads one entity by the aggregate's main identifier.
  - `observeBy<Identifier>()` reads one entity by another identifier, such as `observeByServerId()`.
  - `observeAll()` reads all entities in scope.
  - `observeByCriteria()` reads at most one entity for an immutable, domain-specific criteria type.
  - `observeAllByCriteria()` reads multiple entities for an immutable, domain-specific criteria type.
- One-shot reads:
  - Optional singular reads use `findById()`, `findBy<Identifier>()`, or `findByCriteria()`. Return
    `Outcome.Success(null)` when no entity exists.
  - Collection reads use `findAll()` or `findAllByCriteria()`. Return `Outcome.Success(emptyList())` when no entities
    exist.
  - Required singular reads use `getById()`. Absence is represented as a failure.
  - Criteria methods accept an immutable, domain-specific criteria type.

Reactive reads use the same absence semantics as one-shot reads. A singular `Flow<Outcome<Type?, Error>>` emits
`Outcome.Success(null)` when absence is valid. A singular `Flow<Outcome<Type, Error>>` emits a domain failure when the
contract requires the entity. A collection `Flow<Outcome<List<Type>, Error>>` emits `Outcome.Success(emptyList())` when
no entities exist.

## Mutation methods

Use `create()`, `update()`, and `delete()` for aggregate lifecycle operations. Do not add a generic `save()` method to
new contracts. Use `clear<Scope>()` only for explicitly scoped bulk removal, such as `clearCache()`.

Within a focused repository contract, method names describe the operation and must not repeat the aggregate named by the
repository. For example, use `DraftRepository.create()`, `FolderQueryRepository.findById()`, and
`FolderQueryRepository.findByServerId()`, not `createDraft()`, `findByFolderId()`, or `findFolderByServerId()`. Use
`FolderQueryRepository.getById()` only for a required lookup by the main identifier.

Avoid repository methods whose names start with `set`. Multiple independent setters expose invalid intermediate state
and usually mean the API is acting as a mutable state holder. Represent values that change together as an immutable
aggregate and update them atomically. A setup draft or UI-only mutable state belongs in a `Store` or `StateHolder`, not
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

The following example starts with the focused contracts new code should depend on. The existing `FolderRepository`
legacy facade is shown last for callers that genuinely need every contract.

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

interface FolderRepository :
    FolderQueryRepository,
    RemoteFolderRepository,
    FolderPushTrackingRepository,
    FolderSettingsRepository
```

In `FolderCriteria`, `UnifiedInboxFilter.ANY` means that the query does not filter on unified inbox inclusion. The
criteria contains filters only. Scope identifiers such as `AccountId` and `FolderId` remain explicit parameters rather
than becoming part of the criteria.

For example, a screen that renders folder preferences depends only on `FolderSettingsRepository`, not on
`FolderRepository`. The implementation may use any storage technology, as long as it maintains the contract.

## Testing repository consumers

Test consumers with fakes that implement the focused contract. Cover successful values, nullable absence, empty
collections, and each expected domain failure. Repository implementations must test account filtering and the mapping
of expected data-source failures to domain errors.
