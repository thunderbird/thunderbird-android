# RFC 0007: Global Database

- Issue: [#11104](https://github.com/thunderbird/thunderbird-android/issues/11104)
- Technical design: [Global Database](../technical-designs/0003-global-database.md)
- Related RFC: [UUIDv7 Identifier Migration](0009-uuidv7-identifier-migration.md)
- Repository pattern: [ADR 0010 proposal](https://github.com/thunderbird/thunderbird-android/pull/11452)
- Portable data format: [RFC 0008: Portable Profile Data Format](0008-portable-profile-data-format.md)
- Status: **Proposed**
- Backend decision: **Room 3 selected by the
  [completed database spike](https://github.com/thunderbird/thunderbird-android/issues/11195)**

## Summary

Global Database replaces the legacy per-account mail stores with one Room 3-backed mail database for the application. It
preserves existing mail behavior and moves production mail access to the global database only after a complete migration
succeeds.

The migration imports durable mail data and downloaded attachments, validates the result, establishes the durable
cutover state, and removes legacy storage. If migration fails before cutover, legacy storage remains authoritative.

## Motivation

Legacy mail storage uses one database and one attachment directory per account. This makes unified mail behavior more
complex and exposes legacy storage types to mail code.

One global database provides a single local source of truth while keeping callers independent of storage
implementation. The migration must preserve durable mail data without exposing a partial or mixed store.

## Proposal

### Storage boundaries

- `core:database` will provide the domain-neutral Room 3 backend.
- `core:database` owns one coordinated schema and migration history, and composes feature schema contributions
  deterministically.
- The internal mail database module owns mail schema and storage implementations.
- Focused repository contracts are the boundary between mail code and storage. They follow the proposed ADR 0010 and
  do not expose persistence or legacy storage types.
- Application composition selects the active repository implementation.

### Global mail representation

The global database keeps the legacy mail-store behavior and schema shape. It makes account scope explicit and adds
global identifiers only where repository contracts require them. Refining the schema is deferred to a future iteration.

Every durable legacy mail record, queued operation, and downloaded attachment is included. Derived data, such as search
indexes, is rebuilt as part of the migration. The technical design owns the table-by-table migration inventory that
records which legacy tables are copied, rebuilt, or excluded.

### Global identifiers

In this RFC, "global" means unique across the mail records of all accounts in one application profile. It does not mean
an identifier shared across devices or provided by a mail server. In particular, IMAP UIDs, server folder IDs, and RFC
5322 `Message-ID` headers retain their protocol-specific scopes and are not global database identifiers.

The global store uses these identifiers at repository boundaries:

- `AccountId` is the existing UUID-backed account identifier. The global mail database uses it to retain account scope
  and does not replace or regenerate it during cutover.
- `FolderId` identifies one local folder record across all accounts, replacing the legacy account-local folder number.
- `MessageId` identifies one local message record across all accounts. Copies of a message in different folders are
  separate local records and have separate identifiers.
- `ThreadId` identifies a conversation within one account. A conversation can contain message records from multiple
  folders, such as Inbox and Sent.
- `AttachmentId` is an opaque reference used to access an attachment. It is not a raw message-part database key.

These domain identifiers are stable for the lifetime of their local records. Repository contracts do not expose legacy
numeric IDs or persistence keys. The database may still use internal surrogate keys where required. The technical design
defines identifier ownership and migration mappings. RFC 0009 separately defines UUID representation and generation
policy.

### Migration and cutover

The migration has one authoritative mail store at a time:

1. For every POP3 account, create and verify the portable mail archive defined by RFC 0008. A user may decline the
   archive after an explicit warning and continue at their own risk. For IMAP accounts, offer the same archive as an
   optional action.
2. Check available storage against the required headroom. Fail with an actionable error when it is insufficient.
3. Read legacy storage without modifying it. Import durable data into an unpublished global database.
4. Rebuild derived data, then validate the imported database, queued operations, attachments, and representative search
   queries.
5. Set the durable cutover state and switch repository bindings to the global implementation.
6. Remove all legacy database and attachment artifacts, including data left behind by previously deleted accounts.

Before cutover, legacy storage remains authoritative. After cutover, global storage remains authoritative. There are no
dual reads, dual writes, or fallback to legacy storage.

Failures before cutover leave legacy storage authoritative and report an actionable error locally. The user can retry
the migration without losing access to the legacy data. An attachment that fails to copy or validate is recorded in the
migration result and fails the migration. Insufficient free space is the expected cause.

After successful cutover, cleanup removes all legacy mail data, including artifacts left behind by previously deleted
accounts. A cleanup failure does not block normal use of the global store or make legacy storage authoritative again.
The technical design defines cleanup state, retries, and user feedback. The legacy storage implementation stays in the
codebase, unbound and unused, until a later release removes it.

### Migration gate and reporting

The migration surfaces progress, failure feedback, retry, and local report export in a dedicated migration screen. That
screen could reuse the existing Android database-migration activity or introduce a suitable replacement. Startup
routing and background mail work respect the migration gate. No mail work bypasses it.

Progress information, failure reports, and logs exclude personally identifiable data. They stay on the device and are
never uploaded.

### Platform support

The global mail schema, repository contracts, and attachment access are KMP-ready for Android and JVM desktop. Legacy
import is Android-only.

### Not included

This RFC does not:

- redesign or normalize the legacy mail schema or queue behavior
- move account settings or profile runtime storage into the global database
- introduce profiles or synchronization
- define portable-data or backup formats. RFC 0008 owns that work.
- select or migrate UUID identifier formats. RFC 0009 owns that work
- add remote synchronization, telemetry, or remote migration reporting
- add iOS or Web persistence support

## Alternatives Considered

### Keep the legacy stores

This retains account-specific storage and prevents establishing a single local source of truth for mail data.

### Use SQLDelight

The completed database spike compared Room 3 and SQLDelight against the same representative mail, queue, and full-text
search slice. Both candidates met the architectural baseline, but Room 3 delivered stronger response-time and
concurrent-write results.

### Redesign the schema during migration

Combining a schema redesign with migration would increase risk and make behavior parity harder to verify.

### Retain legacy storage after cutover

Keeping the legacy artifacts would preserve a local recovery path for a defect found after cutover. It would also store
mail data twice for as long as it is retained, and no code path would read it, because cutover permits no fallback.
Validation before cutover and the archive from step 1 cover that risk instead.

## Risks & Drawbacks

- Migration requires time and storage, especially for downloaded attachments.
- Rebuilding the search index during migration adds time proportional to locally stored mail.
- A device without enough free space cannot migrate until space is freed.
- After cutover, locally stored mail exists only in the global database and in any archive the user created.
- The legacy storage implementation stays in the codebase until a later release removes it.
- A POP3 archive adds a user-visible prerequisite before migration.
- Replacing legacy storage access is broad work.
- The legacy schema retains existing constraints until a later change addresses them.

## Open Questions

- Should migration require a pre-flight free-space check with a stated minimum headroom, and should a failed check block
  migration or only warn?
- Does declining the POP3 archive require a durable record of the user's acknowledgement?

The technical design owns implementation questions, including driver configuration, the identifier mapping inventory,
the migration screen, source-schema support, and the cleanup retry strategy.

## Outcome

Filled in when the RFC is accepted, rejected, or obsolete.

Summarize the final decision and link follow-up work.
