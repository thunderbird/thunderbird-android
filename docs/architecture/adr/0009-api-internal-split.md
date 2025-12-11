# Feature/Core API/Internal split and dependency rules

- Issue: [#10197](https://github.com/thunderbird/thunderbird-android/issues/10197)
- Pull Request: [#10198](https://github.com/thunderbird/thunderbird-android/pull/10198)

## Status

- **proposed**

## Context

Thunderbird for Android uses a modular architecture. Many feature and core areas are split into `api` (public contracts)
and `impl` (implementation) modules, e.g. `:feature:account:settings:api` and `:feature:account:settings:impl`.

Over time, the main friction was ambiguity in naming and ownership:
- Inconsistent use of `api` in package names made it unclear whether a type was part of a public contract.
- Lack of a clear convention for naming feature-internal packages (`impl`, `internal`, or none) led to confusion.
- Team wasn’t aligned on what belongs in `api` (stable contracts) vs. what should stay internal to the feature.

To improve clarity and discoverability, we rename `impl` modules to `internal` and formalize module, package, and
content rules for both API and internal code — for both feature and core modules.

## Decision

- `:feature:*:api` modules define the public contracts exposed to other features.
- `:feature:*:impl` modules are renamed to `:feature:*:internal`, marking them as private implementation details.
- `:core:*:api` modules define public contracts exposed to feature and other core modules.
- `:core:*:impl` modules are renamed to `:core:*:internal`, marking them as private implementation details.
- Other modules must only declare dependencies on `:feature:*:api` or `:core:*:api` of other areas. Depending on
  `:feature:*:internal` or `:core:*:internal` from a different area is prohibited.
- Binding of contracts to implementations happens in central composition modules (application assembly): `:app-common`
  and the app-specific modules `:app-k9mail` and `:app-thunderbird`.

### What goes into API vs. Internal

Put only stable, intentionally shared contracts in `api`:
- Public interfaces and abstractions other features depend on (e.g., repositories, use cases, service interfaces).
- Data contracts exchanged across features (DTOs/value objects).
- Navigation contracts and events that other features can trigger/observe.
- DI entry points/interfaces needed by composition modules (`:app-common`, app modules).

Keep everything else in `internal`:
- Implementations of the above contracts (repositories, data sources, mappers, use case implementations).
- UI implementations, view models, and UI state models scoped to the feature.
- Feature-scoped DI wiring, modules, and factories.
- Experimental or volatile details that must not be exposed as public API.

Notes for core modules:
- The same rules apply. Core `api` exposes stable, shared infrastructure contracts (e.g., logging, networking abstractions, serialization, clock, crypto interfaces). Core `internal` contains their implementations and wiring.

### Module naming rules

- Standard two-module shape for a feature area:
  - `:feature:<area>[:<subarea>]:api`
  - `:feature:<area>[:<subarea>]:internal` (formerly `impl`)
- Standard two-module shape for a core area:
  - `:core:<area>[:<subarea>]:api`
  - `:core:<area>[:<subarea>]:internal` (formerly `impl`)
- If there are multiple implementation variants, suffix the `internal` module with a qualifier, e.g. rename
  `:feature:mail:message:export:impl-eml` to `:feature:mail:message:export:internal-eml`.
- Library/legacy modules may adopt this pattern later but are currently out of scope for this ADR.

### Package naming rules

- For features, in `:feature:*:api`, use `net.thunderbird.feature.<area>[.<subarea>]`, e.g.:
  `net.thunderbird.feature.account.settings`, `net.thunderbird.feature.mail.message.reader`.
- For features, in `:feature:*:internal`, mirror the API package structure but place all implementation under an
  `.internal` segment, e.g.: `net.thunderbird.feature.account.settings.internal`,
  `net.thunderbird.feature.mail.message.reader.internal.data`, `net.thunderbird.feature.mail.message.reader.internal.domain`.
- For core, in `:core:*:api`, use `net.thunderbird.core.<area>[.<subarea>]`, e.g.: `net.thunderbird.core.network`.
- For core, in `:core:*:internal`, mirror the API package structure and place implementation under `.internal`, e.g.:
  `net.thunderbird.core.network.internal`, `net.thunderbird.core.crypto.internal`.
- Multiple variants (several implementations of the same contract): reflect the module qualifier after the `.internal`
  segment.
  - Mapping: `:…:internal-<variant>` → package `…internal.<variant>`.
  - Feature examples:
    - Module `:feature:mail:message:export:internal-eml` → package root
      `net.thunderbird.feature.mail.message.export.internal.eml` (e.g., `…internal.eml.data`, `…internal.eml.domain`).
    - If a PDF exporter exists: `:feature:mail:message:export:internal-pdf` →
      `net.thunderbird.feature.mail.message.export.internal.pdf`.
  - Core examples:
    - Module `:core:network:internal-okhttp` → `net.thunderbird.core.network.internal.okhttp`.
    - Module `:core:crypto:internal-bouncycastle` → `net.thunderbird.core.crypto.internal.bouncycastle`.
  - Keep variant tokens lowercase and alphanumeric. Use dot separators for dimension/value patterns (see below). Avoid
    kebab-case in package names.
- Multi-dimension variants: when a variant expresses a dimension and a value, prefer `internal.<dimension>.<value>`.
  - Examples: `net.thunderbird.core.storage.internal.database.sqlite`,
    `net.thunderbird.feature.search.internal.engine.lucene`.
  - Prefer at most two levels under `.internal` for the variant part to keep packages readable.

> [!NOTE]
> - Avoid adding `.api` to package names for new code — the module already is the API.
> - Prefer small focused API packages with narrow sets of contracts; keep type stability in mind when promoting code to API.
> - API packages should remain variant-agnostic in almost all cases; concrete variants live under `.internal.<variant>`.

### Dependency rules (Gradle)

- Feature-to-feature dependencies must target `:feature:*:api` only.
- Feature-to-core dependencies must target `:core:*:api` only.
- Core-to-core dependencies must target `:core:*:api` only.
- Core modules must not depend on `:feature:*` modules.
- `:feature:*:internal` and `:core:*:internal` dependencies are only allowed from:
  - the same area’s `api` module (when strictly necessary), and
  - composition modules: `:app-common`, `:app-k9mail`, `:app-thunderbird`.
- Build logic will add a check that fails the build if a module depends on a `:*:internal` outside of these exceptions.

### Migration plan

1. Rename modules in Gradle from `impl` to `internal`:
   - Examples in `settings.gradle.kts` to update:
     - `:feature:account:avatar:impl` → `:feature:account:avatar:internal`
     - `:feature:account:settings:impl` → `:feature:account:settings:internal`
     - `:feature:mail:message:export:impl-eml` → `:feature:mail:message:export:internal-eml`
     - `:core:<area>:impl` → `:core:<area>:internal` (and for subareas accordingly)
2. Adjust `namespace` in `build.gradle.kts` and Kotlin/Java `package` declarations to include `.internal`.
3. Update imports and references after package moves.
4. Add build-plugin check to disallow external dependencies on `:feature:*:internal` and `:core:*:internal` (enforced
   in the root build).
5. Move all composition wiring (DI, factory bindings, navigation registrations) to `:app-common` or app modules.
6. When in doubt, prefer starting in `internal`. Promote types to `api` only once they’re needed and stable.

## Consequences

### Positive Consequences

- Explicit and discoverable boundary between public and internal code.
- Stronger enforcement of architectural intent; easier refactors.
- Package-level naming makes internal code clearly visible as non-public.
- Works cleanly with KMP source sets (common/platform-specific).
- Reduced confusion around whether a type is public or internal.
- Clear criteria for placing types in `api` vs. `internal` improves consistency.

### Negative Consequences

- Requires renaming existing modules and packages, plus updating imports.
- Slight learning overhead for contributors until the pattern becomes familiar.
- Temporary churn in open branches during the migration window.

