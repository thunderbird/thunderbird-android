# Switch Test Assertions from Truth to assertk

- Pull Request: [#7242](https://github.com/thunderbird/thunderbird-android/pull/7242)

## Status

- **Accepted**

## Context

Our project has been using the Truth testing library for writing tests. While Truth has served us well, it is primarily
designed for Java and lacks some features that make our Kotlin tests more idiomatic and expressive. As our codebase is
[primarily Kotlin](0001-switch-from-java-to-kotlin.md), we have been looking for a testing library that is more aligned
with Kotlin's features and idioms.

## Decision

We have decided to use [assertk](https://github.com/willowtreeapps/assertk) as the default assertions framework for
writing tests in our project. assertk provides a fluent API that is very similar to Truth, making the transition easier.
Moreover, it is designed to work well with Kotlin, enabling us to leverage Kotlin-specific features in our tests.

We've further committed to converting all pre-existing tests from Truth to assertk.

## Consequences

**Note**: The migration of all Truth tests to assertk has already been completed.

- **Positive Consequences**

  - **Ease of Transition**: The syntax of assertk is very similar to Truth, which makes the migration process smoother.
  - **Kotlin-Friendly**: assertk is designed specifically for Kotlin, allowing us to write more idiomatic and
    expressive Kotlin tests.

- **Negative Consequences**

  - **Dependency**: While we are replacing one library with another, introducing a new library always carries the risk
    of bugs or future deprecation.
  - **Migration Effort**: Existing tests written using Truth will need to be migrated to use assertk, requiring some
    effort, although mitigated by the similar syntax.
