# Writing an S/MIME provider

This document specifies, normatively, what an Android app must do to act as
an S/MIME provider for Thunderbird via the API in `plugins/smime-api/`. It
is the implementer's guide; for the client-side counterpart see
[`plugins/smime-api/README.md`](../../plugins/smime-api/README.md), and for
the architectural rationale see
[ADR 0009](../architecture/adr/0009-smime-companion-app-architecture.md).

Use this document when:

- Reviewing a new provider for inclusion in the picker.
- Auditing the reference provider (CipherMail) against the contract.
- Considering a change to the wire protocol (bump `API_VERSION` if any rule
  below changes).

## Overview

A provider is an Android app that:

1. Declares a **bound service** implementing `ISmimeService`, discoverable by
   the `com.ciphermail.smime.api.ISmimeService` intent action.
2. Performs S/MIME operations on behalf of mail clients, with all key
   material and certificate state living **inside the provider's own
   process** — never crossing the IPC boundary.
3. Surfaces user interaction (passphrase, consent) via `PendingIntent`s
   returned in a `RESULT_CODE_USER_INTERACTION_REQUIRED` reply.

Conformance to this guide is mandatory. Clients (Thunderbird) trust the
provider for both the cryptographic outcome **and** for honest reporting of
that outcome — failing to comply is a security defect, not a usability one.

## 1. Manifest declarations

Add to your `AndroidManifest.xml`:

```xml
<service
    android:name=".YourSmimeService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.ciphermail.smime.api.ISmimeService" />
    </intent-filter>
</service>
```

Rules:

- The service **must** be `exported="true"` — clients live in another
  process.
- Do **not** guard the service with a custom `android:permission`. The
  obvious choice (a `normal`-protection bind permission) does not provide
  meaningful security — any app can declare `<uses-permission>` for it —
  and creates a real install-order trap: if a client app is installed
  before the provider, the system never grants the permission and binds
  silently fail until the client is reinstalled. Authorisation belongs in
  `handleCheckPermission` (see §3) where caller identity can be checked
  per-call.
- The intent-filter `action` **must** match the constant
  `SmimeApi.SERVICE_INTENT`.
- Any activity launched via `PendingIntent` for user interaction (e.g. a
  passphrase dialog) **must** be `exported="false"`. The PendingIntent
  carries the launch grant; the activity itself must not be directly
  invocable.

## 2. The AIDL contract

`ISmimeService.aidl` has exactly two methods:

```java
ParcelFileDescriptor createOutputPipe(in int pipeId);
Intent execute(in Intent data, in ParcelFileDescriptor input, int pipeId);
```

`createOutputPipe` is called *before* `execute` for any operation that
produces a bulk byte stream (decrypted MIME, signed/encrypted output). The
provider must:

- Use a fresh anonymous pipe (`ParcelFileDescriptor.createPipe()`) per call.
- Keep the read end internally, return the write end. The client reads from
  its own read end of the pair it created independently.
- Associate the pipe with `pipeId` so the subsequent `execute()` call can
  look it up. Pipe ids are scoped per-client; do not assume global
  uniqueness.

`execute` is the operation dispatch. The provider must:

- Inspect `data.getAction()` to route.
- Inspect `data.getIntExtra(EXTRA_API_VERSION, 0)` first. If unsupported,
  return `RESULT_CODE_ERROR` with `SmimeError.INCOMPATIBLE_API_VERSIONS` and
  do nothing else.
- Block until the operation completes or fails. Long-running operations are
  acceptable; the client is on a worker thread.
- Close the write end of the output pipe before returning. Failure to close
  leaks an FD on the client side and stalls its read loop.

## 3. Action-by-action behaviour

### `ACTION_CHECK_PERMISSION`

- **Purpose:** Let the client confirm consent without performing crypto.
- **Streams:** None.
- **Required behaviour:** On first call after install (or after the user
  revoked consent), return `RESULT_CODE_USER_INTERACTION_REQUIRED` with a
  `PendingIntent` for a consent activity. On subsequent calls, return
  `RESULT_CODE_SUCCESS`.
- **Anti-pattern:** Returning `SUCCESS` unconditionally without a consent
  step. The reference implementation has a `TODO` here today; new providers
  must not copy that gap.

### `ACTION_DECRYPT_VERIFY`

- **Streams:** Input = raw MIME bytes of an `application/pkcs7-mime` part
  or a `multipart/signed` part. Output = the decrypted/inner MIME content.
