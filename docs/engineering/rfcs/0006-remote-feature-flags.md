# RFC 0006: Remote Feature Flags

- Issue: [#11253](https://github.com/thunderbird/thunderbird-android/issues/11253)
- Status: **Proposed**

## Summary

Adopt a hosted JSON Catalog consumed and cached by Ktor within the application

## Motivation

Whenever we have an issue that is related to a feature that is guarded by a feature flag, we can only disable that
feature by deploying a new version of the app.

Depending on the situation, this can have a negative impact on our reviews, which could be simply solved by providing
a Remote Feature Flag JSON catalog where the app could fetch from time to time.

## Proposal

A JSON Catalog should be hosted in a place of easy, with no authentication required, so the app can consume it once and
cache it.

This would allow us to toggle feature flags on and off whenever we need, without requiring the app to be published
again.

We should also allow users to disable the Remote Feature Flags by their wish, in case they don't want our app
communicating with an external host to fetch the data.

The app should also keep track of possible changes in the current remote feature flag, and react accordingly whenever a
flag has it's state changed.

This RFC propose to keep using our current architecture, without integrating or adding a new one at this time.

### Fallback Strategy

Local bundled defaults will always serve as the base, overridden only when a valid remote payload is parsed. If network
requests fail, timeout, or if the user disables remote fetching, the app safely defaults to the cached remote catalog
when present or to these local flags.

### Fetch Lifecycle & Frequency

The app should query on cold start, with a short timeout so app startup isn't blocked. If it can fetch the Remote
Catalog, it will be cached and used in the next cold start if no changes in the catalog are detected.

While the app is on foreground, it will query if the remote catalog has any changes, constrained by a minimum refresh
interval (e.g., no more than once every 15-30 minutes) to avoid spamming the host. This interval could also be
configured by the user, to avoid data usage.

## Alternatives Considered

- Use the OpenFeature SDK to implement the remote feature flags; **Rejected**: would add more complexity and wouldn't
  bring much value at this point, since we won't integrate any flag evaluation based on app's attributes at this moment.
- Use OkHttp for the transport layer; **Rejected**: The app is moving towards to a KMP direction. The most logical thing
  is to use KMP friendly libraries and OkHttp is currently Android only. We'll use OkHttp as underlying driver by Ktor
  configuration, though.

## Risks & Drawbacks

- Legal Team could advise not to use a remote feature flag because of possible by any privacy reason
- We may need to update our Terms of Use, depending on the Legal Team advise.
- Users could get frustrated by known we will reach an external service to fetch the Remote Feature Flag catalog;
  however this is mitigated by allowing them to disable it.

## Open Questions

- Should we use GitHub Pages as the hosting place for our Remote Feature Flag?
- Is it possible to request only if the remote catalog got updated? This would allow us to quickly verify the need for
  an update without downloading the whole catalog
  - Maybe checking either `ETag` or `Last-Modified` or both headers?
  - Maybe using HEAD request?
  - May require a full-fledged backend with at least two endpoints (GET, HEAD)
  - Use a Conditional GET request with the `If-None-Match` (or `If-Modified-Since`) header?

## Outcome

Filled in when the RFC is accepted, rejected, or obsolete.

Summarize the final decision and link follow-up work.
