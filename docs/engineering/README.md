# Engineering

The Engineering section defines the processes and artifacts used to propose, decide, and deliver technical changes. It connects high-level roadmap planning with public delivery work through RFCs, Technical Designs, and Architecture Decision Records (ADRs).

Use the smallest set of documents and issues that makes the work clear, reviewable, and maintainable.

## For Contributors

If you want to contribute:

1. Start with the [Contribution Workflow](../contributing/contribution-workflow.md), including its guidance for finding
   an issue, reporting bugs, and discussing your plan before coding.
2. Use GitHub Feature Issues for user-visible work and GitHub Task Issues for supporting work.
3. If the work looks larger than one issue or pull request, ask maintainers whether it needs a GitHub Milestone Issue. Use the
   [Matrix development channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) when you are unsure where to ask.
4. If the work needs a user journey, RFC, ADR, or technical design, add that document in a pull request and link it from
   the relevant issue.

## Overview

Our engineering process connects high-level technical decisions with public delivery work and internal roadmap planning.

```mermaid
flowchart TD
    subgraph Roadmap[Roadmap]
        direction LR
        Epic[Notion Epic] -- split into --> MilestoneNotion[Notion Milestone]
    end

    subgraph Public[Public Delivery]
        direction LR
        MilestoneGH[GitHub Milestone Issue] --> Issues[GitHub Issues]
        Issues --> PR[Pull Requests]
    end
    
    subgraph Planning[Proposals & Decisions]
        direction LR
        RFC[RFC] -. proposes .-> TechDesign[Technical Design]
        RFC -. produces .-> ADR[ADR]
        ADR -. defines .-> TechDesign
    end


    Epic -. defines .-> MilestoneGH
    MilestoneGH -. synced .-> MilestoneNotion
    Planning -. defines .-> MilestoneGH
    MilestoneGH -. planning .-> Planning
    Issues -. planning .-> Planning
    
    classDef proposal fill:#d9e9ff,stroke:#000000,color:#000000
    classDef adr fill:#d9ffd9,stroke:#000000,color:#000000
    classDef design fill:#ffe6cc,stroke:#000000,color:#000000
    classDef issue fill:#fefce8,stroke:#eab308,color:#854d0e
    classDef notion fill:#fdf4ff,stroke:#d946ef,color:#86198f
    classDef pr fill:#f0fdf4,stroke:#22c55e,color:#166534

    linkStyle default stroke:#94a3b8,stroke-width:2px

    class RFC proposal
    class ADR adr
    class TechDesign design
    class MilestoneGH,Issues issue
    class Epic,MilestoneNotion,Roadmap notion
    class PR pr
    class Planning proposal
```

### The Three Pillars

The process is built on three pillars that serve different purposes and audiences:

1. **Roadmap**: The high-level layer managed through **Notion Epics** and **Notion Milestones**. The roadmap is the leading influence for Thunderbird maintainers. This is used for high-level reporting and resource planning.
2. **Public Delivery**: The public project management layer. We use **GitHub Milestone Issues**, **GitHub Feature Issues**, and **GitHub Task Issues** to track *what* is being delivered and *when*. This is the source of truth for all delivery work, including milestone creation and technical planning, and the layer visible to external contributors.
3. **Proposals & Decisions**: Technical documentation (User Journeys, RFCs, ADRs, Technical Designs) stored in the repository. These define *why* and *how* we build things. They are the durable technical record for all contributors and maintainers.

### Continuous Synchronization

We maintain a strict relationship between public delivery and the roadmap:

- **Milestone Sync**: Every **GitHub Milestone Issue** is automatically synced to Notion as a **Notion Milestone**. This ensures that roadmap progress reflects actual delivery status.
- **Public First**: Public GitHub artifacts must always be understandable on their own. We never reference internal Notion artifacts as the only source of requirements or technical detail.
- **Durable Records**: While discussions happen in Pull Requests and Issues, the final decisions and Technical Designs must be captured in the durable artifacts (ADRs, RFCs, or Technical Designs) to remain discoverable for future contributors.

