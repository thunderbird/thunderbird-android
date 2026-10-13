# Implementation notes

## Included

- A new `backend/graph` module with a Graph v1.0 REST client and a `Backend` adapter.
- A distinct `graph` entry in Thunderbird's backend factory map and a factory that uses the account's OAuth state through Thunderbird's existing token-provider abstraction.
- Polling only; initial window of ten per folder, one older page of ten per “Fetch more” request, uncapped delta refresh, and lazy file/item attachment bytes.
- Graph-supported Move/Copy, read/unread and follow-up flag changes, mark-all-read, delete, and MIME send. Message Move/Copy return the destination server IDs expected by Thunderbird.
- Send requests use a 120-second write timeout. No-response network I/O failures are recorded as permanent outcome-unknown errors, preventing automatic resend while retaining the message in the Outbox; the user is told to check Sent Items before retrying.
- The backend contract exposes `savesSentMessagesToServer`; Graph sets it because `sendMail` stores the sent copy. The controller drops the temporary local Outbox copy instead of queuing a duplicate `PendingAppend`, and queues appends only for backends that advertise upload support.
- MockWebServer regressions cover token refresh, continuation URL validation, attachment metadata, bounded sync/delta behavior, send MIME encoding and ambiguous failure handling, Move/Copy, read/star updates, deletion, and unread listing.
- A controller regression verifies that a backend which already saves Sent Items does not trigger the unsupported append path.

## Not changed

- `/workspace/fairemail` remains a read-only reference and is not modified.
- Existing IMAP and POP backend code and routing are unchanged.
- Thunderbird's current Microsoft IMAP/SMTP OAuth client IDs, redirect URIs, and scope lists are unchanged.
- No remote push, upstream branch update, or commit was made.

## Native REST provider enablement and remaining authorization boundary

- New accounts have distinct IDs `gmailapi`, `zohomailapi`, and `msgraphapi`. Existing Microsoft accounts persisted as `graph` remain routable and keep that legacy ID on edits; the native Graph backend and Microsoft-only reauthorization remain unchanged.
- Gmail uses Thunderbird's existing Google OAuth identity and its already-configured `https://mail.google.com/` scope; that scope can authorize Gmail REST, but Gmail API enablement and consent/verification for the current Google project were not confirmed through live sign-in.
- Zoho has a user-configured form for sender name/email, client ID, optional client secret, package-specific `${applicationId}:/oauth2redirect`, incoming-only mode, and existing-account reauthorization. It uses Zoho mobile OAuth with PKCE, requests offline access, and derives the regional Mail API origin only from allowlisted Zoho callback/token data. No Zoho client registration, secret, token, or FairEmail URI is bundled.
- All three native REST providers poll; no webhook/push path is introduced. Initial visible counts use the user's account-creation selection (default 10 for API accounts), normal and forced refresh are uncapped, Fetch more is older pages of ten, and per-folder sync/body-download preferences remain authoritative. Attachment bytes stay deferred according to provider-specific MIME behavior; complete raw MIME is requested only for explicit EML export.
- Provider-specific menu/capability behavior, folder roles, message mutations, native send/reply, and unsupported controls are documented in the native API audit. Legacy IMAP/POP/SMTP setup and transport paths remain intact; REST accounts do not use those bridges.

No Gmail or Zoho account was authorized and no live mailbox was accessed during implementation. The app-owned Google OAuth identity is present, but its API project/consent readiness is unknown; Zoho requires users to register their own mobile OAuth client and consent to the requested scopes. This is an expected user-testing boundary, not a fabricated authorization success.

## Post-send crash diagnosis

The entire `k9mail-logs.7z` archive was scanned: one log file, 4,549,309 bytes, 29,647 lines. It contained two repeated crash sequences (four fatal/uncaught markers): after send, `MessagingController` processed a `PendingAppend`, called `GraphBackend.uploadMessage()`, and hit `UnsupportedOperationException`; that exception escaped the controller's expected `MessagingException` handling and crashed the process. The fix uses the backend capability above to skip this append and discards only the temporary local copy, allowing the already-saved remote Sent Items message to appear through sync. No raw log lines or private mail/account data are included here.

## Verification

`./gradlew :backend:graph:check` and `./gradlew :legacy:core:testDebugUnitTest --tests com.fsck.k9.controller.MessagingControllerTest` passed, including the new send-ambiguity and sent-copy regressions. The repository-required command `./gradlew test lint detekt spotlessCheck :backend:graph:spotlessKotlinCheck :app-thunderbird:assembleFullDebug` passed in 10m 37s. `connectedAndroidTest` was not run; this validation used no Android device/emulator or live mailbox.

The fresh APK signature and the LZMA2 archive integrity were verified, and the archived APK matches the build byte-for-byte. Artifact paths, sizes, package/version, and SHA-256 checksums are recorded in [build validation](GRAPH_BUILD_VALIDATION.md).

Use `/home/ubuntu/Android/Sdk` through `ANDROID_HOME` and `ANDROID_SDK_ROOT` for Gradle invocations; no project-local SDK configuration is added. Do not sign in to Microsoft or perform mailbox actions during validation.

Thunderbird remains on `main` at the same revision as `origin/main` (`c010b7a4ec3f21377e8340906c80a222919fbcb4`); the working changes are uncommitted and no remote push or upstream update was made. FairEmail was used only for read-only reference and was not written to by this task.


## Gmail and Zoho REST completion — 2026-10-09

- Implemented native REST backends under provider IDs `gmailapi` and `zohomailapi`; preserved `msgraphapi`, legacy `graph` account routing, and the baseline IMAP/POP/SMTP paths.
- Gmail uses Thunderbird's existing Google OAuth identity. Its recursive MIME traversal and lazy attachment retrieval preserve message composition/threading behavior while API sync and mutations use server-authoritative state.
- Zoho setup asks for the user's own client ID/secret and uses a Thunderbird-package-specific OAuth redirect URI. Regional origins are allowlisted; incoming-only accounts are gated from outgoing actions. Provider-specific action labels and permanent-delete capabilities are applied only where supported.
- API account setup defaults to ten messages; Fetch more uses batches of ten. Regression coverage spans API clients, account setup/settings, message actions, and incoming-only UI guards.
- The final `test lint detekt spotlessCheck`, Graph/Gmail/Zoho backend checks, and `:app-thunderbird:assembleFullDebug` all passed. The signed fullDebug APK, debug signer identity, LZMA2 archive, sizes, and SHA-256 checksums are recorded in [build validation](GRAPH_BUILD_VALIDATION.md).
- Validation did not perform `connectedAndroidTest`, live OAuth authorization, or live mailbox actions. No commit/push was made; `/workspace/fairemail` remained read-only and unchanged.
