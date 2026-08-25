# ADR 0010: Adopt project-wide Repository pattern

- Issue: [#11440](https://github.com/thunderbird/thunderbird-android/issues/11440)
- Status: **Proposed**

## Context

One of our main pain points in the app is how we consume the data. The current implementation consists of one
database per account, which creates several challenges such as not being able to properly paginate the unified
inbox, or having unified folders.

Besides, the access to the data is spread across the codebase, with a lot of coupled business logic, making it hard
to maintain and impossible to scale.

## Decision

To remove the business logic and data access coupling, while allowing us to migrate to a new — consolidated — database,
we decided to adopt the Repository pattern project-wide, where each Repository, in order of priority:

1. has one reason to change (non-negotiable)
2. is agnostic of how it consumes the data
3. is KMP-safe (no `android.*`, no `Cursor`)
4. has an `accountId` as an explicit call parameter everywhere — never tied to a per-account instance — since
   that's the precondition for both DB consolidation and for a Room/SQLDelight implementation that owns one connection
   for all accounts.

## Outcomes

### Positive Outcomes

- Business logic is decoupled from the data layer, so the underlying data source can change without touching call sites
- Splitting repositories by responsibility, not by table, avoids reproducing the god-interface problem
  `FolderRepository` already has today
- Explicit `accountId` per call is the precondition for both database consolidation and a KMP-safe implementation

### Negative Outcomes

- The change will touch many code surfaces
  - Fetch operations (~45 sites)
  - Mutate operations (~45 sites)
  - 27 non-test files depend on `LocalStore` directly
- Requires a phased migration, so old and new data-access paths coexist for a while
- `account_id` filtering has to be added to every query before consolidation is safe; a missed predicate fails
  silently, not loudly

