# RFC 0003: Render inline images in plain-text messages

- Issue: [#11179](https://github.com/thunderbird/thunderbird-android/issues/11179)
- Technical design: TBD
- Status: **Proposed**

## Summary

Display images in the message body when a message carries them as positionally placed parts rather than `cid:`
-referenced HTML images. Today these render only as attachments. This brings Thunderbird for Android in line with
Thunderbird Desktop, which displays such images inline. The behaviour is gated behind a new preference (default on),
mirroring Desktop's `mail.inline_attachments`. Additionally, this new behaviour must be secured under a feature flag
(`enable_inline_images_positional_body_message`)

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
`Content-ID`. An image like the one above satisfies neither condition, so it is shown solely as an attachment. The user
sees an attachment card/entry instead of the picture the sender placed in the message.

Thunderbird Desktop displays this image inline by default. The two products diverging on a common, real-world message
shape is a usability gap and the same message to look the same.

This is **not** a regression: the `cid:` inline path works correctly. It is a class of message that has never been
supported on the app.

## Proposal

Render any images which contain the header `Content-Disposition: inline`, that a message places among its body parts
inline, at its position, even when it has no `Content-ID`, and it is not referenced by any HTML.

Concretely:

1. When a message body contains an image part that is not already handled as a `cid:` attachment, display it inline in
   the rendered body at the position it occupies in the MIME structure. This applies to supported image types only, and
   images that contain the header `Content-Disposition: inline`; other media (PDF, video, ...) continue to be shown as a
   regular attachment because the message body cannot render them.

2. Introduce an Android equivalent of the desktop's `mail.inline_attachments` preference. When enabled (the default),
   images display inline as described. When disabled, behaviour escape hatch if they prefer a compact, attachment-only
   view, and matches the control Desktop already exposes.

3. Following Desktop's rule, an inline image that has a filename is shown **both** in the body and as an attachment
   chip (so it remains easy to find and save). An inline image with no filename is shown in the body only. This differs
   from how Android currently hides `cid:` inline images from the attachment list; see Risks & Drawbacks.

4. Scope to plain-text messages (no HTML alternative). The unambiguous, high-value case is a `multipart/mixed` (or
   similar) message with no HTML body. Messages that contain both an HTML alternative and loose inline images are out of
   scope for the first iteration; they are discussed in Open Questions.

The decision requested from reviewers: **adopt Desktop-parity inline image display for plain-text messages, gated behind
a default-on preference, with named images also listed as attachments.**

## Alternatives Considered

- **Strict, Apple-Mail-only heuristic.** Inline only images that exactly match the Apple Mail shape (explicit
  `Content-Disposition: inline`, sibling of text parts, no HTML). Lowest false-positive risk, but narrower than Desktop
  and would still render some messages differently from desktop. **Rejected** in favour of Desktop parity, which the
  preference makes safe to default on.

- **Inline broadly with no preference.** Simpler (no settings surface), but removes the user's ability to opt out and is
  a less faithful match to Desktop, which ships the toggle. **Rejected** because the toggle is cheap insurance against
  the breadth occasionally inlining something a user considered an attachment.

- **Hide all inline images from the attachment list (body-only).** Consistent with how Android treats `cid:` inline
  images today. **Rejected** because it removes the easy save affordance and diverges from Desktop, which lists named
  inline images.  ***Note**: TfA supports long-press to save on inline images, but it isn't as explicit as on Desktop.*

- **Do nothing.** Leave these images as attachments. **Rejected:** It is a visible cross-product inconsistency on a
  common message shape.

## Risks & Drawbacks

- **False positives.** Broad inlining may render something a sender or user thought of as an attachment in the body. The
  default-on preference mitigates this: users who dislike it can turn it off. Scoping to plain-text-only further limits
  exposure.

- **Behavioural divergence within Android.** Existing `cid:` inline images are hidden from the attachment list; named
  positional inline images will be listed. Two inline images could be treated differently in the list depending on how
  they were authored. This matches Desktop's actual behaviour, but is an internal inconsistency worth noting.

- **Large images.** Inlining a multi-megabyte photo affects body rendering cost. The chosen rendering path loads the
  image lazily (it is not embedded in the HTML document), which keeps this in line with existing `cid:` images.

## Open Questions

- **HTML + loose images.** When a message has both an HTML alternative and loose inline images, should the loose images
  be appended/inlined (as Desktop does), or should we trust the HTML and leave them as attachments? **Proposed:** out of
  scope for this RFC, follow-up issue.

- **Nested under `multipart/alternative`.** Positional images nested inside an alternative are an unusual shape. Exclude
  or handle? **Proposed:** exclude.

- **Reply/quote/forward.** Should positional inline images be reproduced when quoting a message (*Desktop has a separate
  preference for this*)? **Proposed:** out of scope for this RFC.

- **Preference placement and naming.** Where in Settings should the toggle live, and what should it be called?
  (Implementation detail deferred to the technical design, but the user-facing label needs product/UX input.)

## Outcome

_To be filled in when the RFC is accepted, rejected, or obsoleted. Summarize the final decision and link follow-up
work._
