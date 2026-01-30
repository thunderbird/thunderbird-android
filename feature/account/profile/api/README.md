# Account Profile API (feature/account/profile/api)

Types and contracts for account display/profile information, separated from the core `feature/account/api` module.
This module focuses on UI-facing profile data keyed by `AccountId` and an abstraction to read/update it.

## What this module provides

- `AccountProfile`: Display/profile data for UI (e.g., display name, color, avatar) keyed by `AccountId`.
- `AccountProfileRepository`: Abstraction to observe and update profiles.

It depends on `feature/account/api` for identity types such as `AccountId`.

## Usage

Add the dependency and use the repository to observe and update a profile.

```kotlin
// Dependency (Gradle Kotlin DSL)
// implementation(projects.feature.account.profile.api)

// Observe a profile
val profiles: Flow<AccountProfile?> = repo.getById(accountId)

// Update a profile
val updated = AccountProfile(
    id = accountId,
    name = "Alice",
    color = 0xFFAA66,
    avatar = AccountProfile.Avatar.Monogram("A") // or other supported avatar types
)

repo.update(updated)
```

Note: The exact set of avatar representations may evolve. Use the sealed/avatar types exposed by this module.

## Design guidelines

- Profile data is purely presentational and keyed by `AccountId`.
- Do not persist data for `UnifiedAccountId` (from `feature/account/api`). Compute unified profiles/labels in UI.
- Keep profile concerns separate from feature-specific capabilities (mail, calendar, sync, etc.).

## Related modules

- `feature/account/api`: Core account identity types (`AccountId`, `AccountIdFactory`, `UnifiedAccountId`).

