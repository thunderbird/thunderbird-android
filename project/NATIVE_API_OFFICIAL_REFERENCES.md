# Native API provider reference notes

Research captured 2026-10-08 from official Google Gmail API and Zoho Mail API documentation. These are implementation facts, not evidence that any OAuth app registration or consent is active.

## Gmail API

- Gmail's official scope table says `https://mail.google.com/` grants read, compose, send, and immediate permanent deletion; it is **restricted**, and Google says to request it only when immediate permanent deletion is required. `gmail.modify` allows read/compose/send but not immediate permanent deletion; `gmail.send` is sensitive; `gmail.readonly` is restricted. Scope use may require OAuth verification; restricted data stored/transmitted by servers may require a security assessment.
- `users.messages.list` is `GET https://gmail.googleapis.com/gmail/v1/users/{userId}/messages`; the default page is 100, maximum 500, with `nextPageToken`, `labelIds`, `q`, `includeSpamTrash`. List items contain only message `id` and `threadId`; full details need `messages.get`.
- The Gmail `Message` resource has immutable message ID, thread ID, label IDs, history ID, internal date, parsed MIME payload, size estimate and optional complete base64url RFC 2822 `raw`. A message's `threadId` can be set for replies, but Gmail requires matching `Subject` and RFC-compliant `References`/`In-Reply-To` headers.
- Gmail send posts a base64url-encoded RFC 2822 MIME value in the JSON `raw` property. Attachments may be represented as nested MIME parts; attachment bodies with an attachment ID require `users.messages.attachments.get`.
- Message methods differ materially: `trash` moves to Trash, `untrash` restores, `delete` immediately permanently deletes. Label membership is message-level; label IDs on different messages in a thread may differ.

Official sources:
- https://developers.google.com/workspace/gmail/api/auth/scopes
- https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages
- https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/list
- https://developers.google.com/gmail/api/guides/sending
- https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages.attachments/get
- https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.threads
- https://developers.google.com/gmail/api/guides/labels

## Zoho Mail API

- Zoho says native mobile apps should be registered as **Mobile-based Applications** and use Authorization Code with PKCE. Authorized redirect URIs must match the registered application. Offline access is required to receive a refresh token.
- Zoho's authorization callback includes authorization code, `location`, and `accounts-server`; the server response can provide `api_domain`. Regional routing must be derived from the account's verified Zoho response and allowlisted, not hard-coded to one `.com` DC.
- Mail API requests use `Authorization: Zoho-oauthtoken <token>` (not a generic Bearer header). Message APIs use `ZohoMail.messages` scopes; individual operations document narrower variants such as `.READ`, `.DELETE`, or `.ALL`.
- Email list, body, original MIME, attachment metadata/content, inline image, read/unread, move, flag, archive, spam, delete, send and reply are separate API operations. Original MIME retrieval is distinct from ordinary structured body/content retrieval.
- Delete uses `DELETE /api/accounts/{accountId}/folders/{folderId}/messages/{messageId}` with `expunge=false` (default) to move to Trash, and `expunge=true` for immediate permanent deletion.
- Inline-image retrieval is a separate `GET /api/accounts/{accountId}/folders/{folderId}/messages/{messageId}/inline` with required `contentId` and optional `fileName`, returning image bytes; it should be requested only when resolving a rendered inline image.

Official sources:
- https://www.zoho.com/mail/help/api/using-oauth-2.html
- https://www.zoho.com/mail/help/api/getting-started-with-api.html
- https://www.zoho.com/mail/help/api/email-api.html
- https://www.zoho.com/mail/help/api/delete-email.html
- https://www.zoho.com/mail/help/api/inline-image-download.html
- https://www.zoho.com/mail/help/api/get-all-folder-details.html
- https://www.zoho.com/mail/help/api/get-email-content.html
- https://www.zoho.com/mail/help/api/get-original-message.html
- https://www.zoho.com/mail/help/api/post-send-an-email.html
- https://www.zoho.com/mail/help/api/post-reply-to-an-email.html