## Artifacts

### Roadmap

Roadmap artifacts are used to track work against the project's long-term goals. We use two types of artifacts:

#### Notion Epic

A Notion epic is an internal roadmap artifact and the leading influence for Thunderbird maintainers.

A Notion epic is typically split into multiple **GitHub Milestone Issues**. Each GitHub Milestone Issue is created in GitHub, synced to Notion as a **Notion Milestone**, and then linked back to the epic to track progress against the roadmap goal.

Notion epics are internal. Public GitHub artifacts should not require access to Notion to be understood.

#### Notion Milestone

A Notion milestone is the internal Notion representation of a synced GitHub Milestone Issue.

It may stand on its own or be manually linked to a Notion epic.

The GitHub Milestone Issue remains the source of truth for delivery scope, creation, and progress.

### Public Delivery

GitHub artifacts are used to track and deliver work.

#### GitHub Milestone Issue

A GitHub Milestone Issue defines a public delivery outcome and is the primary driver for technical planning. Defining the GitHub Milestone Issue is a critical first step of the delivery phase.

Use a GitHub Milestone Issue to describe the objective, scope, out-of-scope work, relevant requirements, and links to related
work.

A GitHub Milestone Issue may stand on its own. It does not need to belong to a Notion epic.

RFCs, ADRs, and Technical Designs may link to a GitHub Milestone Issue when they define the direction for a delivery outcome.
A GitHub Milestone Issue is often the primary driver for these artifacts; when the work is complex enough, it can include
specific tasks to define and review the necessary technical documentation before implementation starts.

GitHub Milestone Issues are created and managed by core maintainers because they require GitHub permissions and roadmap
coordination. See [Delivery Planning](delivery-planning.md) for the GitHub Milestone Issue template and guidance on splitting
milestones into GitHub Feature Issues and GitHub Task Issues.

External contributors usually start from existing GitHub Feature Issues or GitHub Task Issues. If work appears large
enough to need a GitHub Milestone Issue, ask maintainers to create one.

#### GitHub Feature or Task Issue

These issues describe the specific work needed for a GitHub Milestone Issue.
- **Feature**: User-facing or product-visible work.
- **Task**: Supporting engineering work (refactoring, tooling, etc.).

Engineers use these issues to break milestone work into reviewable and assignable pieces. When a GitHub Milestone Issue needs a new
technical direction or durable decision record, specific **Task Issues** are often created to track the delivery of the
relevant RFC, ADR, or Technical Design.

### Proposals & Decisions

Technical documentation helps us reach consensus and record why decisions were made. All proposals and designs are reviewed and approved by **maintainers**.

### User Journey

A user journey records the user-centered reason for product-visible work.

Use a user journey when a milestone or feature changes an important user workflow, introduces a new workflow, or depends
on user-centered product decisions that future contributors will need to understand.

User journeys are stored in the [**`docs/engineering/user-journeys`**](user-journeys/README.md) directory.

### RFC

An RFC proposes a technical direction before implementation starts.

Use an RFC when the team needs to agree on direction before implementation, especially when there are multiple reasonable
approaches, broad impact, or unclear scope.

RFCs are stored in the repository and reviewed through [pull requests](../contributing/contribution-workflow.md).

### ADR

An ADR records a durable architectural decision.

Use an ADR when future contributors need to understand what decision was made, why it was made, and what consequences
it has.

An ADR may come from an RFC, technical design, implementation PR, or stand on its own.

ADRs are stored in the [**`docs/engineering/adr`**](adr/README.md) directory.

### Technical Design

A technical design describes how an accepted direction will be implemented.

Use a technical design when implementation details are too large for an RFC, such as schemas, API contracts, migration
plans, runtime behavior, build tooling, or multi-PR implementation plans.

## Process

