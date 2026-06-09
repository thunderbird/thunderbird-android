# Delivery Planning

Delivery planning turns an accepted goal into public, reviewable GitHub work. Most planned work starts with a GitHub
Milestone Issue, then adds the GitHub Feature Issues, GitHub Task Issues, and technical documents needed to deliver it.

The goal is to keep GitHub issues understandable on their own, even when the work also relates to internal roadmap
planning.

## Internal Planning Input

Larger roadmap work may be prepared internally through a Scope of Work (SoW), kickoff meeting, milestone owner
assignment, and follow-up planning. This internal process is not usually owned by the person creating the GitHub
delivery artifacts.

For this engineering process, the required public step is to create the GitHub Milestone Issue from the agreed planning
output. The GitHub Milestone Issue remains the public source of truth for delivery scope, even when an internal SoW or Notion
artifact provides additional planning context.

External contributors do not need access to the SoW or Notion artifacts to contribute. Public GitHub issues must contain
the context needed to understand and deliver the work.

## Mapping Internal Planning to Public GitHub Work

Internal planning can contain more detail than should live in a single GitHub issue. Translate only the information
needed for public planning and delivery:

- **Milestone or epic scope**: Capture the objective, resources, scope, out-of-scope work, and requirements in the
  GitHub Milestone Issue.
- **User journey and product value**: Capture durable user journeys and user stories in repository documentation. Link
  to those documents from the GitHub Milestone Issue and related GitHub Feature Issues.
- **Technical direction**: Create GitHub Task Issues for an RFC, ADR, or technical design only when the milestone needs them.
- **Implementation work**: Split the milestone into GitHub Feature Issues and GitHub Task Issues that can be assigned,
  reviewed, and merged independently.
- **Validation needs**: Capture expected verification in the relevant GitHub Feature Issues or GitHub Task Issues.
  Create separate GitHub Task Issues when validation work needs its own owner or pull request.

Do not copy internal-only planning notes into public issues. Public issues must contain enough context to be understood
without access to private documents.

## Issue Types

We use three GitHub issue types for planned delivery work:

- **GitHub Milestone Issue**: Defines the public delivery outcome, scope, requirements, and related resources.
- **GitHub Feature Issue**: Describes user-facing or product-visible work needed to complete the milestone.
- **GitHub Task Issue**: Describes supporting work needed to complete the milestone, such as technical planning, refactoring,
  tests, build changes, documentation, or follow-up investigation.

GitHub issues are limited to core maintainers because creating and managing them depends on GitHub
permissions and roadmap coordination. Contributors and non-maintainer developers can discuss work through existing
issues, but should not create new GitHub issues without prior approval.

## Contributor Path

If you are not a core maintainer:

1. Follow the [Contribution Workflow](../contributing/contribution-workflow.md) for finding an issue and
   discussing your plan before coding.
