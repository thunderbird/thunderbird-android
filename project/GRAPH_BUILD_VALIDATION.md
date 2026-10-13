# Graph Integration Build Validation

Validated `main` at `c010b7a4ec3f21377e8340906c80a222919fbcb4` with Java 21.0.12.1, Gradle 9.7.0, and the Android SDK at `/home/ubuntu/Android/Sdk`. All four requested tasks passed: `:feature:account:server:settings:spotlessKotlinCheck`, `:backend:graph:check`, `:app-thunderbird:testFullDebugUnitTest`, and `:app-thunderbird:assembleFullDebug`. The full run reported `BUILD SUCCESSFUL` in 4m 46s; an incremental confirmation rerun also reported `BUILD SUCCESSFUL` and exited 0. The unit-test report contains 2 tests with 0 failures, errors, or skipped tests.

The verified debug APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (51,171,059 bytes). `apksigner verify` passed, and its signer certificate is `C=US, O=Android, CN=Android Debug`. SHA-256: `ffc98190ebd973ca4b04db4252bada356f651eda0be9f86d45ec3757bcf5f073`.

No `clean`, APK installation, Microsoft sign-in, commit, or push was performed. `/workspace/fairemail` was left untouched.

## Send parity and post-send crash fix — 2026-10-08

The complete user-provided `k9mail-logs.7z` archive (one log, 4,549,309 bytes, 29,647 lines) was scanned. It contained two repeated crash sequences: a successful-send flow queued `PendingAppend`, which called the Graph backend's unsupported `uploadMessage()` and let an `UnsupportedOperationException` escape. The backend contract now signals that Graph `sendMail` already saves Sent Items; the controller drops the temporary local Outbox copy and does not queue append work when upload is unsupported. The scan's raw log contents and private mail/account data are not reproduced here.

The focused tasks `:backend:graph:check` and `:legacy:core:testDebugUnitTest --tests com.fsck.k9.controller.MessagingControllerTest` passed. Repository checks and the fresh fullDebug build also passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew test lint detekt spotlessCheck :backend:graph:spotlessKotlinCheck :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 10m 37s
```

The new APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,761,346 bytes; package `net.thunderbird.android.debug`, version `26.0-SNAPSHOT`, versionCode `4`). `apksigner verify` passed. SHA-256: `d5f9ba2cbfc9847b7a9002146b6dce94e752e91e83face09b7d2eeac7420b840`.

The LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,252,456 bytes; method `LZMA2:25`). `7z t` passed, and extraction plus `cmp` confirmed the packaged APK is byte-identical. SHA-256: `85c5e4aaa4967a1d55476d08343421a781aa46a7c16b0296089a7e377fc405d2`.

`connectedAndroidTest` was not run; no Android device/emulator or live Microsoft mailbox session was used. No commit or push was made, and `/workspace/fairemail` was not touched.

## Account-setup initial-sync limit — 2026-10-08

Graph's first sync now uses the folder's configured `visibleLimit`, which is populated from the account-creation “Number of messages to display” selection. That count controls both the Graph `$top` request and the bounded set retained locally; an unset/non-positive folder value falls back to a positive `SyncConfig.defaultVisibleLimit`. Other backend implementations and `/workspace/fairemail` were not changed. A new regression test verifies a selected limit of 25 (rather than the former fixed ten); the focused Graph sync suite passes all 4 tests, and the regression failed against the pre-fix implementation.

The full repository checks and Thunderbird fullDebug build passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew test lint detekt spotlessCheck :backend:graph:spotlessKotlinCheck :backend:graph:check :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL
```

The APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,783,613 bytes; SHA-256 `ae633fe528a9977446ea920c59ea1449f9591eb53d953d6447c69dfce1cdcfaa`). `apksigner verify --verbose --print-certs` passed; signer DN: `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The one-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,269,925 bytes; method `LZMA2:25`; SHA-256 `0d4badc60947fcd1f482c4652fba19e067ecae4119678794923411603bd98fe7`). `7z t` passed; the archive contains only `app-thunderbird-full-debug.apk`, and extracting it and comparing with `cmp` confirmed a byte-identical APK.

No APK installation, Android device/emulator, live mailbox, commit, or push was used. `connectedAndroidTest` was not run; `/workspace/fairemail` was left untouched.


## Graph-specific setup default and fetch audit — 2026-10-08

New Graph account setup now defaults the per-folder message count to 10. The shared IMAP/POP defaults remain 100, and any count explicitly selected for a Graph account remains in effect. First sync and Fetch more use the folder's own visible limit; Fetch more follows opaque continuation pages as needed and retains overflow IDs between limit increments. Attachment metadata remains narrow, inline file `contentId` is read only from its typed resource, attachment bytes stay deferred, and attachment metadata pages follow the server's continuation URL.

Focused regressions passed: `GraphApiClientTest` (12 tests, including inline content-ID lookup and attachment metadata paging), `GraphBackendSyncTest` (5 tests, including the per-folder initial limit and multi-page Fetch more with preserved overflow), and `SyncOptionsStateMapperKtTest` (4 tests, including Graph=10, IMAP/POP=100, and preservation of an explicit Graph selection). The full account-setup unit-test task passed all 126 tests. The repository-wide checks and fresh fullDebug build passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck :backend:graph:spotlessKotlinCheck :feature:account:setup:spotlessKotlinCheck :backend:graph:check :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 2m 58s
```

The fresh APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,544,432 bytes; SHA-256 `ad258cea73a431bcb9220cea1eee33a4fa5aeb4182e980b9bd9a19331fa9e1f4`). `apksigner verify --verbose --print-certs` passed; the signer is `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The one-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,260,637 bytes; SHA-256 `3441b7d4b022e41a649d2faf7596f7659f8493a2d13175c827166625136b5215`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and extraction compared byte-for-byte with the APK.

No `clean`, APK installation, Android device/emulator, connected-device test, or live mailbox test was used. No commit or push was made, and `/workspace/fairemail` was not touched.

## Graph attachment-size persistence — 2026-10-08

The Graph API response's numeric `size` was already parsed into `GraphAttachment.size`, but the MIME adapter wrote it only to a Graph-specific header. Thunderbird's attachment extractor reads the standard `Content-Disposition; size=...` parameter instead. More importantly, the Graph MIME part carried an empty placeholder body; local import treated that as a real body and persisted its decoded length (`0`), which the UI then preferred over the header. The Graph MIME adapter now emits the standard size parameter and removes only bodies of parts carrying the Graph attachment-reference header. The existing missing-part import path consequently persists the metadata size, while `fetchPart()` continues to retrieve bytes lazily. Shared MIME, database, UI, IMAP, and POP code were not changed.

Regression coverage verifies Graph JSON `size` parsing and that a Graph attachment MIME part exposes the standard size parameter while keeping its body absent. The test failed before the final lazy-body fix and passes now. All 19 Graph backend tests pass (12 `GraphApiClientTest`, 5 `GraphBackendSyncTest`, and 2 `GraphMessageMimeFactoryTest`); Graph Kotlin formatting passes.

The complete repository checks and final Thunderbird fullDebug build passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk \
./gradlew --console=plain test lint detekt spotlessCheck \
  :backend:graph:spotlessKotlinCheck :backend:graph:check \
  :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 1m 13s
```

The final APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,546,533 bytes; SHA-256 `560e80133bd573825aa1012ca60320835168feb58cc93be871d3c212e8fe72a5`). `apksigner verify --verbose --print-certs` passed with one signer, `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The one-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,265,373 bytes; SHA-256 `52c184c22e796276cdfe4d8a0003459eccda24d5b0baf8866e63c0c2331486f8`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and extraction compared byte-for-byte with the APK.

No `clean`, APK installation, Android device/emulator, live mailbox, commit, or push was used. `connectedAndroidTest` was not run; `/workspace/fairemail` was left untouched.


## Graph attachment lookup and streamed-size validation — 2026-10-08

Graph MIME import now assigns the encoded opaque attachment reference to `Part.serverExtra` before saving, and `fetchPart()` prefers that same persisted key (retaining a custom-header fallback for older stored messages). When Graph metadata provides a non-negative size, the attachment stream is counted as the body factory consumes it. A mismatch raises `MessagingException`; the temporary body is removed before returning the failure. Graph-only regression tests cover lookup-key continuity, successful size validation, mismatch propagation, and temporary-file cleanup. No shared SQL/persistence, common UI, IMAP/POP, or `/workspace/fairemail` code was changed.

The focused attachment/MIME regressions passed, as did the full repository test and quality checks and the fresh Thunderbird fullDebug build:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck \
  :backend:graph:spotlessKotlinCheck :backend:graph:check \
  :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 1m 29s
```

