# User Journeys

User journeys and user stories describe the user-centered reason for product-visible work. Store them in the repository
so maintainers and contributors can find the product context alongside the technical documentation.

Use a user journey document when a milestone or feature changes an important user-visible workflow, introduces a new
workflow, or depends on user-centered product decisions that future contributors will need to understand.

Small fixes, refactorings, build tasks, and purely technical work do not need a user journey document.

## Relationship to Issues

GitHub issues are used for planning and delivery tracking. They should link to the relevant user journey document when
one exists, but the repository document is the source of truth for the journey and user stories.

- **Milestone issues** link to the relevant user journey documents for the milestone.
- **Feature issues** link to the user journey document that explains the user-visible behavior.
- **Task issues** link to a user journey document only when the task directly supports that journey.

## Suggested Structure

Use the [user journey template](0000-user-journey-template.md) as the starting point for new user journey documents.
Delete sections that do not apply and add sections when the feature needs more context.

Store user journey documents in this directory using a short, descriptive lowercase filename.

Add or update user journey documents through pull requests, like RFCs, ADRs, and technical designs. Link the pull
request and resulting document from the relevant milestone or feature issues.