The engineering process is flexible and scales with the complexity of the change. Work can start from a proposal,
a milestone, a roadmap item, or a small implementation need.

```mermaid
flowchart TD
    Start[Engineering Work] --> Source{Entry Point}

    subgraph Initiating[1. Initiating]
        direction LR
        Epic[Notion Epic]
        RFC[RFC]
    end

    subgraph Planning[2. Technical Planning]
        direction LR
        TechDesign[Technical Design]
        ADR[ADR]
    end

    subgraph Delivery[3. Delivery]
        direction LR
        MilestoneGH[GitHub Milestone Issue]
        MilestoneNotion[Notion Milestone]
        Issues[GitHub Issues]
    end

    subgraph Implementation[4. Implementation]
        direction LR
        PR[Pull Requests]
    end

    %% Entry Points
    Source --> Epic
    Source --> MilestoneGH
    Source --> RFC
    Source --> ADR

    %% Process Flow
    Epic -. defines .-> MilestoneGH
    RFC -. proposes .-> TechDesign
    RFC -. produces .-> ADR
    ADR -. defines .-> TechDesign
    
    MilestoneGH -. synced .-> MilestoneNotion
    MilestoneNotion -. linked to .-> Epic
    
    TechDesign -. may define .-> MilestoneGH
    MilestoneGH --> Issues
    Issues --> PR

    %% Technical Planning from Issues
    MilestoneGH -. planning .-> Planning
    MilestoneGH -. may require .-> RFC
    Issues -. planning .-> Planning
    Issues -. may propose .-> RFC
    PR -. updates .-> Planning

    classDef start fill:#f8fafc,stroke:#64748b,color:#0f172a
    classDef decision fill:#ffffff,stroke:#000000,color:#000000
    classDef proposal fill:#d9e9ff,stroke:#000000,color:#000000
    classDef adr fill:#d9ffd9,stroke:#000000,color:#000000
    classDef design fill:#ffe6cc,stroke:#000000,color:#000000
    classDef notion fill:#fdf4ff,stroke:#d946ef,color:#86198f
    classDef issue fill:#fefce8,stroke:#eab308,color:#854d0e
    classDef pr fill:#f0fdf4,stroke:#22c55e,color:#166534

    linkStyle default stroke:#94a3b8,stroke-width:2px

    class Start start
    class Source decision
    class RFC proposal
    class ADR adr
    class TechDesign design
    class MilestoneGH,Issues issue
    class Epic,MilestoneNotion notion
    class PR pr
```

### 1. Initiating Work

Work enters the process through different channels depending on its nature:
-   **Roadmap**: High-level goals start as a **Notion Epic**. These are split into multiple outcomes, each requiring the **definition of a GitHub Milestone Issue**.
-   **Public Delivery**: Specific product requirements start with the **definition of a GitHub Milestone Issue**. For external contributors, existing GitHub Feature Issues and GitHub Task Issues are the primary entry point.
-   **Proposals**: New technical ideas or significant changes can start as an **RFC** or as a GitHub Task Issue proposing creation of an RFC. The GitHub Task Issue is delivered by opening the RFC pull request. If the accepted RFC leads to planned delivery work, maintainers create a **GitHub Milestone Issue** and link the RFC GitHub Task Issue as part of that milestone.
-   **Direct Decisions**: Architectural changes that don't require a broad RFC discussion start as an **ADR**.

### 2. Technical Planning

For complex changes, we use durable artifacts to reach consensus before writing production code. Not every milestone
needs an RFC, ADR, or Technical Design. All planning artifacts are reviewed by **maintainers**:
-   **RFC to ADR**: If an RFC results in a significant architectural change, it should produce an ADR to record the decision.
-   **RFC to Technical Design**: If the implementation details are complex, an RFC leads to a **Technical Design**.
-   **Direct to Milestone**: If the direction is clear and the impact is contained, a proposal can go directly to a **GitHub Milestone Issue**.
-   **Milestone to Planning Artifacts**: The usual delivery flow is to define the **GitHub Milestone Issue** first, then add GitHub Task Issues for any RFCs, ADRs, or Technical Designs needed by that milestone.

