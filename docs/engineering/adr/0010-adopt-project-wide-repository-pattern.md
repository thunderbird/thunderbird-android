# ADR 0010: Adopt project-wide Repository pattern

- Issue: [#11440](https://github.com/thunderbird/thunderbird-android/issues/11440)
- Status: **Proposed**

## Context

One of our main pain points in the app is how we consume the data. The current implementation consists of one
database per account, which creates several challenges such as not being able to properly paginate the unified
inbox, or having unified folders.

Besides, the access to the data is spread across the codebase, with a lot of coupled business logic, making it hard
to maintain and impossible to scale.

## Decision

To remove the business logic and data access coupling, while allowing us to migrate to a new — consolidated — database,
we decided to adopt the Repository pattern project-wide, where each Repository, in order of priority:

1. has one reason to change (non-negotiable)
2. is agnostic of how it consumes the data
3. is KMP-safe (no `android.*`, no `Cursor`)
4. has an `accountId` as an explicit call parameter everywhere — never tied to a per-account instance — since
   that's the precondition for both DB consolidation and for a Room/SQLDelight implementation that owns one connection
   for all accounts.

### Repository API definition

When creating a new Repository you must follow this principle: **split by responsibility first, aggregate second**.

This differs both from today's file-level split by SQL operation type (Retrieve/Save/Delete/Flag) and from "one
interface per table", which produced `FolderRepository`'s current god-interface problem. Each aggregate instead gets a
small family of interfaces:

```kotlin
/** 
 * Composing facade only; no methods of its own, just groups the interfaces 
 * below for call sites that need all of them 
 **/
interface FolderRepository : FolderQueryRepository, RemoteFolderRepository, FolderPushTrackingRepository,
    FolderSettingsRepository

/** folders as seen locally **/
interface FolderQueryRepository {
    suspend fun getFolder(accountId: AccountId, folderId: Long): Folder?
    suspend fun getFolderDetails(accountId: AccountId, folderId: Long): FolderDetails?
    suspend fun getFolders(accountId: AccountId, excludeLocalOnly: Boolean): List<FolderDetails>
    suspend fun getFolderServerId(accountId: AccountId, folderId: Long): String?
    suspend fun getFolderId(accountId: AccountId, folderServerId: String): Long?
    suspend fun isFolderPresent(accountId: AccountId, folderId: Long): Boolean
}

/** folders as seen on the server — distinct concern from local state **/
interface RemoteFolderRepository {
    suspend fun getRemoteFolders(accountId: AccountId): List<RemoteFolder>
    suspend fun getRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails>
}

/** push-sync-specific, changes for different reasons than folder CRUD **/
interface FolderPushTrackingRepository {
    fun getPushFoldersFlow(accountId: AccountId): Flow<List<RemoteFolder>>
    suspend fun getPushFolders(accountId: AccountId): List<RemoteFolder>
    suspend fun setPushDisabled(accountId: AccountId)
    suspend fun hasPushEnabledFolder(accountId: AccountId): Boolean
    fun hasPushEnabledFolderFlow(accountId: AccountId): Flow<Boolean>
}

/** the five per-flag setters, one cohesive "user changed a folder setting" concern **/
interface FolderSettingsRepository {
    suspend fun updateFolderDetails(accountId: AccountId, folderDetails: FolderDetails)
    suspend fun setIncludeInUnifiedInbox(accountId: AccountId, folderId: Long, includeInUnifiedInbox: Boolean)
    suspend fun setVisible(accountId: AccountId, folderId: Long, visible: Boolean)
    suspend fun setSyncEnabled(accountId: AccountId, folderId: Long, enable: Boolean)
    suspend fun setNotificationsEnabled(accountId: AccountId, folderId: Long, enable: Boolean)
}
```

When defining the Repository functions, you must always use Coroutines. Rule of thumb:

- Is the function consumed once? Use `suspend fun` and return the concrete data model
- Is the function consumed reactively? Use `fun` and return a `Flow` of the concrete data model

Ideally, a repository should pick either one-time or reactive consumption; however, there are scenarios where that
isn't possible, as displayed above. In that case, the method that returns a `Flow` **must** be suffixed with `Flow`.

If reactive consumption is the only usage needed for a given function, the suffix isn't required.

## Outcomes

### Positive Outcomes

- Business logic is decoupled from the data layer, so the underlying data source can change without touching call sites
- Splitting repositories by responsibility, not by table, avoids reproducing the god-interface problem
  `FolderRepository` already has today
- Explicit `accountId` per call is the precondition for both database consolidation and a KMP-safe implementation

### Negative Outcomes

- The change will touch many code surfaces
  - Fetch operations (~45 sites)
  - Mutate operations (~45 sites)
  - 27 non-test files depend on `LocalStore` directly
- Requires a phased migration, so old and new data-access paths coexist for a while
- `account_id` filtering has to be added to every query before consolidation is safe; a missed predicate fails
  silently, not loudly

