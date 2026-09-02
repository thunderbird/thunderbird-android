# ADR 0010: Adopt project-wide Repository pattern

- Issue: [#11440](https://github.com/thunderbird/thunderbird-android/issues/11440)
- Status: **Proposed**

## Context

Data access is spread across the codebase and is often coupled to business logic. The current per-account database
model makes cross-account features, including unified inbox pagination and unified folders, difficult to implement and
prevents a gradual migration to a consolidated database.

Repository-shaped APIs are inconsistent. Some expose mutable state through setters, and `FolderRepository` combines
local queries, remote queries, push tracking, and folder preference mutation in one interface. New work must not
replicate these broad, data-source-shaped interfaces.

## Decision

Adopt focused, KMP-safe repository contracts project-wide. Repositories isolate domain code from data-source
implementations and may coordinate local storage, remote services, and synchronization. They do not contain UI logic.

Each repository contract must:

1. Have one coherent responsibility and one reason to change.
2. Be independent of data-source implementation details and platform types such as `android.*` and `Cursor`.
3. Receive all identifiers required to scope an operation explicitly. An account-scoped operation always receives an
   `AccountId`, even when it also receives an entity identifier such as `FolderId`. Repositories must not be bound to a
   single account or entity instance.
4. Be exposed from an `:api` module only when it is an intentionally shared, stable contract. Implementations, data
   sources, mappers, and database details remain internal, as defined by [ADR-0009](0009-api-internal-split.md).

Repository APIs use Coroutines. The project `Outcome` type and a domain-specific error type represent expected,
recoverable failures. Repository contracts do not expose database, HTTP, or platform exceptions.

The detailed [Repository Pattern guide](../../architecture/repository-pattern.md) defines naming, reactive and one-shot
API shapes, atomic updates, and examples. Migrate legacy access one use case at a time, introducing a focused
contract and error type first. Implementations or `:app-common` adapters preserve the contract while the backing data
moves to the consolidated schema.

## Outcomes

### Positive Outcomes

- Domain code can evolve independently from the current per-account database and its eventual consolidated replacement.
- Focused contracts avoid overly broad interfaces, make dependencies clearer, and are easier to fake in tests.
- Explicit account and entity identifiers make account filtering reviewable and support safe database consolidation.
- KMP-safe contracts can move to shared source sets without exposing Android implementation details.

### Negative Outcomes

- The migration touches many call sites and requires old and new data-access paths to coexist temporarily.
- `Outcome` and domain error types add API and call-site verbosity.
- A missed `account_id` filter can still fail silently. Implementation-level safeguards and tests remain necessary.

