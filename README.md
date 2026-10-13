# Thunderbird Native REST API Project

This repository contains source-code changes that add native REST-based mail-provider integrations to Thunderbird. It is intended for users who want to inspect the code, build it themselves, or adapt it for their own use. It is **not an official Thunderbird release**, and this project does not separately distribute a Thunderbird app or prebuilt app package.

The upstream project's original root README is preserved as [`readme_thunderbird.md`](readme_thunderbird.md).

## Implemented functionality

- **Gmail REST API:** Native Gmail mailbox synchronization, including labels and messages; message content and attachment handling; sending; and supported mailbox actions.
- **Zoho Mail REST API:** Native Zoho Mail synchronization for folders and messages, message content and attachment handling, sending, and supported mailbox actions.
- **Microsoft Graph REST API:** Native Graph-backed folder and message synchronization, message and attachment handling, and supported message operations, including sending.
- **Shared mail-client behavior:** Provider-specific account handling is integrated with Thunderbird's existing account, folder, message, and background-operation infrastructure.
- **Existing transports retained:** Existing IMAP and POP account paths remain available, including Gmail-hosted IMAP for accounts configured to use IMAP. The native Gmail REST path is additional; it does not replace those existing paths.

Exact capabilities and behavior depend on the provider and the operation supported by its API. The source tree also includes tests for provider-specific and shared functionality.

## Provider OAuth credentials

These integrations use **OAuth 2.0**, not a static API key. Create and use OAuth credentials from accounts and developer consoles that you control. Provider registration screens and consent requirements can change, so follow the linked provider documentation as well as the source configuration described below.

> **Use your own OAuth client credentials.** Do not rely on credentials that happen to be present in a checkout or upstream build. Client IDs identify an OAuth application; they are not passwords. Never publish a client secret, refresh/access token, signing key, or private keystore in source control. Native Android apps are public clients and cannot keep a bundled secret confidential.

### Gmail

1. Create a Google Cloud project, enable the **Gmail API**, and configure the Google Auth consent screen. For external/testing apps, set the intended audience and add test users as needed.
2. Create an OAuth client suitable for this Android app's authorization flow. The app currently requests `openid`, `profile`, and the broad Gmail scope `https://mail.google.com/`. Google classifies that Gmail scope as **restricted**; review Google's verification and user-data requirements before distributing a public build.
   > **Placeholder client ID:** Treat the Gmail API OAuth client ID included in this source as a dummy placeholder for your own build. It will not work until you configure your own Google OAuth client, enable the Gmail API, and grant the scopes listed above (`openid`, `profile`, and `https://mail.google.com/`).
3. For a `fullDebug` build, update the `clientId` in `createGmailConfiguration()` in [`app-thunderbird/src/debug/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt`](app-thunderbird/src/debug/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt). For other Thunderbird build types, update the matching `TbOAuthConfigurationFactory.kt` under `app-thunderbird/src/beta/`, `src/daily/`, or `src/release/` as appropriate.
4. Keep the OAuth callback configured in code and in the provider registration in sync. The current factory derives it from the application ID as `${applicationId}:/oauth2redirect`; for `fullDebug`, that resolves to `net.thunderbird.android.debug:/oauth2redirect`. Google's native-app redirect rules can depend on client type and platform; verify that the current supported flow accepts your app's callback, and update the app flow and registration together if required.

The Gmail OAuth entry is also used by the existing Gmail IMAP/SMTP OAuth path. Replacing it therefore affects Gmail accounts using either REST or the existing mail protocols. An Android client ID is public; do not add a Google client secret to the app.

