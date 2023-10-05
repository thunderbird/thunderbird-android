# 0002 - UI - Wrap Material Components in Atomic Design System

## Status

- **Proposed**

## Context

As we continue developing our Jetpack Compose application, we find a need to increase the consistency, reusability,
and maintainability of our UI components. Currently, we are using Material components directly throughout our
application. This leads to a lack of uniformity and increases the complexity of changes as the same modifications
must be implemented multiple times across different screens.

## Decision

We are proposing to create a new atomic design system where Material components are wrapped into our own 'atomic'
components. For instance, we would have components such as `AtomicButton`, `AtomicText`, etc. These atomic components
will encapsulate Material components with predefined styles and behaviors.

This approach will be taken further to develop more complex 'molecule' and 'organism' components by combining these
atomic components. We will also define 'templates' as layout structures and 'pages' as specific instances of these
templates to build a complete user interface.

## Consequences

- **Positive Consequences**

  - Increased reusability of components across the application, reducing code duplication.
  - More consistent UI across the entire application.
  - Improved maintainability, as changes to a component only need to be made in one place.

- **Negative Consequences**

  - Initial effort and time investment needed to implement the atomic design system.
  - Developers need to adapt to the new system and learn how to use it effectively.
  - Potential for over-complication if simple components are excessively broken down into atomic parts.
