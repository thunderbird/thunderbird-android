# Technical Design 0001: Changelog System Replacement

* Issue: [#11079](https://github.com/thunderbird/thunderbird-android/issues/11079)
* RFC: [Changelog System Replacement](../rfcs/0001-changelog-system-replacement.md)
* Status: **Proposed**

## Summary

This design replaces `ckChangeLog` with a feature-owned changelog reader backed by generated JSON raw resources.

The implementation consists of:

* A generated changelog index resource for each app target.
* Generated per-release changelog JSON resources.
* JSON schema validation before packaging.
* Migration from existing `changelog_master.xml` files.
* K-9 Mail changelog resource migration from `src/main` to app-target source sets.
* A changelog feature data source that reads the index and release resources.
* Preservation of the existing Changelog screen and Recent Changes behavior.
* Removal of `ckChangeLog` after all runtime references are gone.

## Current State

The app currently uses `ckChangeLog` to read XML changelog resources and expose releases to the Changelog and Recent Changes flows.

Relevant areas:

* `scripts/ci/render-notes.py`
* `scripts/ci/templates/changelog_master.xml`
* `app-k9mail/src/main/res/raw/changelog_master.xml`
* `app-thunderbird/src/*/res/raw/changelog_master.xml`
* `feature/changelog/internal`
* `feature/changelog/internal/src/main/kotlin/net/thunderbird/feature/changelog/internal/ChangeLogManager.kt`
* `feature/changelog/internal/src/main/kotlin/net/thunderbird/feature/changelog/internal/ChangelogViewModel.kt`
* `feature/changelog/internal/src/main/kotlin/net/thunderbird/feature/changelog/internal/RecentChangesViewModel.kt`
* `feature/changelog/internal/build.gradle.kts`
* `legacy/ui/legacy/build.gradle.kts`
* `gradle/libs.versions.toml`
* `docs/release/RELEASE.md`

The existing implementation supports:

* Full in-app changelog display.
* Recent Changes display after upgrade.
* Seen-state tracking.
* XML changelog assets.
* Generation of changelog XML from release-note tooling.

The replacement must preserve the user-facing behavior while changing the data format, release tooling, and runtime
reader.

## Proposed Design

At a high level, the packaged changelog resources move from one XML file per relevant source set to one generated index
plus one generated JSON file per release:

Before:

```text
app-k9mail/
`-- src/
    `-- main/
        `-- res/
            `-- raw/
                `-- changelog_master.xml

app-thunderbird/
`-- src/
    |-- daily/
    |   `-- res/
    |       `-- raw/
    |           `-- changelog_master.xml
    `-- debug/
        `-- res/
            `-- raw/
                `-- changelog_master.xml
```

After:

```text
app-k9mail/
`-- src/
    |-- release/
    |   `-- res/
    |       `-- raw/
    |           |-- changelog_index.json
    |           |-- changelog_release_8_2.json
    |           `-- changelog_release_8_1.json
    `-- debug/
        `-- res/
            `-- raw/
                |-- changelog_index.json
                `-- changelog_release_dummy.json

app-thunderbird/
`-- src/
    |-- release/
    |   `-- res/
    |       `-- raw/
    |           |-- changelog_index.json
    |           |-- changelog_release_21_0.json
    |           `-- changelog_release_20_0.json
    |-- beta/
    |   `-- res/
    |       `-- raw/
    |           |-- changelog_index.json
    |           `-- changelog_release_21_0_b1.json
    |-- daily/
    |   `-- res/
    |       `-- raw/
    |           |-- changelog_index.json
    |           `-- changelog_release_21_0_a1_2026_05_29.json
    `-- debug/
        `-- res/
            `-- raw/
                |-- changelog_index.json
                `-- changelog_release_dummy.json
```

The daily example includes the date because the daily version name suffix, `a1`, is constant. The generator should append
the `date` to the sanitized daily release resource name so daily builds do not collide.
Debug source sets package dummy changelog JSON files for testing the UI and do not mirror real release history.
The dummy release file should contain representative changelog content only; it is not generated from release notes and
must not be used for release, beta, or daily builds.

### Runtime Asset

Each app target packages one generated changelog index raw resource:

```text
R.raw.changelog_index
```

The index points to generated per-release raw resources. Android cannot scan arbitrary files in `res/raw`, so the index
is the app's discovery mechanism.

The layout matches `thunderbird-notes`, where release notes are maintained as one file per release. Generation can
transform each release source file into one packaged release resource and update the index separately.

Expected generated index files:

```text
app-k9mail/src/release/res/raw/changelog_index.json
app-thunderbird/src/release/res/raw/changelog_index.json
app-thunderbird/src/beta/res/raw/changelog_index.json
app-thunderbird/src/daily/res/raw/changelog_index.json
```

Expected debug dummy index files:

```text
app-k9mail/src/debug/res/raw/changelog_index.json
app-thunderbird/src/debug/res/raw/changelog_index.json
```

Expected debug dummy release files:

```text
app-k9mail/src/debug/res/raw/changelog_release_dummy.json
app-thunderbird/src/debug/res/raw/changelog_release_dummy.json
```

Expected per-release file naming:

```text
changelog_release_<version>.json
changelog_release_<version>_<date>.json
```

Example:

```text
app-thunderbird/src/release/res/raw/changelog_release_17_0.json
app-thunderbird/src/beta/res/raw/changelog_release_10_0_b1.json
app-thunderbird/src/beta/res/raw/changelog_release_10_0_b2.json
app-thunderbird/src/daily/res/raw/changelog_release_21_0_a1_2026_05_29.json
```

The release filename is derived from the release `version`. Daily release filenames also append the `date` because daily
builds keep the same `a1` version suffix throughout a release cycle. The generator must sanitize the version and date for
Android raw resource naming by lowercasing them, separating trailing prerelease labels from the numeric version with `_`,
and replacing characters outside `[a-z0-9_]` with `_`. For example, `10.0b1` becomes `10_0_b1` and `2026-05-29` becomes
`2026_05_29`.

K-9 Mail currently stores `changelog_master.xml` in `app-k9mail/src/main/res/raw`. The JSON replacement should change that
structure to be consistent with Thunderbird for Android during migration and switch to:

```text
app-k9mail/src/release/res/raw/changelog_index.json
app-k9mail/src/debug/res/raw/changelog_index.json
```

The index must list every packaged release resource. The app must not try to discover changelog files by scanning
`res/raw`.

### JSON Schema

Add schemas at:

```text
scripts/ci/schemas/changelog-index.schema.json
scripts/ci/schemas/changelog-release.schema.json
```

Use JSON Schema draft `2020-12`.

Index shape:

```json
{
  "schemaVersion": 1,
  "releases": [
    {
      "version": "21.0",
      "date": "2026-05-29",
      "url": "https://github.com/thunderbird/thunderbird-android/releases/tag/THUNDERBIRD_21_0",
      "resourceName": "changelog_release_21_0"
    }
  ]
}
```

Release file shape:

```json
{
  "schemaVersion": 1,
  "version": "21.0",
  "date": "2026-05-29",
  "url": "https://github.com/thunderbird/thunderbird-android/releases/tag/THUNDERBIRD_21_0",
  "notes": [
    {
      "type": "fixed",
      "text": "Fixed a crash when opening the message list",
      "issues": [12345],
      "pullRequests": [10301],
      "featureFlags": ["use_compose_for_message_reader"]
    }
  ]
}
```

Required index fields:

* `schemaVersion`: integer, must be `1`.
* `releases`: array of release index objects.

Required release index fields:

* `version`: non-empty string.
* `date`: string matching `YYYY-MM-DD`.
* `resourceName`: non-empty string matching the generated raw resource name for `version`, plus `date` for daily
  entries.

Optional release index fields:

* `summary`: non-empty string.
* `url`: URI string.

Required release file fields:

* `schemaVersion`: integer, must be `1`.
* `version`: non-empty string.
* `date`: string matching `YYYY-MM-DD`.
* `notes`: array of note objects.

Optional release file fields:

* `summary`: non-empty string.
* `url`: URI string.

Required note fields:

* `type`: enum, one of `new`, `changed`, or `fixed`.
* `text`: non-empty string.

Optional note fields:

* `id`: stable non-empty string.
* `issues`: array of positive integers.
* `pullRequests`: array of positive integers.
* `featureFlags`: array of non-empty strings.
* `source`: enum, one of `thunderbird-notes` or `ckchangelog-xml`.

The schemas must use `additionalProperties: false` for every object.

The index and release file for the same release must agree on `version` and `date`.

### Data Semantics

Release ordering:

* Releases sort by descending `date`.
* Recent Changes matches the full current app version name to the release `version` for beta and release builds.
* Recent Changes matches both the full current app version name and the current date for daily builds.
* Per-release resource names are derived from the release `version`, matching `thunderbird-notes`.
* Daily per-release resource names also include the `date` to avoid collisions while the daily version name suffix remains
  `a1`.

Note ordering:

* Notes render in the order they appear in the release file.
* Generation must preserve source note order from `thunderbird-notes`.
* For `thunderbird-notes` files with `groups`, generation must preserve source note order within each generated
  release file after filtering by release group.
* XML migration must preserve source XML note order after classification.

Note types:

* `new`: new user-visible functionality or newly supported capability.
* `changed`: behavior changes, improvements, removals, translations, and entries that cannot be confidently classified.
* `fixed`: bug fixes, crash fixes, regressions fixed, and security fixes.

Text rendering:

* v1 note text is plain text.
* Markdown rendering is out of scope for the first replacement.

Feature flags:

* Feature-gated notes must use explicit `featureFlags` metadata.
* Feature flags must not be inferred from note text.
* Packaged changelog JSON must contain only notes that are available for the generated app target.

URLs:

* Release URLs should point to GitHub releases when reliably constructible.
* XML migration must not invent URLs.

Issue and pull request references:

* `issues` maps from `thunderbird-notes` `issues`.
* `pullRequests` maps from `thunderbird-notes` `pull_requests`.
* These fields are the canonical link metadata for notes.
* XML migration must not invent issue or pull request references.

App-specific notes:

* `thunderbird_only: true` notes are emitted only for Thunderbird targets.
* `k9mail_only: true` notes are emitted only for K-9 Mail targets.
* Notes without app-specific metadata are emitted for both apps, subject to feature flag filtering.
* If both `thunderbird_only` and `k9mail_only` are true, generation must fail.

### JSON Generation

Extend the existing release-note tooling instead of adding runtime parsing.

Primary entry point:

```text
scripts/ci/render-notes.py
```

Generation behavior:

1. Read release notes from `thunderbird-notes`.
2. Create one release file for each entry in `release.releases`.
3. Use the release entry `version` and `release_date` as the generated release `version` and `date`.
4. For grouped beta notes, include only notes whose `group` belongs to the generated release entry.
5. Map release-note tags to `new`, `changed`, or `fixed`.
6. Prefer long-form note text for the packaged in-app changelog when both long-form and store-facing text are
   available.
7. Preserve note text, release summary, release URL, release date, issues, pull requests, source note order, and
   explicit feature flag metadata when available.
8. Filter app-specific notes for the generated app target using `thunderbird_only` and `k9mail_only`.
9. Filter feature-gated notes for the generated app target.
10. Write or replace each per-release changelog file.
11. Load the existing `changelog_index.json` when present.
12. Insert or replace release index entries by `version`.
13. Sort index entries descending by `date`.
14. Validate the index and release files against their schemas.
15. Write the target `changelog_index.json`.

Existing store-facing release-note outputs must continue to work during the transition. Store-facing outputs are not the
preferred source for the packaged in-app changelog when long-form content exists.

### Feature Flag Handling

Feature-gated changelog notes require explicit metadata in `thunderbird-notes`.

Relevant feature flag factory locations include:

```text
app-thunderbird/src/release/kotlin/net/thunderbird/android/featureflag/TbFeatureFlagFactory.kt
app-thunderbird/src/beta/kotlin/net/thunderbird/android/featureflag/TbFeatureFlagFactory.kt
app-thunderbird/src/daily/kotlin/net/thunderbird/android/featureflag/TbFeatureFlagFactory.kt
app-thunderbird/src/debug/kotlin/net/thunderbird/android/featureflag/TbFeatureFlagFactory.kt
app-k9mail/src/release/kotlin/app/k9mail/featureflag/K9FeatureFlagFactory.kt
app-k9mail/src/debug/kotlin/app/k9mail/featureflag/K9FeatureFlagFactory.kt
```

Generation behavior:

1. If a note has no explicit feature flags, emit it normally.
2. If a note has feature flags, resolve them for the generated app target.
3. If all required flags are enabled, emit the note with `featureFlags`.
4. If any required flag is disabled, omit the note from the packaged target JSON.
5. If a flag cannot be resolved, fail generation.

Runtime debug overrides do not affect packaged changelog JSON.

### XML Migration

Add a migration tool for historical XML changelog data.

```text
scripts/migrate-changelog-xml-to-json.py
```

The migration tool lives under `scripts` because it is repository-level migration tooling. It is separate from
`scripts/ci/render-notes.py`, which remains responsible for normal release-note generation from `thunderbird-notes`.

The migration tool must:

* Accept an explicit input XML path.
* Accept an explicit output resource directory.
* Parse XML with a structured XML parser.
* Convert releases and notes into schema v1 index and release files.
* Sort output deterministically.
* Validate output against the schemas.
* Report migration counts.

Example:

```bash
python scripts/migrate-changelog-xml-to-json.py \
  --input app-k9mail/src/main/res/raw/changelog_master.xml \
  --output-dir app-k9mail/src/release/res/raw
```

For K-9 Mail, migration reads the legacy `src/main` XML file and writes JSON output to the target source set that will
package it.

Migration classification should be deterministic and conservative:

* Prefixes such as `New:`, `Added:`, and `Add:` map to `new`.
* Prefixes such as `Fixed:` and `Fix:` map to `fixed`.
* Prefixes such as `Changed:`, `Updated:`, `Improved:`, and `Removed:` map to `changed`.
* Unmatched notes default to `changed`.

Migration must not invent URLs, issues, pull requests, feature flags, breaking-change metadata, or availability metadata.

### Android Runtime

The changelog feature owns the replacement runtime code.

Runtime requirements:

* Add internal serializable models for schema v1.
* Add a data source that reads `R.raw.changelog_index`.
* Inject the app version provider and use the full app version name, including build-type suffixes, for Recent Changes
  lookup. Daily builds must also match the current date.
* Resolve each `resourceName` from the index to the corresponding generated raw resource ID.
* Load per-release JSON files on demand for the Changelog screen.
* Load only the release file matching the current app version name for Recent Changes, and also matching the current date
  for daily builds.
* Replace `ChangeLogManager` internals without changing public navigation APIs.
* Throw an exception in debug builds when the index or a required release file is missing, has an unsupported schema
  version, or contains invalid JSON.
* Return an empty changelog in release, beta, and daily builds when the index or a required release file is missing, has
  an unsupported schema version, or contains invalid JSON.
* Use `net.thunderbird.core.logging.Logger` for non-PII diagnostics when decode fails.
* Map explicit JSON note types to the existing `NEW`, `CHANGED`, and `FIXED` UI categories.
* Keep the existing Compose UI layout. Adding new display fields requires a later design.

### Recent Changes

Preserve current Recent Changes behavior:

* Fresh install does not show the Recent Changes snackbar.
* Upgrade shows the snackbar once when Recent Changes is enabled, the release matching the current app version has
  notes, and that version has not been marked as seen.
* Opening or dismissing Recent Changes records the current app version as seen.

Seen state should be stored in feature-owned namespaced storage. Migration from `ckChangeLog` seen state is optional
until the RFC open question is resolved.

### Dependency Removal

After the JSON reader is active and verified:

* Remove `ckChangeLog` from changelog feature dependencies.
* Remove `ckChangeLog` from legacy UI dependencies once unused.
* Remove `ckChangeLog` from the version catalog.
* Remove obsolete XML runtime references.
* Keep or remove XML generation only according to the final release-tooling rollout decision.

Search terms for cleanup:

```text
de.cketti
ckchangelog
ChangeLog.newInstance
ReleaseItem
changelog_master
```

## Migration and Rollout

Implement the change in reviewable slices:

1. Add schema and validation tests.
2. Add JSON generation beside the existing XML generation.
3. Add XML migration tooling and migrate selected historical assets.
4. Replace the changelog feature runtime reader.
5. Remove `ckChangeLog` dependencies and obsolete runtime references.
6. Update release documentation to make JSON generation and validation part of the release flow.

JSON generation can run beside XML generation until the app runtime switches to JSON. After the runtime switch is
complete and verified, `ckChangeLog` can be removed.

## Testing and Verification

Schema validation:

```bash
python -m jsonschema -i app-k9mail/src/release/res/raw/changelog_index.json scripts/ci/schemas/changelog-index.schema.json
python -m jsonschema -i app-thunderbird/src/release/res/raw/changelog_index.json scripts/ci/schemas/changelog-index.schema.json
python -m jsonschema -i app-thunderbird/src/release/res/raw/changelog_release_21_0.json scripts/ci/schemas/changelog-release.schema.json
```

Schema tests should verify:

* Valid fixtures pass.
* Unknown fields fail.
* Missing required fields fail.
* Unsupported note types fail.
* Invalid URLs fail where URI format validation applies.

Generator tests should verify:

* Type mapping from `thunderbird-notes`.
* Invalid type rejection.
* Per-release file insert and replace by `version`.
* Index insert and replace by `version`.
* Resource name generation from `version`.
* Resource name generation from beta versions such as `10.0b1`.
* Multiple release files generated from one `thunderbird-notes` file with multiple `release.releases` entries.
* Grouped beta notes are emitted only to the matching generated release file.
* Descending release ordering by `date`.
* Source note order preservation.
* `issues` preservation.
* `pull_requests` to `pullRequests` mapping.
* Thunderbird-only note inclusion for Thunderbird targets.
* Thunderbird-only note omission for K-9 Mail targets.
* K-9 Mail-only note inclusion for K-9 Mail targets.
* K-9 Mail-only note omission for Thunderbird targets.
* Conflicting app-specific note metadata fails generation.
* Feature-gated note inclusion.
* Feature-gated note omission.
* Unresolved feature flags fail generation.
* Missing release resources referenced by the index fail validation.
* Schema validation before writing.

Migration tests should verify:

* XML releases and notes are parsed.
* Prefix classification works.
* Unmatched notes default to `changed`.
* Source XML note order is preserved after classification.
* Migrated index and release files validate against the schemas.

Android tests should cover:

* Valid JSON maps to existing release UI models.
* Missing or invalid index JSON returns an empty changelog and logs a non-PII diagnostic.
* Missing or invalid release JSON is handled defensively and logs a non-PII diagnostic.
* Recent Changes is not shown on fresh install.
* Recent Changes is shown once after upgrade when the release matching the current app version has notes.
* Opening or dismissing Recent Changes records the current app version as seen.

## Open Technical Questions

None.