Official guides: [Enable/configure Gmail API access](https://developers.google.com/workspace/gmail/api/auth/scopes), [configure Google OAuth consent](https://developers.google.com/workspace/guides/configure-oauth-consent), and [OAuth for installed/native apps](https://developers.google.com/identity/protocols/oauth2/native-app).

### Microsoft Graph

1. Register a new app in the [Microsoft Entra admin center](https://entra.microsoft.com/). Select supported account types that match the users you intend to allow.
2. Add the **Android** platform for the build you will run. For `fullDebug`, the application ID is `net.thunderbird.android.debug`. Register the package name and the signature hash for your own debug signing certificate, then use the exact redirect URI generated for that registration in the OAuth configuration.
3. Add the **delegated Microsoft Graph** permissions `Mail.ReadWrite` and `Mail.Send`. The first permits reading and changing the signed-in user's mail but **does not grant send permission**; `Mail.Send` is separate. User or administrator consent may be required by the tenant's policy.
4. For `fullDebug`, replace the client ID and, if your registration uses a different callback, the redirect URI in `createMicrosoftGraphConfiguration()` in [`app-thunderbird/src/debug/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt`](app-thunderbird/src/debug/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt). This is distinct from `createMicrosoftConfiguration()`, which configures the existing Microsoft IMAP/SMTP OAuth route.

In the current tree, the separate Graph OAuth mapping is present in the **debug** source set. If you need Graph in a beta, daily, or release build, add and validate the corresponding mapping for that source set and register the matching application ID/signing certificate/redirect; changing the debug entry alone does not configure those builds. Do not put a confidential client secret in the Android app.

Official guides: [Register an app](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app), [configure Android redirect/signature hash](https://learn.microsoft.com/en-us/entra/identity-platform/tutorial-mobile-app-android-prepare-app), and [Microsoft Graph permissions](https://learn.microsoft.com/en-us/graph/permissions-reference).

### Zoho Mail

1. In the [Zoho API Console](https://api-console.zoho.com/), register a **Mobile-based application** for a native Android client. This integration uses OAuth authorization code with PKCE; a mobile client should not depend on a secret embedded in the app.
2. Register the callback URI shown in Thunderbird's Zoho account setup screen exactly. The app fills a package-specific default. For `fullDebug`, it is `net.thunderbird.android.debug://oauth2redirect`; other variants have different application IDs. The URI registered with Zoho must match the URI saved in the account settings.
3. During Zoho account setup, select the **Zoho Mail REST API** incoming protocol and enter your Zoho account email, OAuth client ID, and callback URI. The password field is labeled for the client secret; leave it blank for a PKCE mobile client unless your Zoho registration specifically requires it. No source-code edit is needed to enter per-account Zoho credentials.
4. The integration requests these Mail API scopes: `ZohoMail.accounts.READ`, `ZohoMail.folders.READ`, `ZohoMail.messages.READ`, `ZohoMail.messages.CREATE`, `ZohoMail.messages.UPDATE`, `ZohoMail.messages.DELETE`, and `ZohoMail.folders.UPDATE`. Review the requested access before consenting. The app derives the regional Mail API origin from the Zoho authorization response rather than asking you to hard-code a regional endpoint.

If you are changing the Zoho setup UI or its OAuth behavior, the relevant code is under `feature/account/server/settings/src/main/kotlin/app/k9mail/feature/account/server/settings/ui/incoming/` and `feature/account/oauth/src/main/kotlin/app/k9mail/feature/account/oauth/`. Normally, create your own Zoho mobile client and provide its credentials in the account form instead of changing these source files.

Official guides: [Register a Zoho OAuth app](https://www.zoho.com/developer/oauth/register-app.html), [mobile/desktop OAuth with PKCE](https://www.zoho.com/developer/oauth/mobile-and-desktop-apps/overview.html), and [Zoho Mail OAuth](https://www.zoho.com/mail/help/api/using-oauth-2.html).

## Synchronization and privacy

- Initial synchronization fetches up to **10 messages per folder with synchronization enabled**.
- **Fetch more** loads **10 older messages per request**.
- API updates rely entirely on polling. **No webhooks are implemented**, for privacy reasons.

## Build a local Thunderbird debug APK

Use JDK 21 or later, install the Android SDK, and set `ANDROID_HOME` to the SDK location if your environment does not already expose it. From the repository root, build the full-flavor debug APK with the included Gradle wrapper:

```bash
./gradlew :app-thunderbird:assembleFullDebug
```

On Windows, use:

```bat
gradlew.bat :app-thunderbird:assembleFullDebug
```

The APK is produced under `app-thunderbird/build/outputs/apk/full/debug/`. To install directly to a connected Android device or emulator, use `./gradlew :app-thunderbird:installFullDebug` after accepting the device's USB debugging prompt.

## Important project notes

- This is a **source-code project** for inspection, source builds, and adaptation. It is not an official Thunderbird release and no separate app distribution is provided here.
- Native REST accounts are separate from IMAP/POP accounts. Existing IMAP and POP transport paths remain available.
- These REST integrations poll provider APIs; they do not use push notifications or webhooks. Initial sync and Fetch more use batches of 10 messages; normal API refresh behavior and available actions also depend on provider capabilities and account settings.
- OAuth access is subject to provider consent, account eligibility, Google verification requirements, and Microsoft organization policies. Creating a client ID alone does not grant mailbox access.
- Some operations differ by provider. Consult the provider API documentation and the implementation/tests before assuming a specific feature, deletion behavior, draft workflow, or search capability.
- Use and modification remain subject to the applicable project and upstream licenses. See [`LICENSE`](LICENSE).

- **Author:** [Anoop Bhatia](https://github.com/anoopspeaks)
- <strong>Tool used:</strong> <img src="assets/kern-badge.jpg" width="32" height="32" alt="Kern badge" align="middle"> <a href="https://cue.im/">Cue by Manus</a>
