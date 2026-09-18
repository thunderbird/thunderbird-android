# RFC 0009: UUIDv7 Identifier Migration

- Related RFC: [RFC 0007: Global Database](0007-global-database.md)
- Related technical design: [Global Database](../technical-designs/0003-global-database.md)
- Status: **Accepted**

## Summary

Adopt UUIDv7 generation for newly created UUID-backed domain identifiers through the shared identifier factory
abstractions. During the Global Database cutover, replace each existing real `AccountId` with a UUIDv7 value and migrate
its persisted references. Existing non-account identifiers remain valid.

This RFC owns identifier format, generation, and the account identifier conversion performed by the Global Database
migration.

## Motivation

The project currently generates UUID-backed identifiers through shared factory abstractions. UUIDv7 would provide
time-ordered generation, improving insertion ordering, temporal sorting, and operational debugging. However, existing
UUID values are already persisted and referenced across features and storage boundaries.

Changing generation is distinct from rewriting existing values. The Global Database cutover already inventories and
rewrites account-qualified mail references behind a migration gate. It provides the bounded opportunity to replace
existing account identifiers while retaining the old stores and settings until validation and cutover succeed.

## Proposal

### Ownership and module placement

- `AccountId` remains in `feature:account:api`.
- `FolderId` belongs to `feature:mail:folder:api`.
- `MessageId`, `ThreadId`, and `AttachmentId` belong to `feature:mail:message:api`.
- `BaseIdentifier`, `BaseUuidIdentifier`, `IdentifierFactory`, and `BaseUuidIdentifierFactory` remain in
  `core:architecture:api`.
- No `core:types:api` module is introduced by this RFC.

Domain identifiers are not generic database infrastructure. Generic identifier abstractions stay in core, while concrete
identifier types remain with their owning feature or domain. The Global Database uses this ownership model but does not
own UUID format policy.

### Rollout

UUIDv7 adoption proceeds in two steps:

1. `BaseUuidIdentifierFactory` generates UUIDv7 for newly created UUID-backed identifiers.
2. The Global Database migration assigns one UUIDv7 `AccountId` to each existing real account and uses a durable
   old-to-new mapping throughout import, validation, cutover, retry, and cleanup.

Factory parsing and equality remain compatible with existing UUID values. `UnifiedAccountId` remains the nil UUID
sentinel and is never generated or rewritten.

### Persisted account identifier consistency

Before importing mail, the migration generates and durably records each old-to-new account mapping. A retry reuses that
mapping. It does not generate another identifier for the same account. Replacement IDs are pairwise unique and must not
equal any legacy account ID or `UnifiedAccountId`. A collision fails the unpublished migration before cutover.

The migration copies every setting in the old account namespace to the corresponding new namespace and retains the old
settings before cutover. Each copied account remains disabled through its existing `enabled` setting while migration is
in progress, and the migration state retains whether the account was previously enabled. Imported mail rows, serialized
queues, and app-owned account-qualified data use the new `AccountId`. Android-owned and externally held references follow
the owner-by-owner rewrite, recreation, reset, or cutover-expiration policy in the technical design's
[persisted reference inventory](../technical-designs/0003-global-database.md#persisted-accountid-reference-inventory).
Before migration starts, the app informs the user which externally held references or Android-managed settings may be
reset or expire. After validation succeeds, cutover publishes the new account list, restores each account's previous
enabled state, and publishes the global database. Cleanup then removes old account settings, legacy mail storage, and
temporary migration mappings. Runtime code does not retain an old-ID compatibility lookup after migration completes.
The completion result identifies affected categories and recovery actions.

The migration gate prevents normal account and mail work from observing a partially switched profile. Recovery resumes
or completes the durable cutover phase after process termination. A failure before cutover leaves the old account list,
settings, and legacy mail storage authoritative.

## Alternatives Considered

### Keep existing UUID generation

This avoids all rollout work but does not provide UUIDv7's time-ordering properties for new identifiers.

### Rewrite all persisted UUIDs during the UUIDv7 factory rollout

This would provide immediate format consistency but combines a small factory change with broad, high-risk data
migrations across unrelated storage owners.

### Preserve existing account identifiers during Global Database migration

This avoids migrating account settings and other persisted account references. It also leaves historical accounts on
older UUID versions despite an existing one-time migration that already rewrites account-qualified mail data. The
bounded settings namespace copy and durable old-to-new mapping make replacement practical during that cutover.

## Risks & Drawbacks

- Account identifier replacement expands the Global Database cutover and requires a complete inventory of persisted
  account references.
- External references that cannot be rewritten expire at cutover and require an explicit safe fallback or reset policy.
- UUIDv7 generation must preserve uniqueness and correct ordering behavior under concurrent generation.

## Validation

- Unit tests cover UUIDv7 generation behavior in the shared factory abstraction.
- Compatibility tests verify parsing and equality for existing UUID values, UUIDv7 values, and `UnifiedAccountId`.
- Migration tests cover settings namespace copying, disabled staging, restoration of each account's previous enabled
  state, account order, multiple accounts, stable retry mappings, duplicate-target rejection, collisions with legacy and
  unified account IDs, failures before cutover, process termination during cutover, and post-cutover cleanup.
- Migration fixtures verify every inventoried account-qualified database and serialized reference follows its declared
  rewrite, recreation, reset, or cutover-expiration policy. This includes account preferences, pending commands, WorkManager
  requests, widgets, avatars, notification channels and active notifications, shortcuts, routes and saved intents, draft
  identity metadata, content-provider URIs, and legacy storage paths.
- UI tests verify that migration discloses possible resets and expirations before starting and reports affected categories
  plus recovery actions afterward.
- Import/export round-trip tests cover each format that contains identifier values.

## Outcome

The proposal was accepted. New UUID-backed identifiers use UUIDv7 through the shared factory abstractions. The Global
Database migration regenerates existing real account IDs as UUIDv7 and rewrites their persisted references before
cutover. `UnifiedAccountId`, portable identifiers, and protocol identifiers are not rewritten.
