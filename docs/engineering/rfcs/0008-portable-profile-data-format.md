# RFC 0008: Portable Profile Data Format

- Issue: [#11466](https://github.com/thunderbird/thunderbird-android/issues/11466)
- Related milestone: [Global Database #10374](https://github.com/thunderbird/thunderbird-android/issues/10374)
- Technical design: [Portable Profile Data Format](../technical-designs/0004-portable-profile-data-format.md)
- Format architecture: [Portable Profile Data Format](../../architecture/portable-profile-data-format.md)
- Standards profile: [Portable Profile Data Standards](../../standards/portable-profile-data.md)
- Mail archive compatibility target: [draft-ietf-mailmaint-pdparchive-02: Personal Data Portability Archive](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02)
- Mail message format: [RFC 5322: Internet Message Format](https://www.rfc-editor.org/info/rfc5322/)
- MIME: [RFC 2045: Multipurpose Internet Mail Extensions](https://www.rfc-editor.org/info/rfc2045/)
- Encryption format: [age version 1](https://age-encryption.org/v1)
- Status: **Accepted**

## Summary

Define one versioned, user-controlled portable profile-data format for mail, app settings, account configuration, and
account profile data. The format supports portable full export, backup, and import. Synchronization is a separate future
project and may extend the record format after separate product, architecture, privacy, and security review.

Each mail account is exported as an independently importable PDPArchive account archive. A versioned, encrypted
Thunderbird envelope packages zero or more raw account archives with Thunderbird-owned settings and profile records
without changing the PDPArchive contents. Every portable profile-data export is an age version 1 passphrase-encrypted
file whose authenticated plaintext is a ZIP64 archive, including standalone PDPArchive and settings-only exports.

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

- **Mail:** One self-contained raw account archive per mail account, with
  [RFC 5322](https://www.rfc-editor.org/info/rfc5322/)/[MIME](https://www.rfc-editor.org/info/rfc2045/) message data and
  mail metadata following the current
  [PDPArchive draft layout](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02).
- **Global settings:** portable, user-selected application preferences in a Thunderbird-owned JSON namespace.
- **Account configuration:** account identity, server endpoints and protocol configuration, identities, sync choices,
  folder choices, and other user-configured account behavior in a new portable shape.
- **Account profile:** stable account identifier, display name, color, avatar selection, ordering, and other portable
  profile presentation data in the same new shape.

The Thunderbird envelope has an app format version and generator version. It is a binary
[age version 1](https://age-encryption.org/v1) file encrypted with the user passphrase. The authenticated plaintext is a
ZIP64 archive. Every account archive records its exact PDPArchive draft revision and is importable after age decryption
and ZIP extraction. Importers tolerate unknown fields, reject unknown required format versions safely, and migrate every
older version that this app has emitted. PDPArchive is the app's versioned mail-archive format. The app initially emits
and imports `-02`. Later revisions are introduced through explicit adapters as the draft evolves toward RFC status. All
PDPArchive JSON generated for `-02` conforms to
[I-JSON](https://www.rfc-editor.org/info/rfc7493).

PDPArchive `-02` does not select a container or encryption mechanism. An age-encrypted ZIP64 payload is Thunderbird's
temporary container and encryption convention until the standard settles those concerns. The multi-account Thunderbird
envelope contains raw PDPArchive account directories but is not itself represented as one PDPArchive. Thunderbird
settings remain outside account archives. A standalone export uses the same age-encrypted representation, with
`archive.json` and `mail/` at the root of its decrypted ZIP64 payload.

PDPArchive also defines standard contact and calendar representations based on
[JSContact](https://www.rfc-editor.org/info/rfc9553),
[RFC 9610 address-book objects](https://www.rfc-editor.org/info/rfc9610),
[JSCalendar](https://www.rfc-editor.org/info/rfc8984), and
[JMAP for Calendars](https://datatracker.ietf.org/doc/html/draft-ietf-jmap-calendars-28). The portable architecture supports adding contacts, address books, events, tasks, notes, and calendar collections to the same
per-account archives. PDPArchive `-02`
[section 6.3.8](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.8) excludes
JSCalendar groups. Product support for contact, calendar, note, and non-mail attachment export and import is TBD and
outside the current rollout.

### Portable settings and profile records

Settings and profile data are portable records, not serialized preference files. Each record has a stable opaque
identifier and schema version. Account records use a stable portable account identifier rather than a database row ID or
legacy preference key.

The record boundaries allow independent import of global settings, an account profile, account configuration,
identities, and folder choices. Import detects an existing record by identity and asks the user before replacement. The
format does not define revision ordering, tombstone propagation, automatic merge, or synchronization conflict
resolution. A future synchronization project may extend these records but must not introduce a parallel settings
representation.

### Export, import, and backup

Portable export lets the user choose complete mail accounts, settings and profile data, or both. Every included mail
account is exported as a full account archive. Partial, filtered, and incremental archives are deferred to the future
synchronization project because PDPArchive `-02`
[section 4.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-4.2) does not define a
complete interoperable incremental contract. A POP3-containing Global Database migration requires successful creation
and verification of the mail part, unless the user declines it after an explicit warning. Settings/profile export is not a cutover prerequisite. An IMAP mail export remains user-requested and
non-blocking.

PDPArchive `-02` does not fully define non-IMAP mappings. Until it does, POP3 and local-only mail use separate declared,
versioned Thunderbird profiles. Both retain the PDPArchive layout and standard fields, use stable opaque string
identifiers, omit inapplicable IMAP-only metadata rather than fabricating it, and keep optional additions namespaced.
Exports preserve source folder nesting. Account-portability imports preserve that nesting, and translations of folder
names that are unsafe on the destination use a stable mapping so repeated imports identify the same folder.
The POP3 profile describes mail downloaded from a configured POP3 source. The local-only profile describes app-managed
mail with no remote source server.

Import previews the contained data and lets the user select what to import. It never silently overwrites a conflicting
account or setting. It restores portable configuration without secrets, then asks the user to authenticate accounts as
needed.

Every portable export, including a standalone PDPArchive, settings-only export, and full app backup, is an age version 1
passphrase-encrypted file containing a ZIP64 payload. Android and JVM use
[Kage](https://github.com/android-password-store/kage) through a narrow encryption boundary. Thunderbird contributes an
API 23 compatibility patch upstream and may temporarily maintain a minimal fork containing only that compatibility
change until an upstream release includes it. Other platforms use compatible maintained age implementations when those
targets are implemented. No unencrypted portable export is published.

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
standalone import/export and full backup contract. It does not move runtime settings storage into
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
security work. This RFC establishes a portable data contract without expanding scope to remote replication. A future
synchronization project may extend the record metadata after separate review.

### Store credentials in portable data

This would substantially increase the consequences of accidental disclosure and cannot safely cover device-bound or
provider-managed authentication state. Imported accounts instead require authentication when necessary.

### Use individual-message or folder EML export as the portable profile-data format

EML preserves a message payload but not the complete profile, account, settings, folder, label, or read-state model.
It remains useful for its narrower mail-export use cases, not for profile portability or backup.

## Risks & Drawbacks

- PDPArchive remains an evolving Internet-Draft, does not yet select a container or encryption mechanism, and has no
  complete non-IMAP mapping. The app must maintain temporary transport conventions, version adapters, and a declared
  POP3 profile.
- The new settings and profile contract needs a deliberate inventory to avoid losing a user-visible setting or carrying
  device-local state into portability.
- Kage currently declares Android API 26. Supporting the app's API 23 minimum requires an upstream compatibility patch
  or a narrowly maintained fork, plus API 23 through 25 verification.
- The age and ZIP layers require separate interoperability fixtures with the reference age implementation and independent
  ZIP tools.
- User-facing conflict resolution can be complex when imported configuration differs from existing accounts.
- All portable bundles add passphrase-loss risk. The app cannot recover an export or backup without its passphrase.
- Portable exports can contain sensitive mail addresses, server details, and message content even when credentials are
  excluded.

## Open Questions

- Which user settings and account or folder choices are portable, and which are explicitly device-local?
- What field-group conflict presentation is appropriate when an imported record conflicts with local profile data?
- Which age scrypt work factor provides an acceptable passphrase-derivation cost across supported Android devices?
- Which PDPArchive `-02` target-client combinations are supported by the first release, and how are later revisions
  introduced?
- Which upstream PDPArchive revision first provides standard POP3 and local-only mappings that can replace Thunderbird's
  versioned profiles?

## Outcome

The proposal was accepted. This RFC authorizes the portable profile-data contract and its standalone import/export and
backup work. Implementation follows
[Technical Design 0004: Portable Profile Data Format](../technical-designs/0004-portable-profile-data-format.md), with
the durable format boundary defined by the
[Portable Profile Data Format architecture](../../architecture/portable-profile-data-format.md). Remote synchronization
remains a follow-up that must build on these portable record boundaries and receive separate review.
