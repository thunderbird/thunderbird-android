# Account API (feature/account/api)

A small, feature‑agnostic API for representing accounts by identity only. It provides:

- Strongly‑typed account identifiers (`AccountId`)
- A minimal Account interface (identity only)
- A unified account sentinel for aggregated views
- Profile types for UI (`AccountProfile`) and an abstraction to fetch/update them (AccountProfileRepository)

This module intentionally avoids feature‑specific fields (like email addresses). Mail, calendar, sync, etc. should
attach their own capability models keyed by `AccountId`.

## Core concepts

- `Account`: Marker interface for an account, identified solely by `AccountId`.
- `AccountId`: Typealias of `Id<Account>` (UUID‑backed inline value class). Prefer this over raw strings.
- `AccountIdFactory`: Utility for creating/parsing `AccountId` values.
- `UnifiedAccountId`: Reserved `AccountId` (UUID nil) used to represent the virtual “Unified” scope across accounts.
  - `AccountId.isUnified`: Shorthand check for unified account id.
  - `AccountId.requireReal()`: Throws if called with the unified ID. Use in repositories/mutation paths.
- `AccountProfile`: Display/profile data for UI (name, color, avatar) keyed by AccountId.
- `AccountProfileRepository`: Abstraction to read/update profiles.

## Usage

Create a new `AccountId` or parse an existing one:

```kotlin
val id = AccountIdFactory.create()            // new random AccountId
val parsed = AccountIdFactory.of(rawString)   // parse from persisted value
```

Detect and guard against the unified account in write paths:

```kotlin
fun AccountId.requireReal(): AccountId // throws IllegalStateException for unified

if (id.isUnified) {
    // route to unified UI/aggregation services instead of repositories
}
```

Fetch/update an account profile:

```kotlin
val profiles: Flow<AccountProfile?> = repo.getById(id)
repo.update(AccountProfile(id, name = "Alice", color = 0xFFAA66, avatar = Avatar.Monogram(value = "A")))
```

## Design guidelines

- Keep Account minimal (identity only). Do not add mail/calendar/sync fields here.
- Feature modules should define their own models keyed by `AccountId`.
- Do not persist data for `UnifiedAccountId`. Compute unified profiles/labels in UI where needed.
- Prefer strong types (`AccountId`) over raw strings for safety and consistency.

## Related types

- `Id<T>`: Generic UUID‑backed identifier (core/architecture/api)