### 3. Transitioning to Delivery

Work is usually organized for delivery through milestones. This phase transitions high-level goals, proposals, or
technical designs into a concrete delivery plan.

- **Defining the Milestone**: Every non-trivial delivery outcome must be defined as a **GitHub Milestone Issue**. This is the source of truth where we establish the objective, scope, out-of-scope items, and success criteria.
- **Technical Planning**: If the technical direction is not yet fully defined, or the work is complex enough to need a durable record, the milestone includes **Task Issues** to create and review the necessary RFC, ADR, or Technical Design.
- **Synchronization**: The GitHub Milestone Issue is synced to a **Notion Milestone**, which is then linked to a **Notion Epic** for roadmap tracking.
- **Decomposition**: The milestone is broken down into **GitHub Feature Issues** (user-facing) and **GitHub Task Issues** (supporting work) for implementation.

For the operational steps and issue guidance, see [Delivery Planning](delivery-planning.md).

### 4. Implementation & Feedback

Implementation happens in **Pull Requests** (PRs):
-   **Small Changes**: Tiny fixes or refactorings can skip the planning artifacts and go directly to a PR or a GitHub Task Issue.
-   **Durable Updates**: If a code review reveals that the original Technical Design or decision was flawed, the corresponding RFC, ADR, or Technical Design must be updated. This ensures the repository remains an accurate record of our technical state.
-   **Atomic Delivery**: PRs should be small and focused on a single GitHub Feature Issue or GitHub Task Issue.

## Common Scenarios

The following scenarios help you choose the right path for your work.

### 1. Small Fix or Improvement

**Examples**: Minor refactorings, documentation typos, or small UI tweaks.
- **Artifacts**: GitHub Task Issue (optional) + Pull Request.
- **Flow**: Go straight to code. Use a GitHub Task Issue if the work needs to be tracked or assigned before the PR is ready.

### 2. Standard Feature or Scoped Change

**Examples**: A new user-facing setting, a small feature, or a well-defined library update.
- **Artifacts**: GitHub Milestone Issue + GitHub Feature/Task Issues + Pull Requests.
- **Flow**: Define the outcome in a **GitHub Milestone Issue**, break it down into issues, and implement.

### 3. Complex Feature or Significant Change

**Examples**: Implementing a new protocol, a major UI overhaul, or a multi-PR feature.
- **Artifacts**: GitHub Milestone Issue + Technical Design + Issues + PRs.
- **Flow**: Start with a **GitHub Milestone Issue** to define the goal. Use a **GitHub Task Issue** within the milestone to create a **Technical Design**, get feedback, and then proceed to implementation.

### 4. New Technical Direction or Broad Impact

**Examples**: Proposing a new library for dependency injection, changing the module structure, or a new concurrency model.
- **Artifacts**: RFC (+ Technical Design) + GitHub Milestone Issue + Issues + PRs.
- **Flow**: Use an **RFC** to reach consensus on the direction. If the implementation is complex, follow up with a **Technical Design**.

### 5. Fundamental Architectural Decision

**Examples**: Decisions that must be recorded for future contributors (e.g., "Why we use Koin").
- **Artifacts**: ADR (+ RFC) + PR.
- **Flow**: Use an **ADR** to record the decision and its consequences. ADRs are often produced by RFCs or Technical Designs but can also stand alone for clear architectural rules.

### 6. Roadmap-Driven Work

**Examples**: Large internal projects or cross-team goals.
- **Artifacts**: Notion Epic + Notion Milestone (synced) + GitHub Milestone Issue + Issues + PRs.
- **Flow**: Start with a **Notion Epic** to align with internal goals. Link it to a **GitHub Milestone Issue** (which syncs back to a **Notion Milestone**) to track public delivery.
