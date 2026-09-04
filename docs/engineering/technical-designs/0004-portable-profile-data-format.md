# Technical Design: Portable Profile Data Format

- Related milestone: [Global Database #10374](https://github.com/thunderbird/thunderbird-android/issues/10374)
- RFC: [RFC 0008: Portable Profile Data Format](../rfcs/0008-portable-profile-data-format.md)
- Mail archive compatibility target: [draft-ietf-mailmaint-pdparchive-01: Personal Data Portability Archive](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-01)
- Full-backup encryption: [NIST SP 800-38D: Galois/Counter Mode](https://csrc.nist.gov/pubs/sp/800/38/d/final)
- Status: **Accepted**

## Summary

This design defines the archive layout, Thunderbird settings/profile record envelope, import behavior, and encrypted
full-backup envelope for RFC 0008. It creates a portable and sync-ready representation without adding a remote
synchronization transport or moving current runtime settings storage.

## Current State

Mail is stored in legacy per-account databases and attachments directories pending Global Database cutover. Account
profiles already have domain data such as a stable account ID, name, color, and avatar, but the account and settings
storage remains legacy-backed. The current settings importer is Android-specific and imports existing settings formats.
It is not a durable, versioned profile-data contract.

Account activation currently obtains credentials after settings import. This behavior remains: credentials and OAuth
tokens are not exported and imported accounts prompt for authentication when required.

## Proposed Design

### Archive forms

The portable profile-data export is an unencrypted ZIP64 container. Its mail portion follows the selected PDPArchive
draft's current folder and JSON structure. The app records the exact PDPArchive draft revision in the archive metadata.

The full app backup is the same complete bundle in a versioned application envelope encrypted with AES-256-GCM. A
user-supplied passphrase derives the encryption key, allowing an export to be imported on another device. The envelope
contains its encryption algorithm identifier, key-derivation algorithm and parameters, salt, nonce, authentication tag,
and envelope version. It rejects authentication failures without extracting or importing partial data.

The portable and encrypted forms contain the same logical records. They differ only in the outer container and user
warning. The portable form may contain selected mail, settings/profile data, or both. The full backup contains the
complete selected portable profile-data bundle.

Individual-message and selected-folder EML export are separate mail-export flows. They do not write this bundle, use
its import path, or satisfy the POP3 migration prerequisite.

### Bundle layout

The following application-owned namespace is included alongside the PDPArchive mail layout. Exact PDPArchive paths
remain governed by the selected draft.

```text
archive metadata and PDPArchive mail layout
thunderbird/
  profile.json
  settings/global.json
  accounts/<portable-account-id>/profile.json
  accounts/<portable-account-id>/configuration.json
  accounts/<portable-account-id>/identities.json
  accounts/<portable-account-id>/folder-settings.json
```

`profile.json` identifies the exported Thunderbird profile and archive selection. `settings/global.json` holds portable
global user preferences. Account records contain portable account configuration and profile data in a new format rather
than legacy preferences, database values, or serialized implementation classes. Mail availability and partial-content
state remain represented by the PDPArchive-compatible mail metadata.

### Record envelope and data boundaries

Every Thunderbird-owned JSON record uses this logical envelope:

```json
{
  "schema": "net.thunderbird.portable-profile-data",
  "schemaVersion": 1,
  "recordId": "opaque-stable-id",
  "revision": { "counter": 42, "originId": "opaque-installation-id" },
  "updatedAt": "2026-08-28T12:00:00Z",
  "deleted": false,
  "payload": {}
}
```

The portable account ID is stable across export/import and is distinct from the application-profile-scoped `AccountId`
used by the Global Database. Portable record identifiers are also distinct from local `FolderId`, `MessageId`, and
`ThreadId` values. Import maps a portable account ID to a user-selected existing account or allocates a new local
`AccountId`. Existing local identifiers are never rewritten. Import never treats a legacy database ID, account number,
row ID, or local mail-domain identifier as portable identity.

`originId` is an opaque installation identity used to identify a revision's origin and order revisions produced by that
same origin. It is not a global last-writer-wins tiebreaker. Tombstones let a later sync service
propagate account/profile deletion without treating an omitted record as an ambiguous delete. Portable snapshots may
compact a tombstone only when retaining it cannot change subsequent import or synchronization semantics.

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

### Import and merge behavior

Import first validates the container or authenticated backup envelope, then parses all records without changing runtime
storage. It presents a selection and conflict preview before applying user-selected records. Account credentials are
requested only after configuration import and never read from the archive.

Record identity and revisions make the format sync-ready. Import compares records by `recordId`. A larger counter from
the same `originId` identifies a newer revision from that origin, and tombstones identify deletion. Revisions from
different origins are not ordered using `originId` or `updatedAt`; an import that cannot prove one revision supersedes
the other reports a conflict and leaves local state unchanged until the user chooses a result.

This design does not select a remote transport or silently resolve same-record, divergent user edits. A future sync
design must define any additional causal metadata needed for remote replication by versioning this envelope. It must
not create a parallel settings representation.

### Integration boundaries

The implementation exposes domain contracts for exporting and importing portable profile data. Its storage adapters read
current settings and account/profile repositories, map them to the portable record model, and map accepted imports back
through those repositories. It must not expose ZIP, encryption, PDPArchive, legacy preference, or database types to
callers.

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
2. Implement versioned record codecs, ZIP64 bundle writing, and import validation with fixtures.
3. Implement portable mail integration using PDPArchive draft `-01`, including size estimation, progress reporting,
   verified publication, and incomplete-output cleanup.
4. Add the encrypted full-backup envelope and cross-device passphrase import flow.
5. Integrate verified POP3 archive creation with Global Database preflight. Keep IMAP export user-requested and
   non-blocking.
6. Keep the existing settings importer available until the new importer supports its planned replacement scope. Do not
   reinterpret old exports as the new format without an explicit adapter.

No remote synchronization transport, remote storage, or account authentication replication is introduced by this
rollout.

## Testing and Verification

Automated verification must cover:

- ZIP64 portable exports containing representative PDPArchive mail and each settings/profile record type.
- round-trip import of selected mail, global settings, account configuration, profile, identities, and folder settings.
- old emitted format and PDPArchive draft revisions, unknown optional fields, unknown required versions, and malformed
  records.
- stable account identity, same-origin revision ordering, cross-origin conflict detection, and tombstone behavior across
  export/import fixtures.
- selective import and conflict preview without writes before confirmation.
- credential, OAuth-token, private-key, queue, migration-state, and device-local-state exclusion.
- AES-256-GCM encrypted-backup round trips, incorrect-passphrase and tamper rejection, and cross-device import.
- archive output and temporary-workspace estimates for same-volume and separate-volume destinations.
- archive progress propagation to the durable Global Database migration state.
- verified publication and cleanup of partial output after cancellation, process termination, or write failure.
- POP3 `Verified`, `Declined`, `Cancelled`, and `Failed` outcomes, with only the first two permitting database import.
- archive-destination insufficient-space and verification failures retaining legacy storage.
- IMAP optional archive cancellation or failure not blocking an IMAP-only Global Database migration.
- redaction: archive contents, destinations, passphrases, account addresses, and server configuration never appear in
  logs, telemetry, or migration reports.

## Open Technical Questions

- Which key-derivation function and initial cost parameters provide suitable Android and JVM desktop support for the
  passphrase envelope?
- Which global preferences and account/folder settings are cross-device meaningful enough for the first inventory?
- How should an import UI present concurrent same-record changes before a future synchronization service exists?
- Which PDPArchive `-01` JSON schemas are required for the first release, and how are later draft adapters tested?

