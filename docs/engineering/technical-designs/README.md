# Technical Designs

Technical design documents describe implementation details that are too large or too specific for an RFC.

## What is a Technical Design?

A technical design is a document that captures how an accepted or proposed direction will be implemented. It focuses
on implementation-level contracts, data formats, APIs, module changes, migration steps, rollout sequencing, and test
coverage.

Technical designs should avoid repeating product, scope, or direction questions from the RFC. Those questions belong
in the RFC. The technical design should focus on how the work will be built and verified.

## Format of a Technical Design

Each technical design document should contain:

1. **Title**: A short descriptive name for the implementation design.
2. **Issue**: A link to the issue that prompted the work, if applicable.
3. **RFC**: A link to the related RFC, if applicable.
4. **ADR**: A link to the related ADR, if applicable.
5. **Status**: The current status of the design.
6. **Summary**: A brief explanation of what the design implements.
7. **Current State**: The relevant current behavior or structure.
8. **Proposed Design**: The target implementation design.
9. **Migration and Rollout**: How the project moves from the current state to the proposed design.
10. **Testing and Verification**: How the implementation will be validated.
11. **Open Technical Questions**: Unresolved implementation-level questions.

## Technical Design Life Cycle

The life cycle of a technical design is as follows:

1. **Proposed**: The technical design is being written or reviewed.
2. **Accepted**: The design has been accepted as the implementation target.
3. **Superseded**: The design has been replaced by another technical design, RFC, or ADR.

Each technical design will have a status indicating its current life-cycle stage. A technical design can be updated
over time, either to change the status or to add more information.

## Creating a New Technical Design

When creating a new technical design, please follow the provided
[technical design template file](0000-technical-design-template.md) and ensure that your document is clear and
concise.

Technical design pull request titles should use the repository's conventional commit style:

```text
docs(technical-design): <short title>
```

When a technical design supports an RFC, link the technical design from the RFC.

## Directory Structure

Technical designs will be stored in `docs/engineering/technical-designs`, and each design will be a file named
`NNNN-title-with-dashes.md`, where `NNNN` is a four-digit number that is increased by 1 for every new technical
design.

Examples:

```text
0001-changelog-json-schema.md
0002-account-setup-state-machine.md
0003-feature-flag-resolution.md
```

Technical design numbers are assigned when the pull request is opened.

## Contributions

We welcome contributions in the form of new technical designs or updates to existing ones. Please ensure all
contributions follow the standard format and provide clear and concise information.
