# Portable Profile Data Format

This document defines the durable archive-format contract for portable Thunderbird profile data. It complements
[RFC 0008](../engineering/rfcs/0008-portable-profile-data-format.md), its
[technical design](../engineering/technical-designs/0004-portable-profile-data-format.md), and the
[portable profile standards profile](../standards/portable-profile-data.md).

## Goals

The format prioritizes interoperability with
[draft-ietf-mailmaint-pdparchive-02](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02)
(PDPArchive). In particular:

- each exported mail account is independently usable as a PDPArchive full archive.
- Thunderbird-specific settings remain outside account archives.
- separate namespaced POP3 and local-only profiles preserve client-held mail without fabricating IMAP metadata.
- decrypting the age file and unpacking its ZIP64 payload exposes account archives that can be imported independently.
- draft-specific behavior is isolated so later PDPArchive revisions can be supported through explicit adapters.

PDPArchive `-02` is a work in progress. It defines a raw file layout but does not select a container format, define an
encryption mechanism, or provide a formal conformance section. In this document, a **PDPArchive account archive** means
a directory tree that follows the raw layout and JSON requirements of PDPArchive `-02`, including
[I-JSON](https://www.rfc-editor.org/info/rfc7493). Thunderbird packages the tree in
a ZIP64 archive and encrypts the complete archive using
[age version 1](https://age-encryption.org/v1) until PDPArchive standardizes a container and encryption mechanism.

## Format boundaries

The portable format has two independent layers:

1. **PDPArchive account archives** contain standards-based mail data for exactly one account.
2. **The Thunderbird envelope** packages zero or more account archives together with Thunderbird-specific profile and
   settings records.

An account archive MUST NOT depend on Thunderbird settings to interpret its mail. A generic PDPArchive importer MAY
import a decrypted and extracted account archive independently. Thunderbird settings and profile records MUST NOT be
inserted into an account archive. The POP3 and local-only profiles may add only the namespaced declarations and optional
metadata defined by this document. These additions are an implementation-specific extension, not a PDPArchive extension mechanism.

A standalone PDPArchive export uses the same mandatory age-encrypted ZIP64 representation. The ZIP64 payload root is the
raw PDPArchive account archive. The representation is replaced or adapted if PDPArchive standardizes a container and
encryption mechanism.

A settings-only export contains an encrypted Thunderbird envelope without account archives. It is not itself a
PDPArchive. Likewise, the complete Thunderbird envelope is a Thunderbird format containing PDPArchives. It is not
represented as one multi-account PDPArchive.

## Thunderbird envelope

The Thunderbird envelope is always an age-encrypted file whose ZIP64 plaintext has this logical layout:

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

Each account directory is a self-contained raw PDPArchive account archive. It contains `archive.json` and `mail/`
directly. The portable account ID is opaque and safe for use as a path segment.

The account directory MAY include a sanitized human-readable label, including an account name or email address, to make
manually extracted archives identifiable. The label is display-only and MUST NOT be used as account identity. The
portable account ID and manifest remain authoritative. Labels MUST be normalized into path-safe segments and duplicate
labels remain distinguishable through the portable account ID.

Age encrypts the complete ZIP64 payload, including entry names. Account labels MUST NOT contain email addresses or server
names because labels become visible after decryption and extraction. Labels and mail data at the user-selected
destination receive the same privacy warning.

`manifest.json` identifies the Thunderbird envelope, not PDPArchive. Version 1 has this logical shape:

```json
{
  "schema": "net.thunderbird.portable-profile-envelope",
  "schemaVersion": 1,
  "exportId": "opaque-unique-export-id",
  "createdAt": "2026-08-28T12:00:00Z",
  "generator": {
    "name": "Thunderbird for Android",
    "version": "generator-version"
  },
  "accounts": [
    {
      "portableAccountId": "opaque-stable-account-id",
      "archive": {
        "path": "accounts/Personal--opaque-stable-account-id",
        "pdpaRevision": "draft-ietf-mailmaint-pdparchive-02"
      },
      "extensions": []
    }
  ],
  "thunderbirdRecordsPresent": true
}
```

`exportId` is unique to an envelope. It is not a profile or account identity. `createdAt` is an RFC 3339 UTC timestamp.
A POP3 account archive includes `net.thunderbird.pdpa.pop3-v1` in `extensions`. A local-only account archive includes
`net.thunderbird.pdpa.local-v1`. The age envelope authenticates the complete encrypted ZIP64 payload.

Paths in `manifest.json` MUST be relative, normalized, and confined to the envelope. Unknown required envelope versions
are rejected. Unknown optional fields are preserved when possible and otherwise ignored.

## PDPArchive account archive

Each account archive represents exactly one source account. The initial implementation follows PDPArchive `-02`
[section 6.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.1) and
[section 6.3.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.1) for mail:

```text
archive.json
mail/
  <folder path>/
    folder.json
    <message>.eml
```

### Future contact and calendar support

The portable architecture also supports the standard PDPArchive contact and calendar data types when Thunderbird adds
those product features:

```text
archive.json
mail/
contacts/
calendars/
notes/
```

Contact export and import will follow PDPArchive
[section 6.3.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.2) and
[section 6.3.3](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.3), using
[JSContact](https://www.rfc-editor.org/info/rfc9553) and the
[RFC 9610](https://www.rfc-editor.org/info/rfc9610) address-book objects. Calendar export and import will follow
[section 6.3.4](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.4) and
[section 6.3.5](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.5), using
[JSCalendar](https://www.rfc-editor.org/rfc/rfc8984.html) for events and tasks and the calendar collection objects derived
from [JMAP for Calendars `-28`](https://datatracker.ietf.org/doc/html/draft-ietf-jmap-calendars-28). JSCalendar groups
are excluded because PDPArchive `-02`
[section 6.3.8](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.8) does not include
them in the archive format.

Future standalone contacts use the PDPArchive `pdpa-contact` schema. Address-book and calendar membership fields take
precedence over physical folder placement, exporters write one physical object when it belongs to multiple collections,
and importers restore every declared membership. Address books and calendars use their own collection objects rather
than mail `folder.json` files. Notes and the shared Link/blob model for non-mail attachments are also deferred. The blob
model does not apply to MIME attachments inside EML messages.

Contact, calendar, note, and non-mail attachment implementation is TBD and requires separate technical design and
delivery work. Thunderbird MUST NOT create a separate proprietary contact or calendar representation when the selected
PDPArchive revision provides the required data model. `archive.json` declares only data types actually included in an export. Until a data type is
implemented, import reports it as unsupported and does not modify or silently discard that data.

### Archive metadata

`archive.json` uses the PDPArchive archive metadata schema. Thunderbird MUST populate all fields required by the selected
draft revision, including:

- a unique archive identifier.
- archive name, timestamp, version, and generator.
- the `FULL` dataset extent, language, timezone, and the `MAIL` datatype.
- datasource service and account information where applicable.

Every generated PDPArchive JSON document conforms to I-JSON. `archive.version` identifies the exact PDPArchive draft
revision. The Thunderbird generator version is recorded separately in `archive.generator`. `datasource.account` uses the opaque portable account ID. It MUST NOT contain an email
address or credential. For example:

```json
{
  "$schema": "https://id.schemas.pub/o/DTI/PDPArchive/archive",
  "archive": {
    "id": "opaque-unique-account-archive-id",
    "name": "Thunderbird mail export",
    "timestamp": "2026-08-28T12:00:00Z",
    "version": "draft-ietf-mailmaint-pdparchive-02",
    "generator": "Thunderbird for Android/generator-version"
  },
  "dataset": {
    "extent": "FULL",
    "datatypes": ["MAIL"],
    "languagetag": "en",
    "timezone": "Etc/UTC"
  },
  "datasource": {
    "service": "imap",
    "account": "opaque-stable-account-id"
  }
}
```

The exported language and timezone reflect the dataset when known. The example values are not mandatory defaults. Each
account archive contains only one account because PDPArchive `-02` has one `datasource/account` and one root mail
hierarchy.
This also avoids collisions between same-named folders belonging to different accounts.

### Folders and messages

Every exported mail folder has a required `folder.json`. Source folder nesting is preserved as nested archive folders,
and account-portability imports restore that nesting. A folder name that is unsafe on the destination is translated
through a stable mapping so repeated exports and imports identify the same folder. Folder paths, metadata, item
references, flags, special-use values, and subscription state follow the selected PDPArchive revision without
Thunderbird-specific reinterpretation. Full exports omit removed-item lists and folder
tombstones.

Message files preserve the complete RFC 5322/MIME representation. Export MUST preserve message header fields, character
sets, content-transfer encodings, MIME structure, signed content, encrypted content, and attachment data. A message that
is only partially available MUST NOT be represented as complete. The exporter retrieves complete content before writing
the archive. If any selected account message cannot be retrieved completely, full-account archive creation fails and no
archive for that account is published.

Filenames are archive-local references, not identities. Portable folder and message identifiers are stable across
repeated exports and are distinct from local `FolderId`, `MessageId`, database row IDs, and filesystem names.

### Full exports

The initial Thunderbird profile supports only full mail-account exports. Every included account archive sets
`dataset.extent` to `FULL` and contains every locally available folder and complete message in that account. Choosing
which accounts to include does not make an included account archive partial.

For an IMAP account, `FULL` covers the account data represented by Thunderbird, not a guarantee that the server made all
remote data available. Thunderbird retrieves complete content for every represented message before publication. A
retrieval failure fails that account archive rather than silently producing a partial archive.

PDPArchive `-02` [section 4.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-4.2)
describes an approach to partial updates but does not define a complete interoperable incremental contract. Partial,
filtered, and incremental account archives are unsupported. Their identifiers, baselines, removal semantics, and
conflict behavior are deferred to the separately reviewed future synchronization project.

## Non-IMAP profile extensions

PDPArchive `-02` describes IMAP/JMAP mailboxes and includes IMAP-specific folder metadata. Its prose requirements and
published JSON Schema are not fully aligned for non-IMAP sources. Thunderbird MUST NOT fabricate IMAP protocol values
such as UIDVALIDITY or UIDNEXT-derived values. PDPArchive `-02` does not require generic importers to preserve or ignore
unknown members, so both profiles below are implementation-specific and are not unqualified PDPArchive conformance.

### POP3 profile

A POP3 account has a configured POP3 source server, but its downloaded messages and folders can exist only on the client.
The **Thunderbird PDPArchive POP3 profile version 1** provides the required backup:

- the account archive retains the standard `archive.json`, `mail/`, `folder.json`, and EML layout.
- `datasource.service` is `pop3` and `datasource.account` is the portable account ID.
- `dataset.extent` is `FULL`.
- standard PDPArchive fields are used unchanged whenever their semantics apply.
- stable opaque string UIDs identify folders and messages where the draft permits string identifiers.
- IMAP-only fields are omitted when no truthful value exists.
- optional POP3 source metadata is namespaced.
- `net.thunderbird.pdpa.pop3-v1` is declared in the root-level `archive.json` field
  `net.thunderbird:extensions` and in the Thunderbird manifest.

```json
{
  "net.thunderbird:extensions": ["net.thunderbird.pdpa.pop3-v1"]
}
```

POP3 UIDLs are source-server identifiers and MUST NOT be treated as portable message identifiers. Thunderbird allocates
and persists separate portable folder and message identifiers. Repeated exports reuse those identifiers. Imports map
them to local identifiers without rewriting existing local identifiers.

### Local-only profile

A local-only account has no POP3, IMAP, or JMAP source server. Its folders and messages are entirely app-managed. The
**Thunderbird PDPArchive local-only profile version 1** defines:

- the same standard `archive.json`, `mail/`, `folder.json`, and EML layout.
- `datasource.service` set to `local` and `datasource.account` set to the portable account ID.
- `dataset.extent` set to `FULL`.
- stable opaque string UIDs allocated and persisted by Thunderbird for folders and messages.
- omission of IMAP-only and POP3-only fields.
- `net.thunderbird.pdpa.local-v1` declared in `net.thunderbird:extensions` and in the Thunderbird manifest.

```json
{
  "net.thunderbird:extensions": ["net.thunderbird.pdpa.local-v1"]
}
```

The extension member is combined with, rather than substituted for, the standard `archive`, `dataset`, and `datasource`
members. Both non-IMAP profiles are structurally compatible with the `-02` JSON Schema but do not satisfy the
IMAP-specific prose requirements in PDPArchive
[section 6.3.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.1). Documentation and
UI identify the applicable Thunderbird profile. The implementation should raise both missing mappings with the IETF working group and replace each profile when a
standardized mapping becomes available.

## Thunderbird records

Thunderbird-owned JSON records use the versioned record shape defined by the technical design. These records contain
only portable profile, account-configuration, identity, folder-setting, and global-setting data. They exclude
credentials, OAuth tokens, certificate private keys, device-bound keys, permissions, notification channels, transient
state, queues, internal database state, migration state, and telemetry.

Thunderbird records may reference an account only through its portable account ID. They MUST NOT be required to recover
mail content or standard PDPArchive metadata.

## age encryption

Every Thunderbird profile export and standalone PDPArchive export is a binary
[age version 1](https://age-encryption.org/v1) file encrypted with the user passphrase. The authenticated plaintext is a
ZIP64 archive. ASCII armor and unencrypted portable profile-data exports MUST NOT be published.

Export streams the ZIP64 payload through the age encryptor into an incomplete output file and publishes it only after both
layers close successfully. Import decrypts and authenticates the complete age stream into an unpublished temporary ZIP64
file before opening and validating selected entries or changing runtime repositories. Authentication failure deletes the
temporary plaintext and imports nothing. Import storage estimation includes this temporary plaintext.

Android and JVM use [Kage](https://github.com/android-password-store/kage) behind the format boundary. The app's API 23
minimum is maintained through an upstream Kage compatibility patch or, temporarily, a minimal fork that replaces
`java.util.Base64` with `kotlin.io.encoding.Base64`. The patch does not alter age cryptography or wire-format behavior.
Other platforms use compatible maintained age implementations when those targets are implemented.

## Import and validation

Import treats every archive as untrusted input. Before presenting or applying records, it validates:

- ZIP structure, entry count, expanded sizes, compression ratios, and configured resource limits.
- absence of absolute paths, path traversal, duplicate normalized paths, links, and unsupported entry types.
- the envelope manifest and declared versions.
- every account archive independently against the selected PDPArchive revision.
- every JSON document's shape, required fields, numeric bounds, timestamps, and referenced files.
- I-JSON requirements for PDPArchive `-02`, including UTF-8, valid Unicode, unique decoded member names, and
  interoperable numeric values. Parsers MUST detect duplicate member names rather than silently retain one value.
- uniqueness and confinement of identifiers and file references, without implicit network retrieval.
- allow-listed portable fields without trusting imported sharing, role, or access-control metadata or selecting
  polymorphic deserializers from archive-provided type names.
- RFC 5322/MIME parseability without altering the original message representation.

Validation and selection occur before runtime repositories are changed. Failure in one account archive is reported for
that account and does not make corrupted content importable. Logs and diagnostics MUST NOT contain archive paths,
account addresses, server configuration, message data, passphrases, or decrypted content.

## Versioning and interoperability

Exporters and importers initially support PDPArchive `-02` and one Thunderbird envelope version. Importers support every
later PDPArchive revision and Thunderbird format version emitted by the app through explicit adapters.

A new PDPArchive revision is enabled only after fixtures verify:

- schema, required-field, and I-JSON compliance.
- independent import of each account archive.
- preservation of source folder nesting on export and account-portability import, with stable mapping of unsafe names.
- exact message/MIME fidelity.
- folder hierarchy, flags, identifiers, subscription state, and complete message selection.
- full-account export semantics and rejection of unsupported partial or incremental archives.
- IMAP, Thunderbird POP3-profile, and Thunderbird local-only-profile behavior.
- binary age version 1 interoperability with the reference age implementation, ZIP64 interoperability with independent
  ZIP tools, incorrect-passphrase rejection, and bounded-memory processing on JVM and Android API 23 and later.
- rejection of malformed and resource-exhaustion inputs.

Compatibility claims always name the exact PDPArchive revision. Until the draft selects a container and defines formal
conformance, documentation says that account archives follow the revision's raw layout using Thunderbird's temporary
age-encrypted ZIP64 convention rather than claiming compliance with a finalized IETF standard. This claim applies to
each decrypted and extracted account archive, not to its encrypted transport.
