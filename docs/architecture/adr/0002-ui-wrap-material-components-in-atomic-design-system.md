# UI - Wrap Material Components in Atomic Design System

- Pull Request: [#7221](https://github.com/thunderbird/thunderbird-android/pull/7221)

## Status

- **Accepted**

## Context

As we continued developing our Jetpack Compose application, we found a need to increase the consistency, reusability,
and maintainability of our user interface (UI) components. We have been using Material components directly throughout our
application. This lead to a lack of uniformity and increases the complexity of changes as the same modifications had to
be implemented multiple times across different screens.

## Decision

To address these challenges, we've decided to adopt an
[Atomic Design System](../../../core/ui/compose/designsystem/README.md) as a foundation for our application UI.
This system encapsulates Material components within our [own components](../../../core/ui/compose/designsystem/),
organized into categories of _atoms_, _molecules_, and _organisms_. We also defined _templates_ as layout structures
that can be flexibly combined to construct _pages_. These components collectively form the building blocks that we are
using to construct our application's UI.

## Consequences

- **Positive Consequences**

  - Increased reusability of components across the application, reducing code duplication.
  - More consistent UI and uniform styling across the entire application.
  - Improved maintainability, as changes to a component only need to be made in one place.

- **Negative Consequences**

  - Initial effort and time investment needed to implement the atomic design system.
  - Developers need to adapt to the new system and learn how to use it effectively.
  - Potential for over-complication if simple components are excessively broken down into atomic parts.
