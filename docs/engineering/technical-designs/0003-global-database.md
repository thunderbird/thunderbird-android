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

## Open technical questions

- Which supported legacy schema versions require dedicated fixtures?
- Which Room driver and locations apply on Android and JVM desktop?
- Which global identifier representation and mapping retention period are required?
- How should post-cutover cleanup retry a locked legacy artifact?

