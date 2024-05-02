# Switch from Java to Kotlin

## Status

- **Accepted**

## Context

We've been using Java as our primary language for Android development. While Java has served us well, it has certain
limitations in terms of null safety, verbosity, functional programming, and more. Kotlin, officially supported by
Google for Android development, offers solutions to many of these issues and provides more modern language features
that can improve productivity, maintainability, and overall code quality.

## Decision

Switch our primary programming language for Android development from Java to Kotlin. This will involve rewriting our
existing Java codebase in Kotlin and writing all new code in Kotlin. To facilitate the transition, we will gradually
refactor our existing Java codebase to Kotlin.

## Consequences

- **Positive Consequences**

  - Improved null safety, reducing potential for null pointer exceptions.
  - Increased code readability and maintainability due to less verbose syntax.
  - Availability of modern language features such as coroutines for asynchronous programming, and extension functions.
  - Officially supported by Google for Android development, ensuring future-proof development.

- **Negative Consequences**

  - The process of refactoring existing Java code to Kotlin can be time-consuming.
  - Potential for introduction of new bugs during refactoring.