The fresh APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,546,993 bytes; SHA-256 `192e544b7f8db11361073f69f86ec7b5205a4c77a5ef1636650aebbcd1a552f3`). `apksigner verify --verbose --print-certs` passed with one signer, `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The one-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,249,339 bytes; SHA-256 `432b703316ec41de3f7f3feb7b5f78ba873647b10cc39ad1c5cca69a7578d0a7`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and extraction compared byte-for-byte with the APK.

No `clean`, APK installation, device/emulator or live mailbox tests, commit, or push was performed. `connectedAndroidTest` was not run; `/workspace/fairemail` was left untouched.

## Graph attachment metadata versus streamed bytes — 2026-10-08

The complete supplied `log.7z` was extracted and scanned. The Android log contains two identical failures: the app's counter reported 2,129,655 bytes against Graph attachment metadata `size=2,129,959`, a 304-byte gap. The accompanying sync log records HTTP 200 success for both `/$value` requests. Neither log records an HTTP `Content-Length`, a captured response body, or a read/truncation exception; they therefore do not establish whether the difference comes from Graph metadata semantics or the delivered raw payload, and they do not show what the 304 bytes are.

The existing `GraphCountingInputStream` increments for single-byte reads, bulk reads, and skips. The attachment path's `DefaultBodyFactory` copies the stream without resetting it, so this counter measures bytes consumed by that factory; the supplied logs provide no evidence that the counter itself dropped 304 bytes. Graph attachment metadata size is now retained for display only. A numeric-only, identifier-free debug diagnostic records metadata-versus-stream count differences, while rejection and temporary-body cleanup are based only on a mismatch against a supplied HTTP `Content-Length`; body-factory/transport read exceptions remain unwrapped. The picker-before-download flow and non-Graph code are unchanged. Regression tests cover a 304-byte metadata over-report on a chunked response with no `Content-Length`, and a known `Content-Length` mismatch that rejects the body and removes its temporary file.

Focused Graph formatting/checks passed. The Graph test suite passed all 22 tests: 12 `GraphApiClientTest`, 5 `GraphBackendSyncTest`, 3 `GraphBackendAttachmentTest`, and 2 `GraphMessageMimeFactoryTest`. Repository-wide checks and the fresh fullDebug build also passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck \
  :backend:graph:spotlessKotlinCheck :feature:account:setup:spotlessKotlinCheck \
  :backend:graph:check :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 1m 59s
```

The fresh APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,547,902 bytes; SHA-256 `a95cb390e3eeb2482f719ccb5f9a72d07c341e1b86db25e8009a2365ec1e4a13`). `apksigner verify --verbose --print-certs` passed with one signer, `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The one-APK archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,275,826 bytes; SHA-256 `6e92b5931d1e6a484d0280a45e74bf9d46020fced27c8d6cbb553bafe5e2370d`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and extraction compared byte-for-byte with the APK.

No `clean`, APK installation, device/emulator, connected-device test, live mailbox, commit, or push was performed; `/workspace/fairemail` was not touched.

## Graph UI capability audit — 2026-10-08

The account setup, settings, folder, message-action, and attachment audit changes passed the full repository tests, lint, detekt, formatting checks, Graph backend check, and a fresh Thunderbird fullDebug build:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck \
  :backend:graph:spotlessKotlinCheck \
  :feature:account:setup:spotlessKotlinCheck \
  :feature:account:server:validation:spotlessKotlinCheck \
  :feature:account:server:settings:spotlessKotlinCheck \
  :feature:account:settings:impl:spotlessKotlinCheck \
  :legacy:ui:legacy:spotlessKotlinCheck \
  :backend:graph:check :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 4m 25s
```

