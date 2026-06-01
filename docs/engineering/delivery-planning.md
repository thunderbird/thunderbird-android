# Delivery Planning

Delivery planning turns an accepted goal into public, reviewable GitHub work. Most planned work starts with a milestone
issue, then adds the feature issues, task issues, and technical documents needed to deliver it.

The goal is to keep GitHub issues understandable on their own, even when the work also relates to internal roadmap
planning.

## Internal Planning Input

Larger roadmap work may be prepared internally through a Scope of Work (SoW), kickoff meeting, milestone owner
assignment, and follow-up planning. This internal process is not usually owned by the person creating the GitHub
delivery artifacts.

For this engineering process, the required public step is to create the GitHub milestone issue from the agreed planning
output. The milestone issue remains the public source of truth for delivery scope, even when an internal SoW or Notion
artifact provides additional planning context.

External contributors do not need access to the SoW or Notion artifacts to contribute. Public GitHub issues must contain
the context needed to understand and deliver the work.

## Mapping Internal Planning to Public GitHub Work

Internal planning can contain more detail than should live in a single GitHub issue. Translate only the information
needed for public planning and delivery:

- **Milestone or epic scope**: Capture the objective, resources, scope, out-of-scope work, and requirements in the
  milestone issue.
- **User journey and product value**: Capture durable user journeys and user stories in repository documentation. Link
  to those documents from the milestone issue and related feature issues.
- **Technical direction**: Create task issues for an RFC, ADR, or technical design only when the milestone needs them.
- **Implementation work**: Split the milestone into feature and task issues that can be assigned, reviewed, and merged
  independently.
- **Validation needs**: Capture expected verification in the relevant feature or task issues. Create separate testing
  task issues when validation work needs its own owner or pull request.

Do not copy internal-only planning notes into public issues. Public issues must contain enough context to be understood
without access to private documents.

## Issue Types

We use three GitHub issue types for planned delivery work:

- **Milestone issue**: Defines the public delivery outcome, scope, requirements, and related resources.
- **Feature issue**: Describes user-facing or product-visible work needed to complete the milestone.
- **Task issue**: Describes supporting work needed to complete the milestone, such as technical planning, refactoring,
  tests, build changes, documentation, or follow-up investigation.

Milestone issues are limited to core maintainers because creating and managing them depends on GitHub permissions and
roadmap coordination. Contributors and non-maintainer developers can create feature and task issues when work needs to
be split further, assigned, discussed, or reviewed.

## Contributor Path

If you are not a core maintainer:

1. Follow the [Contribution Workflow](../contributing/contribution-workflow.md) for finding an issue, reporting bugs,
   and discussing your plan before coding.
2. If the work is already planned, contribute through the relevant feature or task issue.
3. If the work needs public tracking but is not milestone-sized, create a feature or task issue and link it to the
   matching milestone issue. If there is no direct milestone match, use the relevant quarterly catch-all milestone.
4. If the work proposes a broad technical direction, create a task issue proposing an RFC. That task issue is delivered
   by opening the pull request that adds the RFC.
5. If the work appears milestone-sized, ask maintainers to create a milestone issue instead of creating one yourself.
   Use the [Matrix development channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) when you are unsure where to ask.

Milestone issues are maintainer-owned. Feature issues, task issues, RFC pull requests, and implementation pull requests
are the usual entry points for external contributors.

## Milestone Issues

A milestone issue is the source of truth for a delivery outcome. It should describe what the team is trying to achieve,
why it matters, what is in scope, and what is intentionally out of scope.

Create a milestone issue when:

- The work represents a meaningful user, product, technical, or roadmap outcome.
- The work needs to be split into multiple feature or task issues.
- The work requires coordination across multiple pull requests, modules, or contributors.
- The work needs public tracking even if it originated from an internal roadmap item.

Do not create a milestone issue for a small standalone fix or a single pull request unless maintainers need explicit
public tracking for it.

### Quarterly Catch-All Milestones

When work does not match a dedicated milestone issue, use the relevant quarterly catch-all milestone issue:

- **Community Contributions YYYY QN**: Community contribution work that does not belong to a dedicated milestone.
- **Android Foundations YYYY QN**: Android foundations work that does not belong to a dedicated milestone.

For example, use **Community Contributions 2026 Q3** or **Android Foundations 2026 Q3** for Q3 2026 work without a
direct milestone match.

### Maintainer Responsibilities

When creating a milestone issue, core maintainers should:

1. Define the objective and success criteria.
2. Add links to relevant resources, such as GitHub issues, user journey documents, Figma files, Notion pages, RFCs, and
   technical designs.
3. Define what is in scope and out of scope.
4. Capture technical and business requirements that are known at the time.
5. Create or link the feature and task issues needed to deliver the milestone as subissues.
6. Keep the milestone issue updated when scope changes.

