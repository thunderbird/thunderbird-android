# Technical Design: Global Database

- Issue: [#11293](https://github.com/thunderbird/thunderbird-android/issues/11293)
- RFC: [RFC 0007: Global Database](../rfcs/0007-global-database.md)
- Repository pattern: [ADR 0010 proposal](https://github.com/thunderbird/thunderbird-android/pull/11452)
- Portable data format: [RFC 0008: Portable Profile Data Format](../rfcs/0008-portable-profile-data-format.md)
- Status: **Proposed**

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

|   Identifier   |         Owner         |                                                                                                                                            Meaning and boundary                                                                                                                                             |
|----------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AccountId`    | `feature:account:api` | The existing UUID-backed account identifier. Global-mail records retain its existing persisted value to establish account scope. Cutover neither replaces nor regenerates it. Account settings and profile runtime state remain outside the global mail database.                                           |
| `FolderId`     | Mail domain           | Identifies one local folder record across all accounts. It replaces the legacy account-local folder number at repository boundaries.                                                                                                                                                                        |
| `MessageId`    | Mail domain           | Identifies one local message record across all accounts. A message copied to another folder is a separate local record and therefore has a separate `MessageId`.                                                                                                                                            |
| `ThreadId`     | Mail domain           | Identifies one account-scoped conversation, which can contain local message records from Inbox, Sent, Archive, and other folders. It is a durable conversation aggregate, not a legacy numeric thread-root key. Thread operations use `ThreadId`. Operations on an individual local record use `MessageId`. |
| `AttachmentId` | Mail domain           | An opaque attachment-access URI or equivalent reference. It resolves unambiguously after cutover and is not a raw message-part primary key. Message-part keys stay internal unless a future focused attachment contract requires one.                                                                       |

RFC 0009 owns the UUID representation and generation policy for `FolderId`, `MessageId`, and `ThreadId`. These types,
along with `AccountId` and `AttachmentId`, are the only identifiers that cross the mail repository boundary for this
design. Repository contracts never expose legacy numeric IDs or persistence keys.

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

| Logical record  |                     Source key                      | Target domain identifier |                                            Required dependent rewrite                                             |
|-----------------|-----------------------------------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------------|
| Folder          | `(AccountId, legacy folder id)`                     | `FolderId`               | folder extra values, message folder references, queued-command folder references                                  |
| Message         | `(AccountId, legacy message id)`                    | `MessageId`              | outbox state, notifications, full-text mapping, message part root, thread message reference                       |
| Thread          | `(AccountId, legacy folder id, legacy thread root)` | `ThreadId`               | rebuild cross-folder memberships from message threading headers and rewrite threaded-list and thread-cache values |
| Message part    | `(AccountId, legacy message-part id)`               | internal only            | part root and parent references, message root-part reference, attachment-file lookup and URI resolution           |
| Pending command | `(AccountId, legacy command id)`                    | internal only            | command row identity and serialized folder references                                                             |

The exact physical representation is internal, but it must enforce uniqueness of every source key and reject an import
that maps one source key to multiple targets. Several legacy folder-local thread roots may map to one `ThreadId` when
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

1. Create and verify the required RFC 0008 archive for POP3 accounts. The user may decline it after an explicit warning
   and continue at their own risk. IMAP export is optional.
2. Check available storage against the required headroom and fail with an actionable error when it is insufficient.
   Then create an unpublished global database.
3. Read legacy databases and attachment directories without modifying them. Import every durable record.
4. Copy each attachment to its target and validate it. If validation fails, record the failure in the migration result
   and fail the migration.
5. Rebuild derived data, validate the imported database, and reopen it.
6. Establish the durable cutover state and switch repository bindings to the global implementation.
7. Remove confirmed legacy database and attachment artifacts.

The import writes folder and message identifier mappings before importing dependents. After all message records for an
account are available, it builds cross-folder `ThreadId` memberships from their threading headers and records the
legacy folder-local thread-root mappings before rewriting threaded-list and cache values. It then validates all
rewritten relationships before the global database is published. It translates serialized pending-command folder
references to `FolderId` or to the chosen internal mapping before the command is eligible to execute. It also accepts
supported pre-cutover message references, such as notification or activity references containing `(AccountId,
legacyFolderId, UID)`, through the compatibility mapping until that support is intentionally retired. Newly created
references use the global identifier model.

Before step 6, global data is not visible to normal mail code. Any failure before that step keeps legacy storage
authoritative. A later retry starts with a new unpublished import. A cleanup failure after cutover leaves global storage
authoritative and records the remaining cleanup work.

The legacy storage implementation stays in the codebase, unbound and unused, and a later release removes it.

## Validation

Cutover requires all of the following:

- every configured account was imported
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
- validation, interruption, retry, and cleanup failure
- migration-gate behavior for startup and background work
- report redaction
- the guarantee that partial global data is never visible to normal callers
- identifier-mapping completeness, duplicate-key rejection, pending-command payload translation, attachment URI
  resolution, and notification-ID collision handling
- cross-folder thread construction, including a conversation spanning Inbox and Sent and a merge of two existing
  conversations

## Open technical questions

- Which supported legacy schema versions require dedicated fixtures?
- Which Room driver and locations apply on Android and JVM desktop?
- How should post-cutover cleanup retry a locked legacy artifact?