## Configuration boundary

Thunderbird has an existing Google OAuth client configuration for IMAP/SMTP using `https://mail.google.com/` and its app redirect URI. This existing app-owned configuration is distinct from FairEmail's OAuth credentials, but source presence does not prove the Gmail API is enabled for that Google project or that the app's consent/verification covers a new REST route. Thunderbird contains no Zoho OAuth client/redirect/scopes/consent configuration. Do not invent or borrow provider credentials; do not run live account authorization during offline testing.


Additional official-contract details verified from the Zoho pages:

| Data-center hint | Zoho Mail API origin |
|---|---|
| `location=us` / `.com` | `https://mail.zoho.com` |
| `eu` / `.eu` | `https://mail.zoho.eu` |
| `in` / `.in` | `https://mail.zoho.in` |
| `.com.au` | `https://mail.zoho.com.au` |
| `.jp` | `https://mail.zoho.jp` |
| `.ca` | `https://mail.zohocloud.ca` |
| `.com.cn` | `https://mail.zoho.com.cn` |
| `.ae` | `https://mail.zoho.ae` |
| `.sa` | `https://mail.zoho.sa` |

The callback's `accounts-server` and the token response's `api_domain` are per-account data and must be persisted/validated against the provider's allowlist; the origin table is not permission to infer a region from a user's approximate location. Zoho's original-message endpoint is `GET /api/accounts/{accountId}/messages/{messageId}/originalmessage`; the documented response wraps the full RFC MIME string in JSON `data.content`, so the `.eml` path must unwrap, validate, and write the entire MIME representation only on explicit request. Zoho folder results provide opaque `folderId`, `folderType`, `folderName`, `path`, and IMAP visibility, so role mapping must use `folderType` rather than only localized display names. Sources: https://www.zoho.com/mail/help/api/getting-started-with-api.html, https://www.zoho.com/mail/help/api/get-original-message.html, https://www.zoho.com/mail/help/api/get-all-folder-details.html.

## Additional Zoho endpoint contracts verified 2026-10-08

- Message listing is `GET /api/accounts/{accountId}/messages/view?folderId={folderId}&start={1-based-index}&limit={1..200}&sortBy=date&sortorder=false`; the documented list response is in `data` and uses `messageId`, `receivedtime`, `subject`, `status`, `flagid`, `folderId`, and `threadId`. The API does not promise a `nextPageToken`, so bounded paging must advance the 1-based `start` index.
- Message content is `GET /api/accounts/{accountId}/folders/{folderId}/messages/{messageId}/content`; response is `data.content`. Attachment metadata is `GET .../{messageId}/attachmentinfo?includeInline=true`, with ordinary `data.attachments` and inline entries separately in `data.inline`; ordinary names include `attachmentId`, `attachmentName`, `attachmentSize`, while inline uses `attachmentId`, `attachmentName`, `attachmentSize`, `cid`. Selected attachment bytes are `GET .../{messageId}/attachments/{attachmentId}` and return an octet stream. Inline-image bytes use the separate documented `/inline?contentId=...` route.
- Read/unread and other message actions use `PUT /api/accounts/{accountId}/updatemessage` with JSON modes such as `markAsRead`, `markAsUnread`, `moveMessage`, and `setFlag`; move requires `destfolderId` and `messageId` as a JSON array. `setFlag` uses Zoho's `info`, `important`, `followup`, or `flag_not_set`; do not map Thunderbird's FLAGGED state onto an unsupported generic Boolean endpoint.
- Search is `GET /api/accounts/{accountId}/messages/search?searchKey=...&start=...&limit=...`; `searchKey` must use Zoho search syntax, and the API's default `receivedTime` cutoff is two minutes before now unless supplied. Provider-specific query syntax must be translated deliberately rather than treated as a generic substring search.
- Send is `POST /api/accounts/{accountId}/messages` with JSON fields `fromAddress`, `toAddress`, optional `ccAddress`/`bccAddress`, `subject`, `content`, `mailFormat` (`html`/`plaintext`) and `encoding`; this is not an RFC MIME/base64 request. Native reply is `POST /api/accounts/{accountId}/messages/{messageId}` with corresponding fields and `action:"reply"`. The linked official page lists OAuth scopes `ZohoMail.messages.ALL` or `.CREATE` for send/reply.
- Permanent deletion is `DELETE /api/accounts/{accountId}/folders/{folderId}/messages/{messageId}?expunge=true`; `expunge=false` moves to Trash. API calls use `Authorization: Zoho-oauthtoken <token>`.