- **Required result extras:**
  - `RESULT_DECRYPTION` — `SmimeDecryptionResult`.
    - `RESULT_NOT_ENCRYPTED` for signed-only or plaintext inputs.
    - `RESULT_ENCRYPTED` after successful decryption.
  - `RESULT_SIGNATURE` — `SmimeSignatureResult`. Must be present whenever
    the input carried a signature, even if invalid. Use `RESULT_NO_SIGNATURE`
    when no signature was present.
- **Trust signal honesty:**
  - `RESULT_VALID_TRUSTED` only if the certificate chain validates **to a
    root in the user's trust store** at the time of the operation. Pinned
    or self-signed certs are `RESULT_VALID_UNTRUSTED`.
  - Expiry / revocation must be reported through their own codes, not
    masked as `INVALID_SIGNATURE`.
- **Failure modes:** If decryption fails because the user's keystore is
  locked, return `RESULT_CODE_USER_INTERACTION_REQUIRED` (see §4). Other
  failures return `RESULT_CODE_ERROR` with an appropriate `SmimeError`.

### `ACTION_SIGN_AND_ENCRYPT`

- **Streams:** Input = plain MIME message bytes. Output = wrapped S/MIME
  bytes ready for SMTP transport.
- **Required extras:** `EXTRA_USER_IDS` (recipient addresses).
- **Optional extras:** `EXTRA_SIGN` (default `true`), `EXTRA_ENCRYPT`
  (default `true`), `EXTRA_FROM` (the composing account's sender address —
  see below).
- **Required behaviour:**
  - The signing identity is selected by `EXTRA_FROM`. A provider that holds
    more than one signing identity **must** sign with the certificate
    matching `EXTRA_FROM` and **must not** fall back to a different
    identity's certificate. If signing was requested and no usable
    certificate matches `EXTRA_FROM`, return `RESULT_CODE_ERROR` — signing
    as the wrong identity is a fail-open defect, not a convenience. When
    `EXTRA_FROM` is absent (older client), a provider may fall back to a
    single configured default identity.
  - When `EXTRA_ENCRYPT` is `true` and any recipient lacks a certificate,
    return `RESULT_CODE_ERROR` with `SmimeError.NO_CERTIFICATE_FOR_RECIPIENT`.
    **Do not** silently send to a partial set or downgrade to sign-only —
    that is a fail-open defect.
  - When keystore is locked, return `RESULT_CODE_USER_INTERACTION_REQUIRED`
    immediately. Do not block on an inline prompt; the client process owns
    no UI on your behalf.

### `ACTION_GET_CERTIFICATES`

- **Streams:** None.
- **Required extras:** `EXTRA_USER_IDS`.
- **Required result extras:** `RESULT_CERTIFICATES` — one
  `SmimeCertificateInfo` per *input* address, in the same order, with
  `hasValidCertificate` set honestly. Returning a partial list violates the
  contract; the client uses index-aligned positions to drive its lock-icon
  state.
- **Performance:** Must complete within a few hundred milliseconds for typical
  recipient counts (≤10). Clients call this on every recipient-field change
  in the compose screen. Slow lookups will visibly stall typing.
- **Privacy:** The recipient list is shared with you. Do not log it, persist
  it, or transmit it off-device. This is the IPC boundary at which the user
  is implicitly trusting your provider.

### `ACTION_IMPORT_CERTIFICATE`

- **Streams:** Input = DER- or PEM-encoded certificate bytes.
- **Behaviour:** Import into the provider's certificate store. Duplicates
  should be deduplicated, not error.
- **No result Parcelables required**, only `RESULT_CODE`.

## 4. The user-interaction handshake

When an operation needs user input (locked keystore, missing consent), do
**not** block waiting for it. Instead:

1. Build a `PendingIntent` that, when fired, launches your provider-owned
   activity (e.g. passphrase dialog).
2. Set `FLAG_IMMUTABLE`. Never `FLAG_MUTABLE`. The client must not be able
   to mutate the launch intent.
3. Target an **explicit `ComponentName`**, not just an action.
4. Set the result extras:
   ```
   RESULT_CODE = RESULT_CODE_USER_INTERACTION_REQUIRED
   RESULT_INTENT = <the PendingIntent>
   ```
5. Return immediately.

The client launches via `startIntentSenderForResult`, the user completes the
interaction, and the client retries the original request from scratch. On
the retry, the precondition that prompted the prompt (e.g. cached
passphrase) must hold without further user input — otherwise the client will
loop.

If the interaction is async-broadcast-based (as in CipherMail's
`PASSPHRASE_BROADCAST_ACTION`):

