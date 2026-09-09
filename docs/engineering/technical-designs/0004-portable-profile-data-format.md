# Technical Design: Portable Profile Data Format

- Related milestone: [Global Database #10374](https://github.com/thunderbird/thunderbird-android/issues/10374)
- RFC: [RFC 0008: Portable Profile Data Format](../rfcs/0008-portable-profile-data-format.md)
- Format architecture: [Portable Profile Data Format](../../architecture/portable-profile-data-format.md)
- Standards profile: [Portable Profile Data Standards](../../standards/portable-profile-data.md)
- Mail archive compatibility target: [draft-ietf-mailmaint-pdparchive-01: Personal Data Portability Archive](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-01)
- Encryption format: [WinZip AES](https://www.winzip.com/en/support/aes-encryption/)
- Status: **Proposed**

## Summary

This design defines the archive layout, Thunderbird settings and profile record shape, import behavior, and encrypted
full-backup representation for RFC 0008. It creates a portable representation without adding synchronization or moving
current runtime settings storage.

## Current State

Mail is stored in legacy per-account databases and attachments directories pending Global Database cutover. Account
profiles already have domain data such as a stable account ID, name, color, and avatar, but the account and settings
storage remains legacy-backed. The current settings importer is Android-specific and imports existing settings formats.
It is not a durable, versioned profile-data contract.

Account activation currently obtains credentials after settings import. This behavior remains: credentials and OAuth
tokens are not exported and imported accounts prompt for authentication when required.

## Proposed Design

### Archive forms

Every portable profile-data export is a password-protected ZIP64 archive using AES-256 and the
[WinZip AES encryption specification](https://www.winzip.com/en/support/aes-encryption/). Legacy ZipCrypto is not
allowed. In a Thunderbird profile export, each included mail account is a self-contained raw PDPArchive account archive
stored directly under its account directory. In a standalone PDPArchive export, `archive.json` and `mail/` are at the
ZIP64 root. The app records the exact PDPArchive draft revision in the account metadata and, when present, the
Thunderbird manifest. An account archive does not depend on Thunderbird settings and can be imported independently after
decryption and extraction.

PDPArchive `-01` defines a raw file layout but does not select a container format or encryption mechanism. Encrypted
ZIP64 is Thunderbird's convention until the standard settles those concerns. The complete multi-account archive is a
Thunderbird format that contains PDPArchives. It is not represented as one PDPArchive.

Android, JVM, iOS, and web use the same encrypted ZIP64 representation through a common Kotlin Multiplatform boundary
backed by maintained target libraries. Export writes directly to an incomplete output file and publishes it only after
the archive closes successfully. Import decrypts and validates the selected entries before changing runtime repositories.
An authentication failure imports nothing.

Portable exports and full backups use the same encrypted ZIP64 representation. They differ in selected content and user
warning. An export may contain selected mail, settings/profile data, or both. A full backup contains the complete
selected portable profile-data bundle.

Individual-message and selected-folder EML export are separate mail-export flows. They do not write this bundle, use
its import path, or satisfy the POP3 migration prerequisite.

### Bundle layout

The envelope and each account archive have separate format boundaries:

```text
manifest.json
accounts/
  <safe-account-label>--<portable-account-id>/
    archive.json
    mail/
thunderbird/
  profile.json
  settings/global.json
  accounts/<portable-account-id>/profile.json
  accounts/<portable-account-id>/configuration.json
  accounts/<portable-account-id>/identities.json
  accounts/<portable-account-id>/folder-settings.json
```

Each account directory is a self-contained raw PDPArchive account archive. A standalone PDPArchive export has the same
`archive.json` and `mail/` layout at the decrypted payload root.

The manifest records envelope version, generator, account-archive paths, and the exact PDPArchive revision.
Each PDPArchive `archive.json` contains one opaque source account identifier. This one-archive-per-account boundary
avoids folder collisions and allows generic importers to consume account archives without understanding Thunderbird
records. Account paths may include sanitized human-readable labels for manual identification after decryption. Labels
are display-only and the portable account ID remains authoritative.

`profile.json` identifies the exported Thunderbird profile and archive selection. `settings/global.json` holds portable
global user preferences. Account records contain portable account configuration and profile data in a new format rather
than legacy preferences, database values, or serialized implementation classes. Every included account is a full account export with `dataset.extent` set to `FULL`. Account selection is recorded for
Thunderbird UI but does not change the full extent of each included account archive. Partial, filtered, and incremental
account archives are unsupported and deferred to the future synchronization project.

### Record shape and data boundaries

Every Thunderbird-owned JSON record uses this logical shape:

```json
{
  "schema": "net.thunderbird.portable-profile-data",
  "schemaVersion": 1,
  "recordId": "opaque-stable-id",
  "payload": {}
}
```

The portable account ID is stable across export/import and is distinct from the application-profile-scoped `AccountId`
used by the Global Database. Portable record identifiers are also distinct from local `FolderId`, `MessageId`, and
`ThreadId` values. Stable portable folder and message identifiers are reused across repeated exports. Import maps a
portable account ID to a user-selected existing account or allocates a new local `AccountId`. Existing local identifiers
are never rewritten. Import never treats a legacy database ID, account number, row ID, or local mail-domain identifier
as portable identity.

The current format does not define revision ordering, causal history, tombstone propagation, automatic merge, or
synchronization conflict resolution. A future synchronization project must define and review any additional metadata it
requires rather than assigning new semantics to the version 1 fields.

Payloads use typed fields and explicit enum values. Unknown fields are preserved when possible and otherwise ignored
without discarding known fields. Unknown required schema versions stop the affected record import with an actionable
local error. Importers migrate every older format and PDPArchive draft revision emitted by the app.

Portable records include only user-controlled, cross-device-meaningful data:

- global behavior and display choices that are safe to carry between devices.
- account profile, identities, server endpoint and security configuration, protocol options, sync choices, and folder
  choices.
- account and profile ordering where it remains meaningful after import.

They exclude credentials, OAuth tokens, certificate private keys, device permissions, notification channels,
device-local UI state, transient caches, queued operations, internal database state, migration state, and telemetry.
The detailed portable-settings inventory is an acceptance artifact of the implementation work.

### Import behavior

Import first validates the container or authenticated encryption envelope, then parses all records without changing runtime
storage. It presents a selection and conflict preview before applying user-selected records. Account credentials are
requested only after configuration import and never read from the archive.

Import compares records by `recordId`. When a record already exists, import reports a conflict and leaves local state
unchanged until the user chooses whether to keep the local record or replace it with the imported record. Import does
not infer ordering from timestamps and does not merge fields automatically.

Synchronization is outside this design. A future synchronization project must define transport, causal metadata,
tombstones, conflict behavior, and any required record-version change. It must build on the portable record boundaries
rather than create a parallel settings representation.

### Integration boundaries

The implementation exposes domain contracts for exporting and importing portable profile data. Its storage adapters read
current settings and account/profile repositories, map them to the portable record model, and map accepted imports back
through those repositories. It must not expose ZIP, encryption, PDPArchive, legacy preference, or database types to
callers. Codecs and validators implement the durable contract in the
[format architecture](../../architecture/portable-profile-data-format.md). POP3 and local-only mail use their separate versioned Thunderbird PDPArchive profiles defined there until the IETF draft defines a standard non-IMAP mapping. Exporters do
not fabricate IMAP-only values.

Global Database migration invokes only the portable-mail archive contract. It does not inspect archive layout or import
settings. The contract provides an estimated archive size and temporary workspace requirement for each destination
volume so the Global Database pre-flight check can include archive creation in its required headroom. If capacity for a
user-selected destination cannot be queried reliably, archive creation reports an actionable insufficient-space failure.

Archive creation runs within the user-initiated foreground migration operation owned by the Global Database design. The
contract reports non-sensitive phase and progress updates to the migration orchestrator, which persists them for the
in-app screen and foreground notification. It writes to an incomplete artifact and publishes the archive only after
verification. Cancellation or process interruption never publishes a partial archive. A later invocation removes or
replaces incomplete output before retrying.

For POP3, the contract returns one of four outcomes before database import begins:

- `Verified` identifies the completed and verified archive.
- `Declined` represents the user's acknowledgement of the data-loss warning.
- `Cancelled` leaves legacy storage authoritative and does not start database import.
- `Failed` includes an actionable, non-sensitive error and does not start database import.

Database import can proceed for POP3 only after `Verified` or `Declined`. IMAP archive creation remains optional, and
its cancellation or failure does not block an IMAP-only migration. A verified archive does not change which mail store
is authoritative. If the later Global Database import fails before cutover, legacy storage remains authoritative and the
verified archive remains a user-controlled artifact.

## Migration and Rollout

1. Define the portable settings/profile inventory and mapping from current account/profile and settings repositories.
2. Select and review maintained Android, JVM, iOS, and web implementations that support AES-256 encrypted ZIP64
   archives.
3. Implement encrypted ZIP64 archives, record codecs, the account-archive writer, cleanup, and untrusted-input validation
   with fixtures.
4. Implement one independently importable full PDPArchive `-01` archive per mail account, including separate Thunderbird
   POP3 and local-only profiles, size estimation, progress reporting, verified publication, and incomplete-output
   cleanup.
5. Add full-backup account and content-type selection and the cross-device passphrase import flow.
6. Integrate verified POP3 archive creation with Global Database preflight. Keep IMAP export user-requested and
   non-blocking.
7. Keep the existing settings importer available until the new importer supports its planned replacement scope. Do not
   reinterpret old exports as the new format without an explicit adapter.

No remote synchronization transport, remote storage, or account authentication replication is introduced by this
rollout.

## Testing and Verification

Automated verification must cover:

- encrypted Thunderbird envelope round trips containing zero, one, and multiple independently valid PDPArchive account
  archives and each settings/profile record type.
- encrypted standalone PDPArchive round trips with `archive.json` and `mail/` at the decrypted ZIP64 payload root.
- verification that every portable profile-data export is an AES-256 encrypted ZIP64 archive.
- incorrect-passphrase, tamper, truncation, and malformed-archive rejection.
- bounded-memory archive writing and reading on Android, JVM, iOS, and web.
- cross-implementation fixtures with supported independent ZIP tools.
- independent extraction and import of every account archive without the Thunderbird envelope.
- required PDPArchive `archive.json` and `folder.json` fields and exact draft-revision metadata.
- round-trip import of selected mail, global settings, account configuration, profile, identities, and folder settings.
- exact RFC 5322/MIME fidelity, including character sets, transfer encodings, MIME structure, signatures, encryption,
  and available attachment data.
- full-account PDPArchive semantics and rejection of unsupported partial, filtered, and incremental archives.
- IMAP exports, POP3 exports using `net.thunderbird.pdpa.pop3-v1`, and local-only exports using
  `net.thunderbird.pdpa.local-v1`.
- old emitted format and PDPArchive draft revisions, unknown optional fields, unknown required versions, and malformed
  records.
- stable account and record identity across export and import fixtures.
- existing-record conflict preview without writes before confirmation.
- credential, OAuth-token, private-key, queue, migration-state, and device-local-state exclusion.
- encrypted ZIP64 round trips for every export type and cross-device import.
- archive output and temporary-workspace estimates for same-volume and separate-volume destinations.
- archive progress propagation to the durable Global Database migration state.
- verified publication and cleanup of partial output after cancellation, process termination, or write failure.
- POP3 `Verified`, `Declined`, `Cancelled`, and `Failed` outcomes, with only the first two permitting database import.
- archive-destination insufficient-space and verification failures retaining legacy storage.
- IMAP optional archive cancellation or failure not blocking an IMAP-only Global Database migration.
- rejection of path traversal, absolute and duplicate normalized paths, links, malformed references, excessive expanded
  size, excessive entry count, and decompression bombs.
- redaction: archive contents, destinations, passphrases, account addresses, and server configuration never appear in
  logs, telemetry, or migration reports.

## Open Technical Questions

- Which maintained AES ZIP implementations satisfy the Android, JVM, iOS, and web security and maintenance requirements?
- Which global preferences and account/folder settings are cross-device meaningful enough for the first inventory?
- How should an import UI present concurrent same-record changes before a future synchronization service exists?
- Which upstream PDPArchive revision first defines a standard non-IMAP/POP3 mapping that can replace Thunderbird's
  versioned POP3 profile?

The technical design remains proposed until these implementation choices receive review.
