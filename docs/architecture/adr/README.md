# Architecture Decision Records

The [docs/architecture/adr](/docs/architecture/adr) folder contains the architecture decision records (ADRs) for our project.

ADRs are short text documents that serve as a historical context for the architecture decisions we make over the
course of the project.

## What is an ADR?

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along
with its context and consequences. ADRs record the decision making process and allow others to understand the
rationale behind decisions, providing insight and facilitating future decision-making processes.

## Format of an ADR

We adhere to Michael Nygard's [ADR format proposal](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions),
where each ADR document should contain:

1. **Title**: A short descriptive name for the decision.
   1. **Link to Issue**: A link to the issue that prompted the decision.
   2. **Link to Pull Request**: A link to the pull request that implements the ADR.
   3. **Link to Tracking Issue**: A link to the tracking issue, if applicable.
2. **Status**: The current status of the decision (proposed, accepted, rejected, deprecated, superseded)
3. **Context**: The context that motivates this decision.
4. **Decision**: The change that we're proposing and/or doing.
5. **Consequences**: What becomes easier or more difficult to do and any risks introduced as a result of the decision.

## Creating a new ADR

When creating a new ADR, please follow the provided [ADR template file](0000-adr-template.md) and ensure that your
document is clear and concise.

## Directory Structure

The ADRs will be stored in a directory named `docs/adr`, and each ADR will be a file named `NNNN-title-with-dashes.md`
where `NNNN` is a four-digit number that is increased by 1 for every new adr.

## ADR Life Cycle

The life cycle of an ADR is as follows:

1. **Proposed**: The ADR is under consideration.
2. **Accepted**: The decision described in the ADR has been accepted and should be adhered to, unless it is superseded by another ADR.
3. **Rejected**: The decision described in the ADR has been rejected.
4. **Deprecated**: The decision described in the ADR is no longer relevant due to changes in system context.
5. **Superseded**: The decision described in the ADR has been replaced by another decision.

Each ADR will have a status indicating its current life-cycle stage. An ADR can be updated over time, either to change
the status or to add more information.

## Contributions

We welcome contributions in the form of new ADRs or updates to existing ones. Please ensure all contributions follow
the standard format and provide clear and concise information.
