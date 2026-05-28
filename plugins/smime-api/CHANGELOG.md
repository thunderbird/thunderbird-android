# Version history

## Version 1 — initial release

First public iteration of the S/MIME companion API. Defines the contract
between an S/MIME-aware mail client (e.g. Thunderbird) and an S/MIME provider
(CipherMail). Parallels the OpenPGP API surface.

Service binding intent action:
  * `com.ciphermail.smime.api.ISmimeService` (constant `SmimeApi.SERVICE_INTENT`)

Actions:
  * `ACTION_CHECK_PERMISSION` — caller asks the provider to confirm consent.
  * `ACTION_DECRYPT_VERIFY` — decrypt and/or verify a single MIME part.
  * `ACTION_SIGN_AND_ENCRYPT` — sign and/or encrypt an outgoing MIME message.
  * `ACTION_GET_CERTIFICATES` — query certificate availability for one or more
    recipient email addresses (used to drive compose-screen lock icons).
  * `ACTION_IMPORT_CERTIFICATE` — import a DER- or PEM-encoded certificate.

Request extras:
  * `EXTRA_API_VERSION` (int, required)
  * `EXTRA_USER_IDS` (String[])
  * `EXTRA_SIGN` (boolean, default true)
  * `EXTRA_ENCRYPT` (boolean, default true)

Result extras:
  * `RESULT_CODE` (`RESULT_CODE_SUCCESS`, `RESULT_CODE_ERROR`,
    `RESULT_CODE_USER_INTERACTION_REQUIRED`)
  * `RESULT_INTENT` (PendingIntent, when user interaction is required)
  * `RESULT_ERROR` (`SmimeError`)
  * `RESULT_DECRYPTION` (`SmimeDecryptionResult`)
  * `RESULT_SIGNATURE` (`SmimeSignatureResult`)
  * `RESULT_CERTIFICATES` (`SmimeCertificateInfo[]`)

Parcelables (all marked `PARCELABLE_VERSION = 1`):
  * `SmimeError`
  * `SmimeSignatureResult`
  * `SmimeDecryptionResult`
  * `SmimeCertificateInfo`

Helper classes:
  * `SmimeApi` — sync (`executeApi`) and async (`executeApiAsync`) execution
    wrappers; manages ParcelFileDescriptor pipes for streaming MIME data.
  * `SmimeServiceConnection` — service-binding lifecycle helper.

Bulk data (message bytes) transfers through ParcelFileDescriptor pipes rather
than Intent extras to keep large messages off the Binder transaction.
