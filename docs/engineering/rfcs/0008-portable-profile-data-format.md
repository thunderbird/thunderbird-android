# RFC 0008: Portable Profile Data Format

- Issue: [#11466](https://github.com/thunderbird/thunderbird-android/issues/11466)
- Related milestone: [Global Database #10374](https://github.com/thunderbird/thunderbird-android/issues/10374)
- Technical design: [Portable Profile Data Format](../technical-designs/0004-portable-profile-data-format.md)
- Mail archive compatibility target: [draft-ietf-mailmaint-pdparchive-02: Personal Data Portability Archive](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02)
- Mail message format: [RFC 5322: Internet Message Format](https://www.rfc-editor.org/info/rfc5322/)
- MIME: [RFC 2045: Multipurpose Internet Mail Extensions](https://www.rfc-editor.org/info/rfc2045/)
- Full-backup encryption: [NIST SP 800-38D: Galois/Counter Mode](https://csrc.nist.gov/pubs/sp/800/38/d/final)
- Status: **Proposed**

## Summary

Define one versioned, user-controlled portable profile-data format for mail, app settings, account configuration, and
account profile data. The format supports portable export/import immediately and carries the stable identities,
revisions, tombstones, and merge metadata required by a future synchronization service. It does not add that service.

Mail uses the current PDPArchive folder and JSON structure inside a ZIP64 container. Thunderbird-owned settings and
profile records use a separate, versioned namespace in the same archive. A full app backup encrypts the complete
portable bundle using a user passphrase, while portable export remains a user-selected data export.

## Motivation

Global Database needs a verified portable-mail artifact before POP3 cutover, but its database migration must not
own a user-data format, backup implementation, or settings migration. The app also needs a durable format for
user-controlled settings and account/profile data that can be exported, imported, and later synchronized without
serializing legacy preferences or storage classes.

Existing account/profile and settings data is still represented through legacy-backed storage. That representation is
not a portable contract and cannot safely become a synchronization protocol. Credentials, OAuth tokens, certificate
private keys, device-local state, and runtime queues also must not become portable profile data.

## Proposal

### One portable profile-data bundle

The app defines a versioned portable profile-data bundle with these parts:

- **Mail:** RFC 5322/MIME message data and mail metadata following the current PDPArchive draft layout.
- **Global settings:** portable, user-selected application preferences in a Thunderbird-owned JSON namespace.
- **Account configuration:** account identity, server endpoints and protocol configuration, identities, sync choices,
  folder choices, and other user-configured account behavior in a new portable shape.
- **Account profile:** stable account identifier, display name, color, avatar selection, ordering, and other portable
  profile presentation data in the same new shape.

The format has an app format version, generator version, and PDPArchive draft revision. Importers tolerate unknown
fields, reject unknown required format versions safely, and migrate every older version that this app has emitted.
PDPArchive is the app's versioned mail-archive format. The app maintains adapters as the draft evolves toward RFC
status.

### Portable and sync-ready settings/profile records

Settings and profile data are portable records, not serialized preference files. Each record has a stable opaque
identifier, schema version, revision, origin identifier, modification timestamp, and deletion tombstone. Account
records use a stable portable account identifier rather than a database row ID or legacy preference key.

The record boundaries allow independent changes to global settings, an account profile, account configuration,
identities, and folder/sync choices. Import and a future sync service can merge records by identity and revision without
changing the format. A later synchronization transport may add authentication, replication, conflict UX, and remote
storage, but it must use these records rather than introduce a second settings representation.

### Export, import, and backup

Portable export lets the user choose mail, settings/profile data, or both. It records selected and unavailable content
explicitly. A POP3-containing Global Database migration requires successful creation and verification of the mail part,
unless the user declines it after an explicit warning. Settings/profile export is not a cutover prerequisite. An IMAP
mail export remains user-requested and non-blocking.

Import previews the contained data and lets the user select what to import. It never silently overwrites a conflicting
account or setting. It restores portable configuration without secrets, then asks the user to authenticate accounts as
needed.

Full app backup wraps the same portable bundle in an app-specific AES-256-GCM encrypted envelope. Its key is derived
from a user-supplied passphrase so the backup can be imported on another device. The envelope records its encryption
and key-derivation versions and parameters so those choices can evolve independently of the profile-data format.

Individual-message and selected-folder EML export are separate mail-export capabilities. They do not act as a fallback
for this profile-data format, do not gate migration, and do not preserve portable profile-data metadata.

### Privacy and exclusions

Portable profile data and full backups exclude passwords, OAuth access and refresh tokens, certificate private keys,
queued operations, internal database and migration state, device-bound keys, device permissions, and telemetry data.
They must not be logged, uploaded, or included in migration diagnostics. Email addresses and server configuration are
user data and must receive the same destination, privacy, and encryption warnings as mail content.

### Relationship to Global Database

[RFC 0007: Global Database](0007-global-database.md) owns the storage cutover. It depends on this RFC only
for verified POP3 mail-archive creation before database import begins. This RFC owns the archive and settings/profile format,
standalone import/export, full backup, and sync-ready record contract. It does not move runtime settings storage into
the Global Database.

## Alternatives Considered

### Keep mail export, settings import, and full backup as unrelated formats

This would duplicate versioning, user experience, privacy, and import logic. It would also leave no stable route to
synchronize settings/profile data later.

### Serialize legacy preferences and account objects

Legacy keys and storage classes are implementation details. Serializing them would couple portable data to Android and
prevent safe evolution, selective import, and conflict handling.

### Implement remote synchronization now

Transport, account authentication, conflict UX, privacy policy, and backend selection are separate product and
security work. This RFC establishes a usable, sync-ready data contract without expanding scope to remote replication.

### Store credentials in portable data

This would substantially increase the consequences of accidental disclosure and cannot safely cover device-bound or
provider-managed authentication state. Imported accounts instead require authentication when necessary.

### Use individual-message or folder EML export as the portable profile-data format

EML preserves a message payload but not the complete profile, account, settings, folder, label, or read-state model.
It remains useful for its narrower mail-export use cases, not for profile portability or backup.

## Risks & Drawbacks

- PDPArchive remains an evolving Internet-Draft, so the app must maintain version adapters for revisions it emits and
  imports.
- The new settings/profile contract needs a deliberate inventory to avoid losing a user-visible setting or carrying
  device-local state into portability.
- User-facing conflict resolution can be complex when imported configuration differs from existing accounts.
- Full backups add passphrase-loss risk. The app cannot recover a backup without its passphrase.
- Portable exports can contain sensitive mail addresses, server details, and message content even when credentials are
  excluded.

## Open Questions

- Which user settings and account/folder choices are portable and syncable, and which are explicitly device-local?
- What field-group conflict presentation is appropriate when an imported record conflicts with local profile data?
- Which passphrase key-derivation function and parameter policy should the first encrypted-backup envelope use?
- Which PDPArchive `-02` target-client combinations are supported by the first release, and how are later revisions
  introduced?

## Outcome

Pending review.

If accepted, this RFC authorizes the portable profile-data contract and its standalone import/export and backup work.
Remote synchronization remains a follow-up that must use this contract and receive separate review.
