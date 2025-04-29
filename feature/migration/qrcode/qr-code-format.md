# Thunderbird "Export for Mobile" QR code format (version 1)

This specification describes the data format of the QR code payload used by Thunderbird desktop to export account to
Thunderbird Mobile.

## Specification versions

### Version 1 (2025-02-06)

Initial version.

## Terms

### Reader

A reader is the software parsing the QR code payload after it has been scanned, e.g. Thunderbird for Android.

### Writer

A writer is the software creating QR codes that follow this specification, e.g. Thunderbird for desktop.

### Account

For the purpose of this specification an account is the combination of an `IncomingServer` object and the
`OutgoingServerGroups` object that is directly following this `IncomingServer` object.
See [root element](#root-element).

### Version

Whenever this document refers to a version without qualifier, the [specification version](#specification-versions) is
meant. It is different from the [format version](#formatversion) that is expected to stay the same until a
backward-incompatible change to the data format needs to be made.

## General structure

The QR code payload is JSON, encoded using UTF-8. The data objects like incoming server, outgoing server, and identities
are mapped to JSON arrays.

The data format is extensible by allowing arrays to contain additional elements from the ones specified in the initial
version. This applies to all arrays unless otherwise stated, but it is usually also explicitly spelled out.
For forward-compatibility a reader must ignore additional array values not listed in the version of the specification it
implements. A writer must not add elements to an array that are not part of the version of the specification it
implements.

### Motivation

We chose JSON because it's a widely supported format that is easily extensible, i.e. it's easy to add additional
properties later. We decided to use nested JSON arrays because it leads to compact output and space in QR codes is very
limited.

The downside of using JSON arrays is that it's most likely more work for implementations. For most object oriented
languages there exist libraries to map objects to JSON objects. However, library-assisted mapping of objects to JSON
arrays is not something that is typically available.
Another issue worth mentioning is that using JSON arrays makes the output harder to read for humans.

This is not a great data format. But it's one that seems to work well within the constraints of a QR code.

### Compatibility

If you intend to create a reader that is not using all of the information present in the payload, you must still
validate all of the properties, even the ones you're not using. This is to avoid situations where QR codes are being
recognized as valid by one (incomplete) reader but not another (full) reader.

### Multiple QR codes

The data format supports encoding an unlimited number of accounts to be able to export multiple accounts at the same
time. However, the payload size of a QR code is limited. So when exporting multiple accounts, multiple QR codes might
have to be used. The `SequenceNumber`/`SequenceEnd` mechanism (see below) is used to signal to the reader how many
QR codes are part of an export and in which order.

Note: This data format doesn't support spreading data for one account over multiple QR codes. So the amount of data that
can be used to encode one account is limited by the maximum QR code size. So far this hasn't been a problem in practice.

It's up to the writer to decide on a strategy of how many accounts to store in one QR code. But since larger QR codes
are harder to scan, it's probably a good idea to aim for a specific (maximum) QR code size, rather than using a fixed
number of accounts per QR code.

## Data format

### Root element

- Available since: version 1
- Type: Array

The root element of the JSON document is an array containing the following elements in this order:

- `FormatVersion`
- `MiscellaneousData`
- `IncomingServer`
- `OutgoingServerGroups`

The array may contain additional `IncomingServer` and `OutgoingServerGroups` elements. But they always have to appear in
pairs in exactly this order because only together they make up an account.

### `FormatVersion`

- Available since: version 1
- Type: Integer
- Value: 1

The version of the data format. This only needs to change if the existing mechanism for extensibility isn't sufficient
and the data format itself has to be changed in a backward-incompatible way. Of course the new data format might not
use a JSON array as root element. In that case the incompatibility can be detected without this property.

This document only describes format version 1. If a reader encounters any other value, it needs to either explicitly
support that specific version or fail with an error.

### `MiscellaneousData`

- Available since: version 1
- Type: Array

This array contains the following elements in this order:

- `SequenceNumber`
- `SequenceEnd`

Future versions of this specification may add additional values to this array.

### `SequenceNumber`

- Available since: version 1
- Type: Integer

Information about multiple accounts might not fit into a single QR code. This property contains the 1-based index of
the current QR code. A reader can use this information together with `SequenceEnd` to figure out how many QR codes in a
sequence have already been read and how many are still missing.

### `SequenceEnd`

- Available since: version 1
- Type: Integer

Information about multiple accounts might not fit into a single QR code. This property contains the number of QR codes
used for a single export operation. A reader can use this information together with `SequenceNumber` to figure out how
many QR codes in a sequence have already been read and how many are still missing.

### `IncomingServer`

- Available since: version 1
- Type: Array

This type contains properties that are only present once in an account, mostly information about the incoming server .
The first element in this array is `IncomingProtocol`. Its value determines the contents of the rest of the array.

For forward-compatibility a reader must skip reading an account when an unsupported `IncomingProtocol` value is
encountered.

Note: Since the size of this array depends on the value of `IncomingProtocol`, future specifications can only add
properties on a per protocol basis.

### `IncomingProtocol`

- Available since: version 1
- Type: Integer
- Values:
  - 0 (`IMAP`; available since: version 1)
  - 1 (`POP3`; available since: version 1)

For the values 0 and 1 the contents of the `IncomingServer` array are as follows:

- `IncomingProtocol`
- `Hostname`
- `Port`
- `ConnectionSecurity`
- `AuthenticationType`
- `Username`
- `AccountName` (optional)
- `Password` (optional)

For forward-compatibility a reader must skip reading an account when an unsupported `IncomingProtocol` value is
encountered. It must also skip the account if any of the other properties contain unsupported values.

A writer may omit optional elements from the array if the subsequent elements are also omitted.

#### Writing and reading `AccountName`

If the account name is equal to the email address of the first identity, the writer may omit the value. If the element
can't be omitted (because one of the following elements is present), `null` or the empty string may be used instead.

A reader must use the email address of the first identity as account name if `AccountName` is omitted or its value is
`null` or the empty string.

#### Writing and reading `Password`

If the writer doesn't want to include the password, it may omit the `Password` element.

For forward-compatibility a reader must treat a `Password` value of `null` or the empty string like an omitted
password.

### `Hostname`

- Available since: version 1
- Type: String

A server hostname. Currently only ASCII-only hostnames are allowed. This includes the ASCII Compatible Encoding (ACE) of
Internationalized Domain Names (IDN).

### `Port`

- Available since: version 1
- Type: Integer
- Values: 1-65535

The TCP port used by an incoming or outgoing server.

### `ConnectionSecurity`

- Available since: version 1
- Type: Integer
- Values:
  - 0 (`Plain`; available since: version 1)
  - 1 (legacy, do not use; reserved since: version 1)
  - 2 (`AlwaysStartTls`; available since: version 1)
  - 3 (`Tls`; available since: version 1)

Describes if and how to use TLS to secure the connection to a server.

### `AuthenticationType`

- Available since: version 1
- Type: Integer
- Values:
  - 0 (`None`; available since: version 1)
  - 1 (`PasswordCleartext`; available since: version 1)
  - 2 (`PasswordEncrypted`; available since: version 1)
  - 3 (`Gssapi`; available since: version 1)
  - 4 (`Ntlm`; available since: version 1)
  - 5 (`TlsCertificate`; available since: version 1)
  - 6 (`OAuth2`; available since: version 1)

The authentication method to use.

### `Username`

- Available since: version 1
- Type: String

The username to use for authentication.

### `AccountName`

- Available since: version 1
- Type: String

The name of the account. If the value is `null` or the empty string, a reader must use the email address of the first
identity as the value of the account name.

A reader must use the email address of the first identity it is able to successfully read.

Note: This can lead to the reader using a different account name than the writer intended. But an unintended account
name is deemed preferable to the whole account having to be skipped because the reader doesn't support reading the first
identity.

### `Password`

- Available since: version 1
- Type: String

The password to use for authentication.

### `OutgoingServerGroups`

- Available since: version 1
- Type: Array

The array contains one or more `OutgoingServerGroup` elements.

### `OutgoingServerGroup`

- Available since: version 1
- Type: Array

This array contains the following elements in this order:

- `OutgoingServer`
- `Identity`

The array may contain additional `Identity` elements.

A reader must skip the `OutgoingServerGroup` if it fails to read the `OutgoingServer` or all `Identity` elements.

### `OutgoingServer`

- Available since: version 1
- Type: Array

The first element in this array is `OutgoingProtocol`. Its value determines the contents of the rest of the array.

Note: Since the size of this array depends on the value of `OutgoingProtocol`, future specifications can only add
properties on a per protocol basis.

### `OutgoingProtocol`

- Available since: version 1
- Type: Integer
- Values:
  - 0 (`SMTP`; available since: version 1)

For the value 0 the contents of the `OutgoingServer` array are as follows:

- `OutgoingProtocol`
- `Hostname`
- `Port`
- `ConnectionSecurity`
- `AuthenticationType`
- `Username`
- `Password` (optional)

For forward-compatibility a reader must skip reading the `OutgoingServerGroup` when an unsupported `OutgoingProtocol`
value is encountered. It must also skip the `OutgoingServerGroup` if any of the other properties contain unsupported
values.

#### Writing and reading `Password`

If the writer doesn't want to include the password, it may omit the `Password` element.

For forward-compatibility a reader must treat a `Password` value of `null` or the empty string like an omitted
password.

### `Identity`

- Available since: version 1
- Type: Array

This array contains the following elements in this order:

- `EmailAddress`
- `DisplayName`

A reader must skip this identity if any of the elements contain unsupported values.

Future versions of this specification may add additional values to this array.

### `EmailAddress`

- Available since: version 1
- Type: String

The email address to use for outgoing messages.

Currently only ASCII-only email addresses are allowed.

### `DisplayName`

- Available since: version 1
- Type: String

The name to use in outgoing messages.

## Examples

### One IMAP account

```json
[
  1,
  [1, 1],
  [0, "imap.domain.example", 993, 3, 1, "user@domain.example"],
  [
    [
      [0, "smtp.domain.example", 465, 3, 1, "user@domain.example"],
      ["user@domain.example", "Jane Doe"]
    ]
  ]
]
```

- Format version: 1
- Sequence: 1 of 1 (there's no other QR code to scan)
- Incoming server:
  - Protocol: `IMAP`
  - Hostname: `imap.domain.example`
  - Port: `993`
  - Connection security: `Tls`
  - Authentication type: `PasswordCleartext`
  - Username: `user@domain.example`
  - Password: _not present_
  - Account name: `user@domain.example` (implicitly defined via the email address of the first identity)
- Outgoing server:
  - Protocol: `SMTP`
  - Hostname: `smtp.domain.example`
  - Port: `465`
  - Connection security: `Tls`
  - Authentication type: `PasswordCleartext`
  - Username: `user@domain.example`
  - Password: _not present_
- Identity:
  - Email: `user@domain.example`
  - Display name: `Jane Doe`

### Two IMAP accounts

```json
[
  1,
  [1, 2],
  [
    0,
    "imap.company.example",
    993,
    3,
    6,
    "user@company.example",
    "user@company.example",
    ""
  ],
  [
    [
      [0, "smtp.company.example", 465, 3, 6, "user@company.example", ""],
      ["user@company.example", "Jane Doe"]
    ]
  ],
  [
    0,
    "imap.domain.example",
    993,
    3,
    1,
    "jane@domain.example",
    "Jane (Personal)",
    ""
  ],
  [
    [
      [0, "smtp.domain.example", 465, 3, 1, "jane@domain.example", ""],
      ["jane@domain.example", "Jane"]
    ]
  ]
]
```

- Format version: 1
- Sequence: 1 of 2 (there's one more QR code to scan)
- Incoming server:
  - Protocol: `IMAP`
  - Hostname: `imap.company.example`
  - Port: `993`
  - Connection security: `Tls`
  - Authentication type: `OAuth2`
  - Username: `user@company.example`
  - Password: _not present_
  - Account name: `user@company.example`
- Outgoing server:
  - Protocol: `SMTP`
  - Hostname: `smtp.company.example`
  - Port: `465`
  - Connection security: `Tls`
  - Authentication type: `OAuth2`
  - Username: `user@company.example`
  - Password: _not present_
- Identity:
  - Email: `user@company.example`
  - Display name: `Jane Doe`

---

- Incoming server:
  - Protocol: `IMAP`
  - Hostname: `imap.domain.example`
  - Port: `993`
  - Connection security: `Tls`
  - Authentication type: `PasswordCleartext`
  - Username: `jane@domain.example`
  - Password: _not present_
  - Account name: `Jane (Personal)`
- Outgoing server:
  - Protocol: `SMTP`
  - Hostname: `smtp.domain.example`
  - Port: `465`
  - Connection security: `Tls`
  - Authentication type: `PasswordCleartext`
  - Username: `jane@domain.example`
  - Password: _not present_
- Identity:
  - Email: `jane@domain.example`
  - Display name: `Jane`
