# RFC 0001: Changelog System Replacement

* Issue: [#NNNN](https://github.com/thunderbird/thunderbird-android/issues/NNNN)
* Technical design: [Changelog System Replacement](../technical-designs/0001-changelog-system-replacement.md)
* Status: **Proposed**

## Summary

Replace the current `ckChangeLog` based in-app changelog with a project-owned changelog system.

The replacement will keep changelog data in generated, schema-validated JSON assets that are packaged with each app
target. The app will read a generated index and the referenced release-note assets at runtime while preserving the
existing Changelog screen and Recent Changes behavior.

## Motivation

The current implementation depends on `ckChangeLog`, which is no longer a dependency the project wants to keep for
in-app changelog behavior.

The current XML-based flow also leaves important behavior implicit. The app has to interpret changelog text at runtime
and infer categories from prose. That makes release-note generation, Android runtime behavior, and historical migration
harder to validate.

The project should own the changelog data format, validate generated output before packaging, and keep runtime behavior
simple and predictable.

## Proposal

Adopt a project-owned changelog format and runtime reader:

* Generate one changelog index file per app target and one JSON file per release.
* Validate generated and migrated changelog JSON against repository-owned schemas.
* Migrate existing XML changelog history into the JSON format.
* Move K-9 Mail changelog resources from the legacy shared `src/main` location to app-target source sets.
* Update the changelog feature to read the packaged index and release JSON assets instead of `ckChangeLog`.
* Preserve the existing Changelog and Recent Changes user experiences.
* Remove the `ckChangeLog` dependency after the replacement is complete.

The v1 format includes the metadata needed to generate and display the packaged in-app changelog: release identity,
release ordering, note text, note type, app-specific filtering, feature flag filtering, and issue or pull request
references.

Detailed schema fields, release tooling changes, XML migration, runtime behavior, rollout order, and verification belong
in the linked technical design.

## Alternatives Considered

### Keep `ckChangeLog`

This has the lowest short-term implementation cost.

This is not preferred because it is not maintained anymore and preserves the legacy XML flow.

### Keep XML and replace only the runtime parser

This removes the dependency but keeps the legacy xml artifact.

This is not preferred because the project still would not have a clear schema for generated changelogs. It also keeps the
app close to the current text-inference model instead of making note type explicit.

### Generate one aggregate file per target

This would keep runtime loading and validation simple because the app would read a single JSON resource.

This is not preferred because Thunderbird for Android and K-9 Mail have more than 15 years of changelog history, and
the existing changelog has required repeated pruning. A single packaged changelog file would recreate the same growth
pressure over time.

It would also differ from `thunderbird-notes`, where release notes are maintained as one file per release.

### Fetch changelog data at runtime

This would allow changelog changes without a new app release.

This is not preferred for this replacement. It would require hosted changelog infrastructure, a network endpoint, app-side
fetching and failure handling, and a data privacy declaration for the network access. The goal is to replace the packaged
in-app changelog implementation, not to introduce a network-delivered changelog service.

## Risks & Drawbacks

* Historical XML migration may classify some old entries differently than manual review would.
* If previous `ckChangeLog` seen state is not migrated, some existing users may see Recent Changes once after upgrade.
* The change touches release tooling and app runtime code, so it should be delivered in small reviewable steps.
* Generated assets can become stale if release documentation does not make the new JSON step explicit.

## Open Questions

* Should existing `ckChangeLog` seen state be migrated, or is one possible extra Recent Changes prompt acceptable?
* Which historical XML files and source branches must be migrated before `ckChangeLog` can be removed?

## Outcome

Filled in when the RFC is accepted, rejected, or superseded.

If accepted, implementation follows the linked technical design.
