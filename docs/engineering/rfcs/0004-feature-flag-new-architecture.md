# RFC 0004: Adopt OpenFeature for the Feature Flag System

- Issue: [#11222](https://github.com/thunderbird/thunderbird-android/issues/11222)
- Technical design: [OpenFeature-based Feature Flag Provider]() TBD
- Status: **Proposed**

## Summary

Adopt the [OpenFeature](https://openfeature.dev) standard as the feature-flag *evaluation layer* for both apps (
Thunderbird and K-9 Mail) across all build_types. Replace the six per-app/per-built type `FeatureFlagFactory` classes
with
a single declarative **flag catalog**, and introduce a remotely-updatable, cacheable provider that supports staleness
checks and (later) gradual rollout and targeting.

Crucially, the existing `FeatureFlagProvider` / `FeatureFlagResult` API is **kept as-is**, so the ~30 call sites do not
change; OpenFeature sits behind that facade as an implementation detail.

## Motivation

Feature flags today are resolved **at build time**: each app per build_type combination ships its own
`FeatureFlagFactory` (`TbFeatureFlagFactory` for debug/daily/beta/release, `K9FeatureFlagFactory` for debug/release)
that hardcodes a list of booleans. This has real limitations:

- **No remote control.** A flag cannot be changed without shipping a new build; there is no remote definition, no cache,
  and no way to check whether an update is needed.
- **Duplication and drift.** A single flag is declared in up to six files; values diverge by accident, and only 3 of 12
  flags actually need to differ across variants.
- **Stringly-typed keys.** Some keys are raw strings; the registry is scattered, with a "do not add flags here" comment
  steering contributors toward per-feature `:api` modules.
- **No path to rollout/targeting.** There is no mechanism for percentage rollouts, cohorts, or A/B (split-testing)
  experiments.

We need a system that, across two apps and their build_types, can ship a default flag definition, fetch and cache a
remote
definition, detect when an update is needed, and leave room for gradual rollout and targeting, **without** locking into
a vendor or rewriting every call site.

## Proposal

### The Schema

```json
{
    "$schema": "featureflag.schema.lean.json",
    "version": "2026-06-18.1",
    "context": {
        "targeting_key": {
            "source": "random_install_id",
            "description": "Stable random per-install identifier for rollout/experiment bucketing. NOT a user identifier."
        },
        "attributes": [
            {
                "name": "app",
                "type": "string",
                "source": "BuildConfig (app module)",
                "values": [
                    "thunderbird",
                    "k9"
                ]
            },
            {
                "name": "build_type",
                "type": "string",
                "source": "BuildConfig.BUILD_TYPE",
                "values": [
                    "debug",
                    "daily",
                    "beta",
                    "release"
                ]
            },
            {
                "name": "variant",
                "type": "string",
                "derived": "{app}-{build_type}"
            },
            {
                "name": "app_version",
                "type": "string",
                "source": "BuildConfig.VERSION_NAME"
            },
            {
                "name": "os_sdk_int",
                "type": "int",
                "source": "Build.VERSION.SDK_INT"
            }
        ]
    },
    "flags": [
        {
            "key": "archive_marks_as_read",
            "default": true,
            "description": "Marks message as read when archived."
        },
        {
            "key": "disable_font_size_config",
            "default": true,
            "description": "Disables legacy font-size configuration UI."
        },
        {
            "key": "email_notification_default",
            "default": true,
            "description": "Default email-notification behaviour for new accounts."
        },
        ...
    ],
    "overrides": {
        "thunderbird": {
            "debug": {
                "display_in_app_notifications": true,
                "use_notification_sender_for_system_notifications": true,
                "message_view_action_export_eml": true
            },
            "daily": {
                ...
            },
            "beta": {
                ...
            },
            "release": {
                ...
            }
        },
        "k9": {
            "debug": {
                "message_view_action_export_eml": true
            },
            "release": {
                ...
            }
        }
    }
}
```

Where:

- `context`: the **evaluation-context contract**: the inputs the app must supply on every flag evaluation. It is both
  documentation and the codegen source for a typed context builder, so call sites cannot pass an undeclared attribute.
  - `targeting_key`: the bucketing key for gradual rollout and experiments. It is sourced from a stable, random
    per-install identifier; it is **pseudonymous, not a user identifier**, and under on-device evaluation it never
    leaves the device.
  - `attributes`: the targeting dimensions available to overrides and (later) rollout rules. Each declares a `name`, a
    `type`, and either where it comes from (`source`) or how it is computed (`derived`):
    - `app` / `build_type`: the former build-time matrix, now supplied as runtime context from `BuildConfig`. These
      two replace the six source sets.
    - `variant`: derived as `{app}-{build_type}`; a convenience key for override lookup and rule authoring.
    - `app_version` / `os_sdk_int`: version and OS dimensions reserved for future targeting (e.g. min-SDK gating or
      a staged rollout by app version).
- `flags`: the **flag registry**: the single source of truth for which flags exist. Each entry carries a `key` (the
  source for typed-key codegen), a `default` (the baseline value shipped with the app and returned when no override or
  remote value applies), and a `description`. Flags are boolean for now; the shape is designed to extend to multivariate
  and rollout later.
- `overrides`: per-`app` -> per-`build_type` deviations from a flag's `default`. Only the few flags that genuinely
  differ
  across variants appear here (today, 3 of 12), which removes the duplication where a single flag was declared in up to
  six files. A flag absent from an override block simply inherits its `default`. This is the offline equivalent of
  today's per-factory overrides, and it is the layer the bundled-catalog provider resolves against.

### Evaluation layer

Adopt OpenFeature as the evaluation layer, structured in **two layers behind the existing facade**:

1. **Keep the facade.** `FeatureFlagProvider.provide(key): FeatureFlagResult` remains the app-facing API. A new
   implementation delegates to the OpenFeature client. Call sites are untouched.
2. **One declarative catalog replaces the six factories.** A single, provider-agnostic file declares the flag registry,
   each flag's baseline value, the per-app/per-built type offline overrides, and the evaluation-context contract. It
   doubles as the **shipped default** and as the **codegen source** for typed keys. Flags are boolean for now; the
   format is designed to extend to multivariate and rollout later.
3. **Layered providers via `MultiProvider`** (priority order):
   `[debug overrides] -> [online provider] -> [bundled catalog]`.
   - The **bundled catalog** provider is the always-present offline baseline.
   - The **online provider** fetches, caches, and staleness-checks a remote copy of the catalog, and is where future
     targeting / rollout / experiments live.
   - The **override** provider preserves today's debug toggles.
4. **`app` and `build_type` become evaluation-context attributes** (plus `app_version`, OS, `targeting_key`, …), so the
   compile-time matrix becomes runtime targeting, and the two-apps/build_types requirement is met by context rather than
   by six source sets.
5. **Prefer on-device (local) evaluation** so rollout bucketing keys never leave the device.

The OpenFeature Kotlin SDK is multiplatform (Android/JVM/iOS/JS) and fits `core:featureflag`'s `commonMain`, which
already targets android + jvm. The choice of the **online** provider (an in-house flagd-compatible evaluator vs. a
managed vendor) is intentionally deferred; the OpenFeature abstraction makes it swappable without touching call sites.

**Decision requested:** agree to adopt OpenFeature as the evaluation layer, with the two-layer architecture (declarative
offline catalog + pluggable online provider) and the existing facade preserved. Implementation detail is in
the [technical design]().

## Alternatives Considered

- **Keep the build-time factories.** Rejected: cannot satisfy remote fetch, cache, staleness, or rollout; perpetuates
  the six-file duplication.
- **Build a fully bespoke flag + targeting system.** Rejected: reinvents OpenFeature, gains no ecosystem or tooling, and
  leaves us maintaining our own targeting engine and a non-standard API.
- **Adopt a managed vendor directly (no OpenFeature).** Rejected as the default: vendor lock-in for all, plus the
  privacy cost of remote evaluation (transmitting the targeting key/traits) for some.
- **Use the flagd-compatible flag-definition format instead of a lean custom catalog.** Seriously considered and kept
  open: it is the OpenFeature reference in-process format with a ready-made evaluator. We propose a lean,
  provider-agnostic catalog as primary because it is minimal and codegen-friendly, but OpenFeature makes the on-disk
  format swappable. The trade-off is detailed in the technical design.
- **Keep the existing `FeatureFlagProvider` facade only (no OpenFeature).** This is the most credible alternative: our
  facade already decouples the ~30 call sites and already allows swapping the implementation. It is rejected as the
  default because the facade alone does not provide layered resolution (`MultiProvider`), provider readiness/staleness
  events, or telemetry hooks — we would build and maintain those ourselves — and every future vendor integration would
  be bespoke glue against a private interface rather than a standard provider / OFREP (OpenFeature Remote Evaluation
  Protocol) boundary. OpenFeature is *the same facade plus* those primitives *plus* a reversible backend boundary.

## Risks & Drawbacks

- **OpenFeature Kotlin SDK is pre-1.0 (0.8.0).** Possible breaking changes; the recent `android-sdk` -> `kotlin-sdk`
  rename shows churn is real. Mitigated by keeping our own facade as the stable surface.
- **We must supply the online provider ourselves.** No candidate vendor ships a Kotlin OpenFeature provider yet, so the
  remote layer is our code regardless of in-house vs. vendor.
- **Remote configuration can change shipped behaviour.** Requires integrity verification (signing) and a security review
  before the remote layer ships.
- **`targeting_key` is a pseudonymous identifier.** Rollouts carry privacy implications; the posture depends on whether
  evaluation is local or remote.
- **Added dependency and concepts.** One more `commonMain` dependency and new concepts (providers, evaluation context,
  events) for contributors.
- **`Unavailable` is not native** to the simple value API; it must be recovered from evaluation details.

## Open Questions

- **Online layer:** in-house flagd-compatible evaluator, or a managed vendor (wrapped in a custom Kotlin provider)?
- **Hosting & integrity:** where is the remote catalog served, and must it be signed before it can flip behaviour?
- **Evaluation location:** local/in-process (privacy-preserving) vs. remote evaluation (transmits the bucketing key)?
- **Catalog shape:** a single catalog with `app` targeting, or one catalog per app?
- **Scope:** is multivariate (non-boolean) needed in the near term, or deferred?
- **Context contract placement:** the `context` block declares the *shape* of the evaluation context (attribute names,
  types, sources) — a build-time concern consumed by codegen, rule validation, and documentation. In OpenFeature the
  evaluation context itself is runtime data supplied per evaluation (global/client/invocation) and is never part of a
  flag definition; the contract is also tied to the app version (the app must be rebuilt to source a new attribute), so
  a remotely-fetched catalog cannot act on it. Should the contract therefore move out of the runtime catalog — into the
  JSON Schema (`featureflag.schema.lean.json`) or a clearly-labelled build-time section — and be renamed off `context`
  (e.g. `evaluationContextContract`) to avoid colliding with OpenFeature's `EvaluationContext`? Or does keeping it
  inline as the codegen source outweigh shipping it in the resolved payload?

## Outcome

Filled in when the RFC is accepted, rejected, or obsolete.

Summarize the final decision and link follow-up work.
