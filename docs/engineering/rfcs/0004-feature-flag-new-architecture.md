# RFC 0004: Add a Declarative Feature Flag Catalog

- Issue: [#11222](https://github.com/thunderbird/thunderbird-android/issues/11222)
- Technical design: [Technical Design 0002: Declarative Feature Flag Catalog](../technical-designs/0002-feature-flag-declarative-catalog.md)
- Status: **Accepted**

## Summary

Replace the six per-app/per-build type `FeatureFlagFactory` classes with one bundled declarative feature-flag catalog
for Thunderbird and K-9 Mail.

The first phase keeps the existing `FeatureFlagProvider` / `FeatureFlagResult` API and the current debug override
mechanism unchanged. It focuses on reducing duplication, validating or generating known keys, and making per-app and
per-build defaults explicit in a single local catalog.

Remote updates, OpenFeature integration, rollout targeting, and managed vendors are out of scope for this RFC. Those can
build on the same catalog in a later phase after privacy, security, and operational requirements are agreed.

## Motivation

Feature flags today are resolved **at build time**: each app per build_type combination ships its own
`FeatureFlagFactory` (`TbFeatureFlagFactory` for debug/daily/beta/release, `K9FeatureFlagFactory` for debug/release)
that hardcodes a list of booleans. This has real limitations:

- **Duplication and drift.** A single flag is declared in up to six files; values can diverge by accident, and only 3 of
  12 flags currently need to differ across variants.
- **Stringly-typed keys.** Some keys are raw strings; the registry is scattered, with a "do not add flags here" comment
  steering contributors toward per-feature `:api` modules.
- **Hardcoded defaults.** The effective default for a flag is embedded in app/build-type source sets instead of being
  visible in one declarative source.
- **No clean foundation for future rollout work.** Remote updates, targeting, and experiments need a single validated
  catalog before they can be designed safely.

We need a small first step that reduces current maintenance cost and creates a stable input for future work without
changing feature-flag call sites or introducing remote behaviour.

## Proposal

### The Catalog

Introduce one bundled, schema-validated catalog that declares:

- the complete boolean flag registry,
- each flag's bundled default value,
- per-app/per-build type deviations from that default, and
- the source for validating or generating typed keys.

The catalog is local-only in this phase. It is packaged with the app and cannot be updated remotely.

```json
{
    "$schema": "featureflag.schema.lean.json",
    "version": "2026-06-18.1",
    "flags": [
        {
            "key": "archive_marks_as_read",
            "default": false,
            "description": "Marks message as read when archived."
        },
        {
            "key": "disable_font_size_config",
            "default": true,
            "description": "Disables legacy font-size configuration UI."
        },
        {
            "key": "email_notification_default",
            "default": false,
            "description": "Default email-notification behaviour for new accounts."
        }
    ],
    "overrides": {
        "thunderbird": {
            "debug": {
                "archive_marks_as_read": true,
                "disable_font_size_config": true,
                "email_notification_default": true
            },
            "daily": { "disable_font_size_config": true },
            "beta": {},
            "release": { "disable_font_size_config": false }
        },
        "k9": {
            "debug": { "archive_marks_as_read": true },
            "release": { "disable_font_size_config": false }
        }
    }
}
```

Where:

- `flags`: the single source of truth for which flags exist. Each entry carries a `key`, a boolean `default`, and an
  optional `description`.
- `overrides`: per-`app` -> per-`build_type` deviations from a flag's `default`. Only flags that differ from the
  default appear here. A flag absent from an override block inherits its `default`.

The first catalog supports boolean flags only. Multivariate flags are deferred until there is a concrete use case.

The catalog does not contain an OpenFeature `EvaluationContext` or a runtime `context` block. App and build type are
known by app wiring and `BuildConfig`; they are used to select the relevant `overrides` entry when constructing
the local catalog. Any future evaluation context contract must be named clearly, for example
`evaluationContextContract`, and must not be part of a remotely fetched runtime payload unless a later design accepts
that requirement.

### Resolution

Keep `FeatureFlagProvider.provide(key): FeatureFlagResult` as the app-facing API. Call sites continue to request flags
the same way they do today.

The local catalog-backed implementation resolves a flag in this order:

1. Local debug overrides from `FeatureFlagOverrides`.
2. Bundled `overrides` for the current app/build type.
3. Bundled flag `default`.
4. `FeatureFlagResult.Unavailable`, only when the key is not present in the bundled catalog.

This preserves the current developer override behaviour while making the app/build-type defaults explicit and shared.

### Implementation Shape

The technical design should define the exact file location, schema, code generation or validation task, and migration
steps. The expected direction is:

- Replace the app/build-type factory duplication with a catalog-backed factory or provider.
- Validate that every override key exists in `flags`.
- Validate that each `overrides` entry references a supported app and build type.
- Generate or validate typed `FeatureFlagKey` declarations from the catalog so new keys cannot silently drift.
- Preserve the current debug settings UI and local override storage.
- Keep all feature-flag consumers behind the existing `FeatureFlagProvider` facade.

## Future Work: Remote Catalogs and OpenFeature

Remote configuration and OpenFeature adoption are a separate phase. That phase should have its own RFC or technical
design before implementation.

The later design must answer at least:

- **Evaluation location.** Initial remote-capable work should still be evaluated locally/in-process. The app must not
  send a rollout key or full evaluation context off-device until a separate privacy design is accepted and user consent
  is defined.
- **Rollout identifiers.** If a rollout key is introduced, the implementation must define how the key is generated,
  stored, reset, rotated, transmitted, logged, and deleted. Although such a key may be pseudonymous, it can become
  identifying when combined with app version, OS version, IP address, request timing, or other service-side metadata.
- **Service-side privacy.** Any remote service that receives or stores rollout keys, derived bucket identifiers, or
  evaluation context must follow equivalent privacy requirements, including retention limits, access controls, metadata
  minimization, and a prohibition on logging raw identifiers.
- **Integrity.** Remote catalogs must be signed before they can change behaviour. Unsigned, invalid, expired, or malformed
  catalogs must fall back to bundled defaults.
- **Hosting and staleness.** The design must define where catalogs are served from, how freshness is checked, and how the
  app behaves when the remote source is unavailable.
- **Vendor choice.** Start with an in-house local evaluator if remote-capable evaluation is needed. Managed vendors need
  a separate privacy and security review before adoption.
- **OpenFeature boundary.** OpenFeature may be useful as an evaluation/provider abstraction, but adopting it is not part
  of the local catalog phase.

If a remote phase is accepted, its intended precedence is:

1. Local debug overrides.
2. Remote overrides/defaults.
3. Bundled variant overrides/defaults.
4. `FeatureFlagResult.Unavailable`, only when the key is not present in any accepted catalog.

## Alternatives Considered

- **Keep the build-time factories.**
  - **Rejected:** cannot satisfy remote fetch, cache, staleness, or rollout; perpetuates the six-file duplication,
    scattered defaults, and key drift risk.
- **Keep the existing `FeatureFlagProvider` facade only, without a catalog.**
  - **Rejected:** the facade already decouples call sites from the implementation, but it does not create a single
    registry, validate keys, or remove duplicated per-app/per-build type definitions.
- **Adopt OpenFeature and remote updates in this RFC.**
  - **Rejected for the first phase:** remote updates and targeting require separate privacy, security, signing,
    hosting, staleness, identifier, and service-side data handling decisions. Bundling those decisions with the local
    catalog cleanup slows down the immediate maintenance win.
- **Use one catalog per app.**
  - **Rejected:** the current definitions are mostly shared across Thunderbird and K-9 Mail. A single catalog with
    app/build-type overrides better matches the white-label architecture and keeps shared defaults visible.
- **Add multivariate flags now.**
  - **Rejected:** there is no concrete use case. Boolean flags cover the current system and keep validation and
    migration smaller.

## Risks & Drawbacks

- **Schema and tooling overhead.** A catalog, schema, and validation or generation task add build-time machinery.
- **Incorrect shared defaults can affect both apps.** Centralizing defaults improves visibility but increases the impact
  of a mistaken value. Review and validation should make the current app/build-type matrix explicit.
- **Debug overrides must not regress.** The current debug settings UI and local override precedence need to be preserved.
- **Remote control remains unsolved in this phase.** This RFC intentionally creates the local foundation first. Remote
  behaviour needs follow-up design work before implementation.

## Open Questions

- Where should the bundled catalog and schema live?
- Should the first implementation generate typed keys, validate existing typed keys against the catalog, or do both?
- Should the catalog-backed implementation keep `FeatureFlagFactory` as the construction point, or introduce a new
  internal resolver behind `FeatureFlagProvider`?

## Outcome

Filled in when the RFC is accepted, rejected, or obsolete.

Summarize the final decision and link follow-up work.
