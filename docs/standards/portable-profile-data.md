# Standards Profile: Portable Profile Data

- Status: Adopted for full mail-account export and age-encrypted ZIP64
- Decision: [RFC 0008](../engineering/rfcs/0008-portable-profile-data-format.md)
- Architecture: [Portable Profile Data Format](../architecture/portable-profile-data-format.md)
- Technical design: [Technical Design 0004](../engineering/technical-designs/0004-portable-profile-data-format.md)
- Implementation: TBD

This profile records the external specifications and local profiles that define portable profile-data interoperability.
Support claims apply to full mail-account archives only. Partial and incremental archives are outside the current scope.

## Standards index

|         ID          |                 Specification                 |  Revision or status   |               Adoption               |             Used by             |                                                Authoritative link                                                |                                                Notes                                                |
|---------------------|-----------------------------------------------|-----------------------|--------------------------------------|---------------------------------|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `pdpa-02`           | Personal Data Portability Archive             | Internet-Draft `-02`  | Adopted                              | Raw account archives            | [Draft `-02`](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02)                          | Work in progress with no selected container, encryption mechanism, or formal conformance definition |
| `i-json`            | Internet JSON                                 | RFC 7493              | Adopted for PDPArchive JSON          | JSON interoperability           | [RFC 7493](https://www.rfc-editor.org/rfc/rfc7493.html)                                                          | Required by PDPArchive `-02`                                                                        |
| `age-v1`            | age File Encryption                           | Version 1             | Adopted with scrypt passphrase mode  | Complete payload encryption     | [age version 1](https://age-encryption.org/v1)                                                                   | Binary format without ASCII armor                                                                   |
| `zip-6310`          | ZIP File Format Specification                 | APPNOTE 6.3.10        | Adopted temporary convention         | Authenticated plaintext payload | [APPNOTE 6.3.10](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)                                    | ZIP64 is replaced or adapted if PDPArchive selects a container                                      |
| `imf`               | Internet Message Format                       | RFC 5322              | Adopted                              | EML message representation      | [RFC 5322](https://www.rfc-editor.org/rfc/rfc5322.html)                                                          | Governs message syntax                                                                              |
| `mime-format`       | MIME Part One and Part Two                    | RFC 2045 and RFC 2046 | Adopted                              | EML body and multipart fidelity | [RFC 2045](https://www.rfc-editor.org/rfc/rfc2045.html), [RFC 2046](https://www.rfc-editor.org/rfc/rfc2046.html) | Governs MIME structure and transfer encoding                                                        |
| `imap4rev2`         | IMAP4rev2                                     | RFC 9051              | Adopted for IMAP archives            | Folder and message metadata     | [RFC 9051](https://www.rfc-editor.org/rfc/rfc9051.html)                                                          | PDPArchive also refers informatively to obsolete IMAP4rev1 terminology                              |
| `objectid`          | IMAP Object Identifiers                       | RFC 8474              | Adopted when supported by the source | Stable mailbox identity         | [RFC 8474](https://www.rfc-editor.org/rfc/rfc8474.html)                                                          | PDPArchive recommends `OBJECTID` for folder `uid`                                                   |
| `condstore`         | IMAP QRESYNC and CONDSTORE                    | RFC 7162              | Adopted when supplied by the source  | IMAP modification sequences     | [RFC 7162](https://www.rfc-editor.org/rfc/rfc7162.html)                                                          | Full export preserves truthful metadata but does not enable incremental export                      |
| `special-use`       | IMAP LIST Extension for Special-Use Mailboxes | RFC 6154              | Adopted                              | Special-use metadata            | [RFC 6154](https://www.rfc-editor.org/rfc/rfc6154.html)                                                          | Invoked by PDPArchive `folder.json`                                                                 |
| `imap-acl`          | IMAP ACL Extension                            | RFC 4314              | Adopted when supplied by the source  | Folder rights                   | [RFC 4314](https://www.rfc-editor.org/rfc/rfc4314.html)                                                          | Invoked by PDPArchive `myrights`                                                                    |
| `pop3`              | Post Office Protocol Version 3                | RFC 1939              | Adopted source protocol              | Thunderbird POP3 profile        | [RFC 1939](https://www.rfc-editor.org/rfc/rfc1939.html)                                                          | POP3 does not provide the IMAP folder metadata required by PDPArchive prose                         |
| `jscontact`         | JSContact                                     | RFC 9553              | Exploratory                          | Future contacts                 | [RFC 9553](https://www.rfc-editor.org/rfc/rfc9553.html)                                                          | Implementation TBD                                                                                  |
| `jmap-contacts`     | JMAP for Contacts                             | RFC 9610              | Exploratory                          | Future address books            | [RFC 9610](https://www.rfc-editor.org/rfc/rfc9610.html)                                                          | Implementation TBD                                                                                  |
| `jscalendar`        | JSCalendar                                    | RFC 8984              | Exploratory                          | Future events and tasks         | [RFC 8984](https://www.rfc-editor.org/rfc/rfc8984.html)                                                          | PDPArchive excludes JSCalendar groups                                                               |
| `jmap-calendars-28` | JMAP for Calendars                            | Internet-Draft `-28`  | Exploratory                          | Future calendar collections     | [Draft `-28`](https://datatracker.ietf.org/doc/html/draft-ietf-jmap-calendars-28)                                | Revision independently pinned by PDPArchive `-02`                                                   |

The `$schema` URI used by PDPArchive `archive.json` currently returns HTTP 404. Until an authoritative schema resource
is published there, validation fixtures pin the schema embedded in PDPArchive `-02`
[section 6.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.1) and track the upstream
availability issue. The `$schema` value remains unchanged in exported metadata.

## Capability dependency graph

```mermaid
graph TD
    PROFILE[Portable profile data] --> AGE[age version 1]
    AGE --> ZIP[ZIP64 APPNOTE 6.3.10]
    PROFILE --> PDPA[PDPArchive draft 02]
    PDPA --> IJSON[RFC 7493]
    PDPA --> IMF[RFC 5322]
    IMF --> MIME1[RFC 2045]
    IMF --> MIME2[RFC 2046]
    PDPA --> IMAP[RFC 9051]
    PDPA --> OBJECTID[RFC 8474]
    PDPA --> CONDSTORE[RFC 7162]
    PDPA --> SPECIALUSE[RFC 6154]
    PDPA --> ACL[RFC 4314]
    PROFILE --> POPPROFILE[Thunderbird POP3 profile v1]
    PROFILE --> LOCALPROFILE[Thunderbird local-only profile v1]
    POPPROFILE --> POP3[RFC 1939]
    POPPROFILE --> PDPA
    LOCALPROFILE --> PDPA
    PDPA -. future .-> JSCONTACT[RFC 9553]
    PDPA -. future .-> JMAPCONTACTS[RFC 9610]
    PDPA -. future .-> JSCALENDAR[RFC 8984]
    PDPA -. future .-> JMAPCAL[JMAP Calendars draft 28]
```

## Profiles and conventions

### Full mail-account profile

Every included account archive uses PDPArchive `-02`
[section 6](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6), sets
`dataset.extent` to `FULL`, and contains all complete messages available for the source account. Generated PDPArchive JSON
conforms to I-JSON. Thunderbird does not emit partial, filtered, or incremental account archives. PDPArchive
[section 4.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-4.2) is informative input
to the future synchronization project, not an adopted update contract.

### Thunderbird PDPArchive POP3 profile version 1

POP3 accounts use the PDPArchive raw layout and schema-compatible fields. They identify the source service as `pop3`,
omit IMAP-only values that have no truthful source, use portable identifiers distinct from POP3 UIDLs, and declare
`net.thunderbird.pdpa.pop3-v1`.

### Thunderbird PDPArchive local-only profile version 1

Local-only accounts have no remote source server. They use the PDPArchive raw layout and schema-compatible fields,
identify the source service as `local`, allocate stable opaque folder and message identifiers, omit IMAP-only and
POP3-only values, and declare `net.thunderbird.pdpa.local-v1`.

Both are implementation-specific profiles necessitated by the mismatch between the IMAP prose and general JSON Schema
in PDPArchive
[section 6.3.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.1). Neither is
unqualified PDPArchive conformance, and neither relies on standardized unknown-member handling.

### Container and encryption

An age-encrypted ZIP64 payload is Thunderbird's container convention because PDPArchive
[section 7.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-7.1) and
[section 7.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-7.2) leave the container
and encryption mechanisms open. Encryption uses binary [age version 1](https://age-encryption.org/v1) with its scrypt
passphrase recipient. ASCII armor and legacy ZipCrypto are not supported.

## Conformance disposition

| Governing specification |                                                 Section                                                 |          Requirement or boundary          |                              Disposition                              |                     Verification                     |
|-------------------------|---------------------------------------------------------------------------------------------------------|-------------------------------------------|-----------------------------------------------------------------------|------------------------------------------------------|
| PDPArchive `-02`        | [Section 6.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.1)     | Required archive metadata                 | Adopted with exact draft revision recorded                            | Schema fixture and field validation                  |
| PDPArchive `-02`        | [Section 6.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.2)     | Raw datatype and collection layout        | Adopted per account with source nesting preserved                     | Nested export and account-portability import fixture |
| PDPArchive `-02`        | [Section 6.3.1](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-6.3.1) | Mail folders, items, flags, and EML files | Adopted for IMAP and separately profiled for POP3 and local-only mail | IMAP, POP3, and local-only fixtures                  |
| PDPArchive `-02`        | [Section 4.2](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-4.2)     | Partial updates                           | Unsupported                                                           | Reject partial or incremental input as unsupported   |
| PDPArchive `-02`        | [Section 9](https://datatracker.ietf.org/doc/html/draft-ietf-mailmaint-pdparchive-02#section-9)         | I-JSON and untrusted-import guidance      | I-JSON required; importer protections adopted                         | I-JSON and malicious-archive fixtures                |
| RFC 7493                | [I-JSON](https://www.rfc-editor.org/rfc/rfc7493.html)                                                   | Interoperable JSON                        | Adopted for generated PDPArchive JSON                                 | UTF-8, Unicode, duplicate-name, and number fixtures  |
| age version 1           | [age specification](https://age-encryption.org/v1)                                                      | Binary scrypt passphrase encryption       | Adopted                                                               | Bidirectional reference-age fixtures                 |
| ZIP APPNOTE 6.3.10      | Section 4.5.3                                                                                           | ZIP64 extended information                | Temporary authenticated plaintext payload                             | ZIP64 boundary and malformed-container fixtures      |

## Implementation and verification status

[Kage v0.7.0](https://github.com/android-password-store/kage/releases/tag/v0.7.0) is the selected age version 1
implementation for Android and JVM. It supports streaming encryption and decryption and the scrypt passphrase recipient.
Kage currently declares Android API 26 because of `java.util.Base64`, as recorded in
[Kage issue #416](https://github.com/android-password-store/kage/issues/416). Thunderbird contributes a patch using the
stable KMP `kotlin.io.encoding.Base64` API and may temporarily maintain a minimal compatibility fork for API 23 through
25. The compatibility patch must not alter cryptographic or wire-format behavior. The reference
[age implementation](https://github.com/FiloSottile/age) provides the independent interoperability boundary. ZIP64
implementation selection remains TBD.

|                   Capability                   |  Implementation   |                        Fixtures                        |   Status    |
|------------------------------------------------|-------------------|--------------------------------------------------------|-------------|
| Full IMAP account archive                      | TBD               | PDPArchive `-02` fixtures TBD                          | Unsupported |
| Full POP3 account archive                      | TBD               | Thunderbird POP3 profile fixtures TBD                  | Unsupported |
| Full local-only account archive                | TBD               | Thunderbird local-only profile fixtures TBD            | Unsupported |
| age encryption on JVM                          | Kage v0.7.0       | Bidirectional reference-age fixtures TBD               | Unsupported |
| age encryption on Android API 23+              | Kage v0.7.0 patch | API 23–25 and reference-age fixtures TBD               | Unsupported |
| ZIP64 payload on JVM                           | TBD               | Independent ZIP-tool fixtures TBD                      | Unsupported |
| ZIP64 payload on Android                       | TBD               | Independent ZIP-tool fixtures TBD                      | Unsupported |
| Contacts and address books                     | TBD               | TBD                                                    | Deferred    |
| Events, tasks, notes, and calendar collections | TBD               | PDPArchive `-02` collection and Link/blob fixtures TBD | Deferred    |

## Open questions

- Which maintained ZIP64 implementation satisfies security, licensing, and bounded-memory requirements on Android and
  JVM?
- Which age scrypt work factor provides an acceptable passphrase-derivation cost across supported Android devices?
- Which PDPArchive `-02` importer combinations define the first interoperability test set?
- Which future PDPArchive revision provides standard POP3 and local-only mappings that replace the Thunderbird profiles?
- Which future synchronization design, if any, introduces partial archives or additional record metadata?

The `-02` importer may reject non-I-JSON documents. Optional IMAP modification-sequence values that cannot be represented
interoperably under I-JSON are omitted because incremental export is outside this profile, rather than encoded using a
schema-invalid type.

Future standalone contacts use the `pdpa-contact` schema and declared collection memberships take precedence over
physical placement. Notes and non-mail Link/blob attachments remain deferred. This Link/blob model does not apply to MIME
attachments inside EML messages.

The adopted format remains unsupported until the implementation and interoperability gates above pass.
