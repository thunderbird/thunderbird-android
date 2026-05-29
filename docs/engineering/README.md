# Engineering

Engineering documents describe how the project discusses, decides, and plans technical changes.

## When to Use Each Document

### RFC

Use an RFC when the team needs to discuss and refine a proposed change before committing to it.

RFCs are useful for:

- Medium or large feature changes.
- Changes that affect multiple modules or teams.
- Contributor-facing implementation plans.
- Changes with several viable approaches.
- Changes where maintainers need to agree on scope before implementation.

An RFC should be opened as a pull request and refined through pull request review.

### ADR

Use an ADR when the team needs to record a durable architectural decision.

ADRs are useful for:

- Architecture-level decisions.
- Long-lived technical direction.
- Decisions that future contributors need to understand.
- Explaining why one approach was chosen over another.

An ADR may come from an accepted RFC, but not every RFC needs an ADR. And not every ADR needs an RFC.

### Technical Design

Use a technical design document when the implementation details are too large for an RFC.

Technical designs are useful for:

- JSON schemas or data formats.
- API contracts.
- Migration procedures.
- Testing plans.
- Multi-PR implementation plans.

The RFC should link to the technical design when one exists.