- Register the receiver with `RECEIVER_NOT_EXPORTED` (Android 13+).
- Restrict the broadcast intent with `setPackage(getPackageName())`.
- On older Android, gate via a `signature`-level permission you own.
- Never carry passphrase material in an exported broadcast.

## 5. Version negotiation

`SmimeApi.API_VERSION` is `1` today. Providers **must**:

- Accept the current `API_VERSION` value.
- Return `SmimeError.INCOMPATIBLE_API_VERSIONS` for any unknown version.
- When `API_VERSION` is bumped (additive change), continue to accept the
  older value with reduced functionality where possible; bump only when a
  breaking change is unavoidable.

Adding new optional extras to an existing action is **not** a version bump.
Removing or changing the semantics of an existing action/extra is.

## 6. Security obligations

The provider is part of the user's TCB for email confidentiality and
authenticity. Concretely:

- **Caller identity.** For any operation that exposes private keys (sign,
  decrypt), the provider should verify the calling package via
  `Binder.getCallingUid()` + `PackageManager`. If your security model
  permits any caller (e.g. you intend to serve multiple mail clients),
  document that explicitly and surface the caller package in any consent
  UI.
- **No outbound network.** S/MIME operations must not touch the network as
  a side effect. OCSP / CRL fetching is acceptable only if explicitly
  enabled by the user and logged.
- **Key material at rest.** Private keys must be encrypted with a
  device-bound key (AndroidKeyStore) wrapping a user-passphrase-derived
  key. Reinstall correctly forces a re-import (the device-bound key is
  wiped).
- **Passphrase cache lifetime.** Cache should clear on logout, device lock
  (configurable), or explicit user action. Indefinite caching is a defect.
- **Logging.** No PII, no passphrases, no key bytes, no decrypted content
  in logs at any log level. Recipient lists are PII.
- **Trust signal honesty.** As detailed in §3 — never report a stronger
  signature/encryption status than was actually achieved.

## 7. Multiple providers on one device

The user may have more than one S/MIME provider installed. The picker in
`SmimeAppSelectDialog` enumerates everything matching the service intent.
Implications:

- Your service must be discoverable by `setPackage(...)`-targeted binds;
  do not require any custom auto-discovery handshake.
- Surface your application label and icon clearly — they are how the user
  distinguishes providers in the picker.
- Do not assume yours is the only provider Thunderbird talks to; do not
  store per-account state in your provider keyed by account identifiers
  beyond what the user explicitly configured.

## 8. Testing checklist

Before shipping a provider, verify against this checklist:

- [ ] `EXTRA_API_VERSION` missing → `INCOMPATIBLE_API_VERSIONS`.
- [ ] `EXTRA_API_VERSION` = 999 → `INCOMPATIBLE_API_VERSIONS`.
- [ ] `ACTION_GET_CERTIFICATES` with N addresses returns N `SmimeCertificateInfo` entries.
- [ ] `ACTION_SIGN_AND_ENCRYPT` with one missing recipient → `NO_CERTIFICATE_FOR_RECIPIENT`, no output produced.
- [ ] `ACTION_SIGN_AND_ENCRYPT` from an `EXTRA_FROM` with no matching signing certificate → `RESULT_CODE_ERROR`, not signed with another identity's certificate.
- [ ] Locked keystore → `RESULT_CODE_USER_INTERACTION_REQUIRED` + immutable `PendingIntent`, returned in <50 ms.
- [ ] Successful retry after unlock writes valid wrapped MIME to the output pipe.
- [ ] Output pipe is closed on every return path, including errors.
- [ ] Large messages (≥10 MB) round-trip without OOM on either side (pipe streams, no in-memory buffering of full payload).
- [ ] `ACTION_DECRYPT_VERIFY` on a tampered signature → `RESULT_INVALID_SIGNATURE`, not `RESULT_VALID_*`.
- [ ] `ACTION_DECRYPT_VERIFY` on an expired-signer message → `RESULT_CERT_EXPIRED`, not `RESULT_INVALID_SIGNATURE`.
- [ ] Recipient list passed via `EXTRA_USER_IDS` does not appear in any persistent log.

## 9. Submitting a new provider

We are not currently maintaining a registry of S/MIME providers. If you
ship a public provider that conforms to this contract, please:

1. Open an issue against `thunderbird-android` describing your provider,
   its package name, distribution channel, and signing identity.
2. Provide a brief security argument: who can use the provider, what
   guarantees it makes about caller verification, and how it handles the
   testing checklist above.
3. Update this document with a link from the "Known providers" section
   below.

### Known providers

- **CipherMail** (`com.ciphermail.android`) — reference implementation;
  source at <https://gitlab.com/ciphermail/ciphermail-for-android>.
