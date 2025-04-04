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

Once you are ready to propose your ADR, you should:

1. Create an issue in the repository, get consensus from at least one other project contributor.
2. Make a post on [the mobile-planning list](https://thunderbird.topicbox.com/groups/mobile-planning)
   to announce your ADR. You can use the below template as needed.
3. Create a pull request in the repository linking the issue.
4. Make a decision together with mobile module owners, the PR will be merged when accepted.

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

## Appendix: Intent to Adopt Template

You may use this template in your Intent to Adopt email as noted above. Tweak it as you feel is useful.

> Hello everyone,
>
> I’m writing to share an intent to adopt a new architecture decision: [ADR-[Number]] [Title of ADR]
>
> This change addresses [brief summary of the problem] and proposes [brief description of the approach].
>
> This decision is based on [briefly mention motivating factors, constraints, or technical context].
>
> You can read the full proposal here: [link to ADR]
>
> If you have feedback or concerns, please respond in the linked issue. We plan to finalize the
> decision after [proposed date], factoring in discussion at that time.
>
> Thanks,
> [Your Name]