Official sources: https://www.zoho.com/mail/help/api/get-emails-list.html, https://www.zoho.com/mail/help/api/get-email-content.html, https://www.zoho.com/mail/help/api/get-attach-info.html, https://www.zoho.com/mail/help/api/get-attachment-content.html, https://www.zoho.com/mail/help/api/inline-image-download.html, https://www.zoho.com/mail/help/api/put-mark-email-as-read.html, https://www.zoho.com/mail/help/api/set-flag-for-email.html, https://www.zoho.com/mail/help/api/move-email.html, https://www.zoho.com/mail/help/api/get-search-emails.html, https://www.zoho.com/mail/help/api/post-send-an-email.html, https://www.zoho.com/mail/help/api/post-reply-to-an-email.html, https://www.zoho.com/mail/help/api/delete-email.html.

## Zoho send and account discovery contracts verified 2026-10-08

Zoho account discovery uses `GET /api/accounts` and returns `data[]` entries with `accountId` plus `primaryEmailAddress`/`mailboxAddress`; selecting by the account's exact authenticated mailbox address avoids guessing. The callback/token response's `api_domain` must still provide the origin—`GET /api/accounts` does not justify inferring a data center. Account discovery is documented under `ZohoMail.accounts.READ` or `.ALL`, so it requires explicit consent in addition to message scopes. Attachment upload uses `POST /api/accounts/{accountId}/messages/attachments?fileName={name}&isInline={bool}` with raw bytes; its JSON `data` returns `storeName`, `attachmentPath`, and `attachmentName`, which must be placed in the send/reply JSON `attachments[]`. Send/reply bodies are message fields plus those uploaded attachment descriptors; reply posts to `/messages/{messageId}` with `action:"Reply"`. Sources: https://www.zoho.com/mail/help/api/get-all-users-accounts.html, https://www.zoho.com/mail/help/api/post-upload-attachments.html, https://www.zoho.com/mail/help/api/post-send-email-attachment.html, https://www.zoho.com/mail/help/api/post-send-an-email.html, https://www.zoho.com/mail/help/api/post-reply-to-an-email.html.


## Zoho form-driven OAuth setup update (2026-10-08)

The Zoho setup form is user-configured; no Zoho client ID, secret, token, or FairEmail URI is bundled. Official Zoho guidance identifies **Mobile-based Application + Authorization Code with PKCE** for native Android. PKCE replaces the client secret for that client type; therefore the requested Client Secret field is optional and is only used if a user supplies credentials for a registration that requires it. The app must never ship a client secret. OAuth requests must ask for offline access (`access_type=offline`) to obtain a refresh token. Zoho documents the callback's `location` and `accounts-server` as provider-returned regional data; map only recognized values to the allowlisted Mail API origins. Message/API scopes use Zoho's comma-separated syntax. Sources: https://www.zoho.com/mail/help/api/using-oauth-2.html and https://www.zoho.com/mail/help/api/email-api.html.

Thunderbird's existing merged AppAuth redirect receiver accepts the installed `${applicationId}` scheme, including flavor suffixes. The Zoho form defaults to `${applicationId}://oauth2redirect` and lets the user edit it; preserve the installed application ID as the URI scheme so the callback returns to the app. Zoho's current registration guide describes authorized redirect URIs as HTTP(S), so verify the accepted URI format for the selected client type in the Zoho console. Do not copy FairEmail's callback. On successful OAuth, derive the Mail API region only from Zoho's callback/token response and validate it against the existing official-origin allowlist before making REST calls.
