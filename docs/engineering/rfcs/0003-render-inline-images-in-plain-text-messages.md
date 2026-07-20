# RFC 0003: Render inline images in plain-text messages

- Issue: [#11179](https://github.com/thunderbird/thunderbird-android/issues/11179)
- Technical design: TBD
- Status: **Accepted**

## Summary

Display images in the message body when a message carries them as positionally placed parts rather than `cid:`
-referenced HTML images. Today these render only as attachments. This brings Thunderbird for Android in line with
Thunderbird Desktop, which displays such images inline. **This behaviour must be secured under a feature flag
(`enable_inline_images_positional_body_message`) while it is being implemented and validated.**

## Motivation

Some senders attach images *inline* by position rather than by HTML reference. Apple Mail composing in plain text is a
common producer of this standards-based message shape, but **the behaviour is not Apple-specific**. A representative
message is a `multipart/mixed` whose children are, in order:

```
multipart/mixed  
├── text/plain   "Text before"  
├── image/jpeg   Content-Disposition: inline; filename=DSC01740.jpeg   (no Content-ID)  
└── text/plain   "Text after"  
```

There is no HTML part and no `cid:` reference. The sender intends that the image appear between the two text blocks.

Currently, the app only inlines images that are referenced from an HTML body via `cid:` and have a matching
`Content-ID`. **That existing path should continue to work unchanged.** An image like the one above satisfies neither
condition, so it is shown solely as an attachment. The user sees an attachment card/entry instead of the picture the
sender placed in the message.

Thunderbird Desktop displays this image inline by default. This divergence is a usability gap because the same message
does not look the same across products.

This is **not** a regression: the `cid:` inline path works correctly. It is a class of message that has never been
supported on the app.

## Standards References

- [RFC 2183, section 2.1](https://www.rfc-editor.org/rfc/rfc2183.html#section-2.1) defines `inline` as a disposition
  for body parts intended to be displayed automatically.
- [RFC 2046, section 5.1.3](https://www.rfc-editor.org/rfc/rfc2046.html#section-5.1.3) defines `multipart/mixed` as
  ordered independent body parts.
- [RFC 2392](https://www.rfc-editor.org/rfc/rfc2392.html) defines `cid:` URLs used by HTML bodies to reference MIME
  parts. This is the existing inline-image path and is distinct from this positional plain-text case.
- [RFC 2387](https://www.rfc-editor.org/rfc/rfc2387.html) defines `multipart/related`, which is relevant for HTML plus
  related resources but is not the plain-text positional case covered by this RFC.

## Proposal

Render supported images that have `Content-Disposition: inline` at the position where the message places them among its
body parts, even when the image has no `Content-ID` and is not referenced by any HTML.

Concretely:

1. When a message body contains an image part that is not already handled as a `cid:` attachment, display it inline in
   the rendered body at the position it occupies in the MIME structure. This applies to supported image types only, and
   images that contain the header `Content-Disposition: inline`; other media (PDF, video, ...) continue to be shown as a
   regular attachment because the message body cannot render them.

2. **Keep existing `cid:` inline image rendering unchanged.** This RFC adds support for positional plain-text inline
   images; it must not reinterpret HTML `cid:` references or `multipart/related` resources.

3. Treat inline images consistently in the user-facing attachment affordance. If an inline image has a filename, it is
   shown in the body and remains discoverable in the attachment UI, regardless of whether it was referenced by `cid:` or
   placed positionally. **An inline image with no filename is shown in the body only.**

4. **Scope to plain-text messages with no HTML alternative.** The unambiguous, high-value case is a `multipart/mixed`
   (or similar) message with no HTML body. Messages that contain both an HTML alternative and loose inline images are
   out of scope for the first iteration; see Non-goals.

The proposed decision is: **render standards-based positional inline images in plain-text messages, without adding a
user-visible setting, while keeping existing `cid:` rendering intact and presenting inline image attachments
consistently to users.**

## Alternatives Considered

- **Strict, Apple-Mail-only heuristic.** Inline only images that exactly match the Apple Mail shape (explicit
  `Content-Disposition: inline`, sibling of text parts, no HTML). Lowest false-positive risk, but narrower than Desktop
  and would still render some standards-based messages differently from desktop.
  - **Rejected** in favour of handling the standards-based shape directly.
- **Add a user-visible setting.** This would mirror Desktop's `mail.inline_attachments` preference and provide an
  opt-out for users who prefer an attachment-only view.
  - **Rejected** because the sender explicitly marked these parts as inline, users should not need to reason about
    sender implementation details, and the app is moving away from adding settings for behaviour that should work
    correctly by default.
- **Hide all inline images from the attachment list (body-only).** Consistent with how Android treats `cid:` inline
  images today.
  - **Rejected** because it removes the explicit save/discovery affordance. Thunderbird for Android supports
    long-press save on inline images, but that is less visible than the attachment UI.
- **Do nothing.** Leave these images as attachments.
  - **Rejected:** It is a visible cross-product inconsistency on a common message shape.

## Risks & Drawbacks

- **False positives.** Broad inlining may render something a sender or user thought of as an attachment in the body. The
  risk is limited by requiring `Content-Disposition: inline`, supported image types, and scoping the first iteration to
  plain-text messages without an HTML alternative.

- **Attachment-list noise.** Named inline images, such as logos or email signatures, may appear in the attachment UI.
  This increases clarity for saving user-visible images but can add noise on small screens. **The UI should rely on the
  existing attachment presentation rather than adding a separate inline-image setting.**

- **Large images.** Inlining a multi-megabyte photo affects body rendering cost. The chosen rendering path loads the
  image lazily (it is not embedded in the HTML document), which keeps this in line with existing `cid:` images.

## Non-goals

- **HTML + loose images.** Messages with both an HTML alternative and loose inline images are out of scope for the first
  iteration. Appending them may duplicate content or expose tracking assets, so this requires real samples and a
  follow-up proposal.

- **Nested under `multipart/alternative`.** Positional images nested inside an alternative are excluded. Alternatives
  are not content to render together; the renderer should pick the best supported alternative and ignore the others.

- **Reply/quote/forward.** This RFC only changes the message display. Reply, quote, and forward behaviour **should  
  remain unchanged and be handled in a follow-up proposal if needed.**

- **Per-account or per-message overrides.** No user-visible setting is proposed for this feature. Per-message state
  would be device-local and hard to sync or import/export, and per-account behaviour would make the same message render
  differently depending on where it is viewed.

## Outcome

_To be filled in when the RFC is accepted, rejected, or obsoleted. Summarize the final decision and link follow-up
work._
