# Technical Design: Global Database

- Issue: [#11293](https://github.com/thunderbird/thunderbird-android/issues/11293)
- RFC: [RFC 0007: Global Database](../rfcs/0007-global-database.md)
- Related RFC: [UUIDv7 Identifier Migration](../rfcs/0009-uuidv7-identifier-migration.md)
- Repository pattern: [ADR 0010 proposal](https://github.com/thunderbird/thunderbird-android/pull/11452)
- Portable data format: [RFC 0008: Portable Profile Data Format](../rfcs/0008-portable-profile-data-format.md)
- Status: **Accepted**

## Summary

Global Database replaces one legacy mail database per account with one Room 3-backed database for the
application. It keeps legacy mail behavior compatible, imports durable data, validates the result, and cuts over once.

## Boundaries

- `core:database` provides the domain-neutral Room 3 backend, lifecycle, transactions, migrations, and platform
  support. It composes feature schema contributions deterministically and owns one coordinated schema and migration
  history.
- The internal mail database module owns mail schema, mappings, local data sources, and repository implementations.
- Focused mail repository contracts hide both legacy and global storage from callers.
- `app-common` or an app module binds repository contracts to the active implementation.

The boundary follows ADR 0009. Mail schema and implementation types remain internal. `core:database` does not own mail
schema, mail domain types, or mail repositories.

## Global mail store

The global database keeps the legacy mail-store schema and behavior as its starting point. It stores all accounts in one
database, keeps account-qualified legacy references, and adds global identifiers only where callers need them.

The [legacy table inventory](0003-global-database/legacy-table-inventory.md) defines the exact mapping. It includes
downloaded attachments. Legacy keeps a body part on disk above a size threshold and as a `message_parts` BLOB at or
below it. The migration preserves that split rather than changing where content lives.

### Identifier model

Global identifiers are opaque, application-profile-scoped domain values. They identify local records in the global
database and are stable while that record exists, including across application restart and database-schema upgrades.
They are not protocol identifiers, portable-profile identifiers, or synchronization identifiers. In particular, an
IMAP UID, a folder server ID, and an RFC 5322 `Message-ID` header must not be used as a global database identifier.

|   Identifier   |           Owner            |                                                                                                                                            Meaning and boundary                                                                                                                                             |
|----------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AccountId`    | `feature:account:api`      | The UUID-backed account identifier. Cutover replaces each existing real account ID with UUIDv7 and uses it to establish account scope. Account settings and profile runtime state remain outside the global mail database, with persisted account references migrated to the replacement.                   |
| `FolderId`     | `feature:mail:folder:api`  | Identifies one local folder record across all accounts. It replaces the legacy account-local folder number at repository boundaries.                                                                                                                                                                        |
| `MessageId`    | `feature:mail:message:api` | Identifies one local message record across all accounts. A message copied to another folder is a separate local record and therefore has a separate `MessageId`.                                                                                                                                            |
| `ThreadId`     | `feature:mail:message:api` | Identifies one account-scoped conversation, which can contain local message records from Inbox, Sent, Archive, and other folders. It is a durable conversation aggregate, not a legacy numeric thread-root key. Thread operations use `ThreadId`. Operations on an individual local record use `MessageId`. |
| `AttachmentId` | `feature:mail:message:api` | Identifies one persisted attachment record across all accounts. It is distinct from attachment access URIs and internal message-part primary keys.                                                                                                                                                          |

RFC 0009 owns the UUID representation and generation policy for `FolderId`, `MessageId`, `ThreadId`, and `AttachmentId`.
These types, along with `AccountId`, are the only identifiers that cross the mail repository boundary for this
design. Repository contracts never expose legacy numeric IDs or persistence keys. Replacing an existing `AccountId`
during this one-time cutover is the only exception to its lifetime stability.

The thread builder uses the imported messages' threading headers across all folders of the same account. When a newly
observed message joins two conversations, the builder selects one existing `ThreadId` deterministically, merges the
memberships, and rewrites the other conversation's internal references in the same transaction. A `ThreadId` is not a
protocol identifier and is not used for server operations.

Room entities may have a separate persistence-local integer surrogate key. Those keys preserve compatible relationships
and support SQLite features such as FTS, and never appear in a repository contract. In particular, FTS4 `docid` remains
an integer internal search-document key mapped to a `MessageId`. A UUID-backed `MessageId` is never stored directly as
`docid`.

### Identifier mapping and internal keys

For imported data, the migrator records an account-qualified source key before linking dependent rows:

| Logical record  |                         Source key                         | Target domain identifier |                                            Required dependent rewrite                                             |
|-----------------|------------------------------------------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------------|
| Account         | legacy `AccountId`                                         | UUIDv7 `AccountId`       | account settings namespace, account order, mail rows, queues, notifications, activity references, and widgets     |
| Folder          | `(legacy AccountId, legacy folder id)`                     | `FolderId`               | folder extra values, message folder references, queued-command folder references                                  |
| Message         | `(legacy AccountId, legacy message id)`                    | `MessageId`              | outbox state, notifications, full-text mapping, message part root, thread message reference                       |
| Thread          | `(legacy AccountId, legacy folder id, legacy thread root)` | `ThreadId`               | rebuild cross-folder memberships from message threading headers and rewrite threaded-list and thread-cache values |
| Message part    | `(legacy AccountId, legacy message-part id)`               | internal only            | part root and parent references, message root-part reference                                                      |
| Attachment      | `(legacy AccountId, legacy message-part id)`               | `AttachmentId`           | attachment-file lookup and access URI resolution                                                                  |
| Pending command | `(legacy AccountId, legacy command id)`                    | internal only            | command row identity and serialized folder references                                                             |

The account mapping is generated once and stored in the durable migration state before account data is copied. Retries
reuse it. The exact physical representation is internal, but it must enforce uniqueness of every source key and reject
an import that maps one source key to multiple targets. Several legacy folder-local thread roots may map to one `ThreadId` when
the imported messages form one cross-folder conversation. Account-qualified mappings remain available until validation
has completed and all durable queue, notification, attachment, and supported legacy external references have been
translated. The implementation may retain them longer for compatibility. Their retention period and removal test are
part of the migration implementation plan.

`notifications.notification_id` is an Android application notification ID, not a mail-domain identifier. It must remain
unique across the application notification namespace. Import validates that constraint and allocates a replacement when
preserving a legacy value would collide. The notification's relationship to its message is rewritten through
`MessageId` and the internal message key.

## Migration

The migration runs before normal mail access is available. A dedicated migration screen, which could reuse the existing
Android migration activity or introduce a suitable replacement, shows non-sensitive progress and a clear completion or
failure state. A migration gate holds startup, sync, and other background mail work until migration completes or fails.

1. Calculate the required storage headroom and compare it with available space. If space is insufficient, block the
   migration and report how much additional space is needed.
2. Create and verify the required RFC 0008 archive for POP3 accounts. The user may decline it after an explicit warning
   and continue at their own risk. IMAP export is optional. Then create an unpublished global database.
3. Generate and durably record one UUIDv7 replacement for each existing real `AccountId`. Copy all keys from each old
   account settings namespace to its new namespace, retaining the old keys. Record the account's previous `enabled`
   value and force the copied account to remain disabled. Do not publish the new account list yet.
4. Read legacy databases and attachment directories without modifying them. Import every durable record using the
   account mapping.
5. Copy each attachment to its target and validate it. If validation fails, record the failure in the migration result
   and fail the migration.
6. Rebuild derived data, validate the imported database and account settings, and reopen the database.
7. Enter the durable cutover phase, replace old IDs in the ordered `accountUuids` value, restore each account's previous
   `enabled` value, establish the global store as authoritative, and switch repository bindings to the global
   implementation.
8. Start post-cutover cleanup of old account settings and all legacy database and attachment artifacts.

The import writes folder, message, and attachment identifier mappings before importing dependents. After all message records for an
account are available, it builds cross-folder `ThreadId` memberships from their threading headers and records the
legacy folder-local thread-root mappings before rewriting threaded-list and cache values. It then validates all
rewritten relationships before the global database is published. It translates serialized pending-command folder
references to `FolderId` or to the chosen internal mapping before the command is eligible to execute. It also accepts
supported pre-cutover message references, such as notification or activity references containing `(AccountId,
legacyFolderId, UID)`, through the compatibility mapping until that support is intentionally retired. Newly created
references use the global identifier model.

Before step 7, global data and copied settings are not visible to normal account or mail code. Any failure before that
step keeps the old account list, old settings, and legacy mail storage authoritative. A later retry starts with a new
unpublished import and reuses the durable account mapping.

### Pre-flight storage check

The pre-flight check runs as the first foreground migration phase, before creating the unpublished database. It
inventories the legacy databases and file-backed attachments, then calculates the additional space needed while legacy
and global storage coexist. Required headroom includes the estimated global database, copied attachments, rebuilt search
data, SQLite transaction and temporary-file growth, and a safety margin. If an RFC 0008 archive is written to the same
volume, its estimated size is also included.

Migration starts only when available space on the target volume meets the calculated headroom. If it does not, the
migration screen shows the required, available, and additional space in user-readable units and provides a retry action.
A warning with an option to continue is not sufficient because running out of space makes successful migration
impossible.

Available space can change after pre-flight. Write failures caused by exhausted storage fail the unpublished import
without changing the authoritative store. Restart recovery removes the incomplete target before another pre-flight check
and retry. The estimator and safety margin are verified against migration fixtures representing the largest supported
legacy schema and attachment layouts.

### Android execution and progress

The user starts migration from the visible migration screen. Android then runs the migration orchestration in a
foreground service with the `dataSync` service type. The service owns the operation independently of the activity and
continues when the app is backgrounded or its task is dismissed. It enters the foreground immediately and keeps an
ongoing notification visible while migration is running.

The service writes non-sensitive phase, progress, completion, and failure state to one durable migration-state source.
The migration screen observes that source and updates continuously while visible. The foreground notification reads the
same state, displays current progress, and opens the migration screen when tapped. When migration finishes, the service
stops foreground execution and posts a completion or actionable failure notification.

The Android application declares only the foreground service permissions required for a user-initiated `dataSync`
operation. Starting the service must comply with the platform's background-start restrictions. Platform time limits,
system process termination, and an explicit user force-stop can still interrupt it. The service therefore improves
continuity but does not replace the recovery guarantees below. After a force-stop, work cannot continue until Android
allows the app to run again.

### Interruption and recovery

The migration must tolerate process termination at every step, including termination after the app is backgrounded. The
import database and copied attachments remain unpublished until validation and cutover complete. Each startup reads the
durable migration phase before binding mail repositories.

If the process stopped before cutover, startup keeps legacy storage authoritative, removes the incomplete unpublished
database and copied artifacts, and starts a fresh import when the migration is retried. SQLite transactions protect
individual writes, while discarding the unpublished target prevents a partially imported database from being reused.

Cutover is an idempotent durable phase guarded from normal account and mail access. It publishes the new `accountUuids`
value and global-store state using the previously recorded mapping. If the process stops between those writes, startup
observes the in-progress cutover phase and completes it before releasing the migration gate. Old settings and legacy
mail storage cannot be deleted before cutover completion is durable.

### Post-cutover cleanup

Completed cutover makes the global store and new account IDs authoritative and records cleanup as pending. Only then can
an idempotent cleanup job delete the old account setting namespaces, legacy per-account databases, and attachment
directories. The job also deletes orphaned artifacts left behind by accounts that were removed before migration.

Cleanup is complete only after the job verifies that all known legacy artifacts are absent. If deletion fails or the app
stops during cleanup, the state remains pending and the job retries on a later startup or scheduled background run. A
retry resumes cleanup without repeating the data migration. The job never reads legacy mail into the global store and
mail repositories never bind to legacy storage after cutover.

A pending cleanup does not block normal mail access. The migration is reported as successful, with a separate notice
that storage cleanup is incomplete and may temporarily use additional device storage. Cleanup failures and retry state
use non-sensitive error codes and do not include paths or account data.

The app does not restore legacy storage after cutover because the global store may already contain new mail or user
actions. Switching back could discard those changes or create conflicting sources of truth. The legacy storage
implementation stays in the codebase, unbound and unused, until a later release removes it.

## Validation

Cutover requires all of the following:

- every configured account was imported
- every real account has exactly one UUIDv7 replacement and retry preserves the same mapping
- all settings from each old account namespace exist under the replacement, account ordering is preserved, and each
  account's previous enabled state is restored
- mapped data and relationships are complete
- queued operations survive restart and remain executable
- file-backed attachments, meaning parts with `data_location = 2`, are present and valid
- database integrity checks pass after reopen
- search is rebuilt and representative queries match
- every source key maps to exactly one target, all dependent keys resolve, and no internal legacy numeric key crosses a
  repository boundary
- messages from Inbox, Sent, and other folders join the same `ThreadId` when their threading headers identify one
  conversation, without joining messages from different accounts
- attachment URIs resolve to the same imported part after restart
- notification IDs are unique in the application namespace after import
- the durable cutover state remains readable after reopen

## Diagnostics

Migration reports contain only non-sensitive progress, counts, status, and stable error codes. They do not contain mail
content, addresses, attachment names, paths, credentials, or tokens. Reports stay local and can be exported by the
user.

## Platform support

The global database, repository contracts, and attachment access support Android and JVM desktop. Legacy storage
reading and import are Android-only.

## Testing

Automated tests cover:

- repository behavior before and after cutover
- schema creation, migration, and restart recovery
- every durable table and attachment type in the inventory
- POP3 archive gating and IMAP optional export
- headroom calculation, insufficient-space blocking, and user-visible required-space reporting
- storage exhaustion after pre-flight without publishing the incomplete target
- migration continuing after the activity is backgrounded or its task is dismissed
- consistency between in-app progress, foreground notification progress, and the durable migration state
- foreground service completion, failure, platform timeout, and user-stop handling
- process termination during every migration phase, including immediately before and after durable cutover
- rejection and removal of an incomplete unpublished database after restart
- validation, interruption, retry, and cleanup failure
- cleanup of orphaned account artifacts, restart-safe retries, and completion verification
- migration-gate behavior for startup and background work
- report redaction
- the guarantee that partial global data is never visible to normal callers
- identifier-mapping completeness, duplicate-key rejection, stable account mapping across retries, account settings
  namespace copying, disabled staging, enabled-state restoration, ordered account-list cutover, old-settings cleanup,
  pending-command payload translation, attachment URI resolution, and notification-ID collision handling
- cross-folder thread construction, including a conversation spanning Inbox and Sent and a merge of two existing
  conversations

## Open technical questions

- Which supported legacy schema versions require dedicated fixtures?
- Which Room driver and locations apply on Android and JVM desktop?
- How long must compatibility mappings support pre-cutover external message references after migration?