The milestone issue does not need to contain every implementation detail. Detailed implementation plans should live in
feature issues, task issues, pull requests, or, when the work is complex enough, RFCs, ADRs, and technical designs.

### Creating a Milestone Issue

When creating a milestone issue in GitHub:

1. Create a new GitHub issue using the [milestone issue template](milestone-issues/0000-milestone-issue-template.md).
2. Select the milestone issue type if GitHub issue types are available.
3. Add milestone issues to the [roadmap project](https://github.com/orgs/thunderbird/projects/19).
4. Add current implementation work to the [sprint board](https://github.com/orgs/thunderbird/projects/20).
5. Set the initial project status.
6. Link relevant resources, including GitHub issues, design artifacts, RFCs, ADRs, technical designs, and public
   planning references.
7. Create or link feature and task issues as subissues of the milestone issue.
8. Add RFC, ADR, or technical design task issues as subissues when the milestone needs them.
9. If the work does not have a dedicated milestone issue, link it to the relevant quarterly catch-all milestone issue.

If a GitHub field or project automation is not available to you, create the issue with the template and ask a core
maintainer to complete the project metadata.

### Status and Progress

The milestone owner is responsible for keeping the milestone issue current.

Update the milestone issue when:

- Scope, requirements, or out-of-scope work changes.
- Feature or task issues are added, removed, completed, or replaced.
- An RFC, ADR, or technical design changes the delivery plan.
- Delivery risk, sequencing, or ownership changes.

Progress should be visible through the milestone issue's subissues. Completed feature and task issues should remain
linked so the milestone issue shows what was delivered and what remains.

## Splitting Milestones Into Work Issues

After the milestone issue exists, split it into feature and task issues. The split should make the work small enough to
assign, review, and merge safely.

Prefer several focused issues over one large issue that mixes product behavior, technical planning, and implementation.
Avoid splitting so far that each issue loses meaningful context.

Feature and task issues that belong to a milestone must be added as subissues of the milestone issue. This includes
task issues for RFCs, ADRs, and technical designs. The milestone issue stays focused on the delivery outcome while
progress is tracked through its subissues.

If the milestone needs technical agreement before implementation, add task issues for the needed RFC, ADR, or
technical design as part of the milestone definition. Not every milestone needs these documents; use them when the
change has multiple reasonable approaches, broad architectural impact, complex implementation details, or decisions that
future contributors will need to understand.

Sometimes the order is reversed: a developer may create a task issue to propose creating an RFC before a milestone
exists. That task issue is delivered by opening the pull request that adds the RFC. If the accepted RFC leads to planned
delivery work, core maintainers should create the milestone issue and add the RFC task issue as a subissue of that
milestone.

### Feature Issues

Use feature issues for product-visible work.

Examples:

- Add a new user-facing setting.
- Change a user flow.
- Implement a new screen or interaction.
- Support a new account or message behavior.

A feature issue should usually include the relevant parts of:

- The user-visible outcome.
- Acceptance criteria or expected behavior.
- Links to relevant user journey documentation and user stories.
- Relevant design, product, or support links.
- Target user segments or user states when they affect the expected behavior.
- Known user frictions, failure cases, or UX research questions.
- Known constraints, edge cases, or dependencies.
- A link to the milestone issue and related tasks.

Feature issues do not currently require a formal template. Keep them concise, but include enough context for a
contributor to understand the expected behavior without relying on private documents.

User journeys and user stories are part of the project's persistent delivery context. Document them in
[User Journeys](user-journeys/README.md). Feature issues should link to the relevant user journey document so delivery
work stays connected to the user-centered source of truth.

### Task Issues

Use task issues for supporting engineering work.

Examples:

- Write an RFC, ADR, or technical design.
- Prepare a migration or refactoring required by a feature.
- Add test infrastructure.
- Update documentation.
- Investigate an implementation option before feature work starts.

A task issue should usually include:

- The concrete output expected from the task.
- A link to the milestone issue and any related feature issues.
- Constraints, affected modules, or architectural boundaries.
- Verification expectations when known.

Task issues do not currently require a formal template. Keep them flexible so developers can organize technical work in
the way that best fits the change.

## Planning Checklist

Before implementation starts, confirm that:

- The milestone issue explains the objective, scope, out-of-scope work, and requirements.
- Relevant user journeys and user stories are documented in the repository and linked from the milestone or feature
  issues.
- Any needed RFCs, ADRs, or technical designs are created or tracked by task issues.
- Feature issues describe user-visible outcomes and acceptance criteria.
- Task issues describe concrete engineering outputs.
- Feature and task issues are linked as subissues of the milestone issue so progress can be tracked publicly.
- Private roadmap context is not required to understand the public GitHub issues.