2. If the work is already planned, contribute through the relevant GitHub Feature Issue or GitHub Task Issue.
3. If you want to propose a **new feature**, start a discussion in [Mozilla Connect](https://connect.mozilla.org/t5/ideas/idb-p/ideas/label-name/thunderbird%20android). Once accepted and planned, maintainers will create the corresponding GitHub issues.
4. If the work is a **technical task** (not a feature) that needs public tracking but is not milestone-sized, discuss your plan in an existing issue. If approved, ask maintainers to create the GitHub Task Issue for you. If there is no direct GitHub Milestone Issue match, mention the likely quarterly catch-all milestone in the issue or pull request. Maintainers link the issue to the milestone.
5. If the work proposes a broad technical direction, discuss your plan in an existing issue. If approved, ask maintainers to create a GitHub Task Issue proposing an RFC. That GitHub Task Issue is delivered by opening the pull request that adds the RFC.
6. If the work appears milestone-sized, ask maintainers to create a GitHub Milestone Issue instead of creating one yourself.
   Use the [Matrix development channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) when you are unsure where to ask.

GitHub Milestone Issues are maintainer-owned. GitHub Feature Issues, GitHub Task Issues, RFC pull requests, and
implementation pull requests are the usual entry points for external contributors.

## GitHub Milestone Issues

A GitHub Milestone Issue is the source of truth for a delivery outcome. It should describe what the team is trying to
achieve, why it matters, what is in scope, and what is intentionally out of scope.

Create a GitHub Milestone Issue when:

- The work represents a meaningful user, product, technical, or roadmap outcome.
- The work needs to be split into multiple GitHub Feature Issues or GitHub Task Issues.
- The work requires coordination across multiple pull requests, modules, or contributors.
- The work needs public tracking even if it originated from an internal roadmap item.

Do not create a GitHub Milestone Issue for a small standalone fix or a single pull request unless maintainers need explicit
public tracking for it.

### Quarterly Catch-All Milestones

When work does not match a dedicated GitHub Milestone Issue, maintainers link it to the relevant quarterly catch-all
GitHub Milestone Issue:

- **Community Contributions YYYY QN**: Community contribution work that does not belong to a dedicated milestone.
- **Android Foundations YYYY QN**: Android foundations work that does not belong to a dedicated milestone.

For example, maintainers use **Community Contributions 2026 Q3** or **Android Foundations 2026 Q3** for Q3 2026 work
without a direct milestone match.

### Maintainer Responsibilities

When creating a GitHub Milestone Issue, core maintainers should:

1. Define the objective and success criteria.
2. Add links to relevant resources, such as GitHub issues, user journey documents, RFCs, and technical designs.
3. Define what is in scope and out of scope.
4. Capture technical and business requirements that are known at the time.
5. Create or link the GitHub Feature Issues and GitHub Task Issues needed to deliver the milestone as subissues.
6. Keep the GitHub Milestone Issue updated when scope changes.

The GitHub Milestone Issue does not need to contain every implementation detail. Detailed implementation plans should
live in GitHub Feature Issues, GitHub Task Issues, pull requests, or, when the work is complex enough, RFCs, ADRs, and
technical designs.

### Creating a Milestone Issue

When creating a GitHub Milestone Issue:

1. Create a new GitHub issue using the [GitHub Milestone Issue template](milestone-issues/0000-milestone-issue-template.md).
2. Select the GitHub Milestone Issue type if GitHub issue types are available.
3. Add GitHub Milestone Issues to the [roadmap project](https://github.com/orgs/thunderbird/projects/19).
4. Add current implementation work to the [sprint board](https://github.com/orgs/thunderbird/projects/20).
5. Set the initial project status.
6. Link relevant resources, including GitHub issues, design artifacts, RFCs, ADRs, technical designs, and public
   planning references.
7. Create or link GitHub Feature Issues and GitHub Task Issues as subissues of the GitHub Milestone Issue.
8. Add RFC, ADR, or technical design GitHub Task Issues as subissues when the milestone needs them.
9. If the work does not have a dedicated GitHub Milestone Issue, link it to the relevant quarterly catch-all milestone
   issue.

If a GitHub field or project automation is not available to you, create the issue with the template and ask a core
maintainer to complete the project metadata.

### Status and Progress

The milestone owner is responsible for keeping the GitHub Milestone Issue current.

Update the GitHub Milestone Issue when:

- Scope, requirements, or out-of-scope work changes.
- GitHub Feature Issues or GitHub Task Issues are added, removed, completed, or replaced.
- An RFC, ADR, or technical design changes the delivery plan.
- Delivery risk, sequencing, or ownership changes.

Progress should be visible through the GitHub Milestone Issue's subissues. Completed GitHub Feature Issues and GitHub
Task Issues should remain linked so the GitHub Milestone Issue shows what was delivered and what remains.

## Splitting Milestones Into Work Issues

After the GitHub Milestone Issue exists, split it into GitHub Feature Issues and GitHub Task Issues. The split should
make the work small enough to assign, review, and merge safely.

Prefer several focused issues over one large issue that mixes product behavior, technical planning, and implementation.
Avoid splitting so far that each issue loses meaningful context.

GitHub Feature Issues and GitHub Task Issues that belong to a milestone must be added as subissues of the GitHub
Milestone Issue. This includes GitHub Task Issues for RFCs, ADRs, and technical designs. The GitHub Milestone Issue
stays focused on the delivery outcome while progress is tracked through its subissues.

If the milestone needs technical agreement before implementation, add GitHub Task Issues for the needed RFC, ADR, or
technical design as part of the milestone definition. Not every milestone needs these documents; use them when the
change has multiple reasonable approaches, broad architectural impact, complex implementation details, or decisions that
future contributors will need to understand.

Sometimes the order is reversed: a developer may create a GitHub Task Issue to propose creating an RFC before a
milestone exists. That GitHub Task Issue is delivered by opening the pull request that adds the RFC. If the accepted RFC
leads to planned delivery work, core maintainers should create the GitHub Milestone Issue and add the RFC GitHub Task
Issue as a subissue of that milestone.

### GitHub Feature Issues

Use GitHub Feature Issues for product-visible work.

Examples:

- Add a new user-facing setting.
- Change a user flow.
- Implement a new screen or interaction.
- Support a new account or message behavior.

A GitHub Feature Issue should usually include the relevant parts of:

- The user-visible outcome.
- Acceptance criteria or expected behavior.
- Links to relevant user journey documentation and user stories.
- Relevant design, product, or support links.
- Target user segments or user states when they affect the expected behavior.
- Known user frictions, failure cases, or UX research questions.
- Known constraints, edge cases, or dependencies.
- A link to the GitHub Milestone Issue and related tasks.

GitHub Feature Issues do not currently require a formal template. Keep them concise, but include enough context for a
contributor to understand the expected behavior without relying on private documents.

User journeys and user stories are part of the project's persistent delivery context. Document them in
[User Journeys](user-journeys/README.md). GitHub Feature Issues should link to the relevant user journey document so
delivery work stays connected to the user-centered source of truth.

### GitHub Task Issues

Use GitHub Task Issues for supporting engineering work.

Examples:

- Write an RFC, ADR, or technical design.
- Prepare a migration or refactoring required by a feature.
- Add test infrastructure.
- Update documentation.
- Investigate an implementation option before feature work starts.

A GitHub Task Issue should usually include:

- The concrete output expected from the task.
- A link to the GitHub Milestone Issue and any related GitHub Feature Issues.
- Constraints, affected modules, or architectural boundaries.
- Verification expectations when known.

GitHub Task Issues do not currently require a formal template. Keep them flexible so developers can organize technical
work in the way that best fits the change.

## Planning Checklist

Before implementation starts, confirm that:

- The GitHub Milestone Issue explains the objective, scope, out-of-scope work, and requirements.
- Relevant user journeys and user stories are documented in the repository and linked from the milestone or feature
  issues.
- Any needed RFCs, ADRs, or technical designs are created or tracked by GitHub Task Issues.
- GitHub Feature Issues describe user-visible outcomes and acceptance criteria.
- GitHub Task Issues describe concrete engineering outputs.
- GitHub Feature Issues and GitHub Task Issues are linked as subissues of the GitHub Milestone Issue so progress can be
  tracked publicly.
- Private roadmap context is not required to understand the public GitHub issues.