The fresh APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,548,985 bytes; SHA-256 `48b61ba7a91919b911e09971a8ec01c7df9f3723a18c0c0916506d20e4e3bc76`). `apksigner verify --verbose --print-certs` passed with one signer, `C=US, O=Android, CN=Android Debug` (certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`).

The single-APK archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,264,609 bytes; SHA-256 `a78634c7f694851f83a9bee947ab3045d94e77302e346b66f75068fae6ef061d`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and extraction compared byte-for-byte with the APK.

No `clean`, APK installation, device/emulator, connected-device test, Microsoft sign-in, or live mailbox was used. No commit or push was made, and `/workspace/fairemail` was not touched.

## Graph-only Microsoft account reauthorization — 2026-10-08

The final account-edit, Incoming Server settings, and legacy persistence suites passed, including eligible Graph-only visibility and hidden-state cases (setup, IMAP, disabled gate, and missing OAuth configuration), successful OAuth-state replacement, cancel/authentication-failure preservation, Microsoft identity mismatch and partial-claim safeguards, stale-state rejection, save-failure rollback, and preservation of account/server/folder-sync metadata. Touched-module Detekt and formatting checks passed. The full repository validation and `fullDebug` build passed on this final source:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck :backend:graph:check :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 4m 5s
```

The final APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (62,141,187 bytes; SHA-256 `31e75e4b124126b5028a84d2e4f6671cc2f36144f204682900c5fdfa68b93898`). `apksigner verify --verbose --print-certs` passed with one signer, package `net.thunderbird.android.debug` version `26.0-SNAPSHOT` (versionCode 4), DN `C=US, O=Android, CN=Android Debug`; certificate SHA-256 `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`.

The one-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,312,913 bytes; SHA-256 `25d4d376114c8cd6b69c754dbd6cf3bb9f16143402ea3e2a9dfba54446cc8718`). It uses `LZMA2:25`, contains only `app-thunderbird-full-debug.apk`, passes `7z t`, and its extracted APK was verified byte-identical to the signed APK.

No `clean`, APK installation, Android device/emulator, connected-device test, Microsoft sign-in, live mailbox, commit, or push was used. `/workspace/fairemail` was not touched.


## Native Gmail and Zoho REST providers — 2026-10-09

Gmail REST (`gmailapi`) and Zoho Mail REST (`zohomailapi`) are integrated additively alongside Microsoft Graph (`msgraphapi`); legacy persisted Graph accounts retain their existing routing. Gmail uses Thunderbird's existing Google OAuth identity, recursive MIME handling, and deferred attachment bytes. Zoho accepts the user's own OAuth client ID/secret and Thunderbird package-specific redirect URI, validates allowlisted regional origins, and supports incoming-only accounts. Provider action labels/capabilities, delete behavior, message view and compose gates, and the server-state sync behavior are covered by regression tests. Initial API sync defaults to ten messages; Fetch more advances in batches of ten. IMAP/POP/SMTP paths remain intact. No FairEmail source or credentials were used for implementation.

The complete repository validation and fresh Thunderbird fullDebug APK build passed:

```text
ANDROID_HOME=/home/ubuntu/Android/Sdk ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
./gradlew --console=plain test lint detekt spotlessCheck \
  :backend:graph:check :backend:gmailapi:check :backend:zohomail:check \
  :app-thunderbird:assembleFullDebug
BUILD SUCCESSFUL in 4m 1s
```

The APK is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug.apk` (56,877,579 bytes; package `net.thunderbird.android.debug`, version `26.0-SNAPSHOT`, versionCode `4`). `apksigner verify --verbose --print-certs` passed with one signer; v1 and v2 signature verification succeeded. Signer DN: `C=US, O=Android, CN=Android Debug`; certificate SHA-256: `d9a78bd2eb850b90fa13591f4f0782613c93f2a618df5c672b51bf09d69b6af8`. APK SHA-256: `8a9acd17dc8d294a46945c3d6c6198fba7d6e25fdbb62d0d2ee275271dba9cf0`.

The single-APK LZMA2 archive is `/workspace/Thunderbird/app-thunderbird/build/outputs/apk/full/debug/app-thunderbird-full-debug-lzma2.7z` (45,390,584 bytes; method `LZMA2:25`; SHA-256 `e66d4b9ffbc5a0884ce1969291caacffe38998747497ae207949312239c89b75`). `7z t` passed; the archive contains only `app-thunderbird-full-debug.apk`, and extraction plus `cmp` verified byte identity with the signed APK.

This is a debug-key-signed test build, not a production-release signature. `connectedAndroidTest`, live Gmail/Zoho OAuth authorization, and live mailbox testing were not performed. No commit or push was made; `/workspace/fairemail` remained untouched.
