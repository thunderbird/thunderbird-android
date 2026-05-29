# Engineering RFCs

RFCs are planning documents for medium-to-large engineering changes that need review before implementation.

## What is an RFC?

A Request for Comments (RFC) is a document that captures a proposed engineering direction along with its motivation,
alternatives, risks, open questions, and outcome. RFCs record the planning process and allow others to understand the
rationale behind a proposal before implementation begins.

Architecture Decision Records (ADRs) continue to record durable architectural decisions. Technical designs capture
implementation details that are too large or too specific for an RFC.

## Format of an RFC

Each RFC document should contain:

1. **Title**: A short descriptive name for the proposal.
2. **Issue**: A link to the issue that prompted the proposal.
3. **Technical Design**: A link to the related technical design, if one exists.
4. **Status**: The current status of the RFC (proposed, accepted, rejected, superseded).
5. **Summary**: A brief explanation of the proposal.
6. **Motivation**: The context and problem the proposal is addressing.
7. **Proposal**: The change being proposed.
8. **Alternatives Considered**: Other options and why they were not chosen.
9. **Risks & Drawbacks**: Known risks, trade-offs, or downsides.
10. **Open Questions**: Unresolved questions that need reviewer input.
11. **Outcome**: The final decision and related follow-up work.

## RFC Life Cycle

The life cycle of an RFC is as follows:

1. **Proposed**: The RFC is under consideration.
2. **Accepted**: The proposal described in the RFC has been accepted and can be implemented, unless it is superseded by another RFC or ADR.
3. **Rejected**: The proposal described in the RFC has been rejected.
4. **Superseded**: The RFC has been replaced by another RFC or ADR.

Each RFC will have a status indicating its current life-cycle stage. An RFC can be updated over time, either to change
the status or to add more information.

## Creating a New RFC

When creating a new RFC, please follow the provided [RFC template file](0000-rfc-template.md) and ensure that your
document is clear and concise.

RFC pull request titles should use the repository's conventional commit style:

```text
docs(rfc): <short title>
```

Once you are ready to propose your RFC, you should:

1. Create a pull request in the repository that adds the RFC document.
2. Request review from the relevant maintainers or module owners.
3. Refine the RFC in the same pull request based on review feedback.
4. Make a decision together with the reviewers. The pull request will be merged when accepted.

## Directory Structure

RFCs will be stored in `docs/engineering/rfcs`, and each RFC will be a file named `NNNN-title-with-dashes.md`, where
`NNNN` is a four-digit number that is increased by 1 for every new RFC.

Examples:

```text
0001-changelog-system-replacement.md
0002-account-setup-flow-refactor.md
0003-feature-flag-cleanup.md
```

RFC numbers are assigned when the pull request is opened.

## Relationship to ADRs and Technical Designs

RFCs, ADRs, and technical designs serve different purposes.

An RFC is used before and during implementation. It describes what is being proposed, why, what options were
considered, and what decision was made.

An ADR records durable architectural decisions. It describes what was decided, why it was chosen, and what the
consequences are.

A technical design describes implementation details. It captures how an accepted or proposed direction will be built and
verified.

An accepted RFC may produce one or more ADRs if the decision is architectural and long-lived. It may also link to one
or more technical designs when the implementation details need separate review.

## Contributions

We welcome contributions in the form of new RFCs or updates to existing ones. Please ensure all contributions follow
the standard format and provide clear and concise information.
