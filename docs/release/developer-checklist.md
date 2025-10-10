# Developer Release Checklist

This checklist is for developers. It summarizes what you (as a contributor/feature owner) should do before the scheduled merges in our release train:
- main → beta
- beta → release

For the full release-driver process (branch locks, announcements, publishing), see [Release → Release Process](../release/RELEASE.md).

## Ongoing (between merges)

Do these as part of regular development:

- Identify potential uplifts early
  - Add risk and user impact notes to the issue/PR; ensure the fix lands on `main` and bakes on Daily first — see [Uplifts](../release/RELEASE.md#uplifts) and [Uplift Criteria](../release/RELEASE.md#uplift-criteria)
- Strings and translations
  - Avoid late string changes; if unavoidable, keep them small, so translators can catch up
  - Prefer not changing localizable strings for uplifts
  - Follow [Changing translations in this repository](../contributing/managing-strings.md#-changing-translations-in-the-repo) only when truly necessary
- Quality signals you own
  - Watch crash/ANR reports and GitHub issues for your area of work and investigate regressions
- Project management
  - Keep your issues in the project up to date (assignees, labels, status) and link PRs to issues
  - Ensure your issues are added to the [project sprint board](https://github.com/orgs/thunderbird/projects/20) and assigned to the current sprint
  - Review the sprint board regularly and pick up backlog items as capacity allows, especially bugs and regressions
  - When reviewing external contributions:
    - Add the issue to the appropriate parent issue if not done already (e.g. `[EPIC] Mobile Foundations QX 20XX`)
    - Add the issue to the project sprint board and assign it to the current sprint

## Before main → beta (developer responsibilities)

> [!NOTE]
> A one-week [Soft Freeze](../release/RELEASE.md#soft-freeze) occurs before merging `main` into `beta`.

During soft freeze:
- Avoid landing risky code changes
- Do not enable feature flags that are currently disabled

Goal: Changes on `main` are safe to expose to a broader audience.

- Feature flags
  - Ensure flags match the [rules for beta](../release/RELEASE.md#feature-flags)
    - New features are disabled by default unless explicitly approved for beta
    - Not-ready features must be disabled
  - Prepare and merge a PR on `main` with necessary flag changes (before soft freeze starts)
- Translations
  - Ensure translation updates needed for your features are merged to `main`
  - If no Weblate PR is pending, trigger one and help review it (fix conflicts if needed)

## Before beta → release (developer responsibilities)

Goal: Changes on `beta` are safe for general availability.

- Feature flags
  - Verify flags align with [rules for release](../release/RELEASE.md#feature-flags)
    - Features are disabled unless explicitly approved for release
    - Not-ready features remain disabled
  - If changes are required, open a PR on `main` and request uplift to `beta` following the criteria in [Uplift Criteria](../release/RELEASE.md#uplift-criteria)
- Translations
  - No new string changes at this stage; confirm your changes don’t introduce them
- Stability checks you can influence
  - Review crash/ANR reports and GitHub issues for changes affecting beta and release
  - Investigate regressions and propose fixes if needed
  - Ensure your changes have been tested on beta and address any issues found

## Optional: PR checklist snippet

Paste the following snippet into your PR description to help reviewers and release drivers verify readiness for merge:

```markdown
- [ ] Feature flags set according to target branch rules ([beta](../release/RELEASE.md#feature-flags) / [release](../release/RELEASE.md#feature-flags))
- [ ] Tests added/updated; CI green on affected modules
- [ ] No new localizable strings (or justified and coordinated)
- [ ] Translations accounted for (Weblate PR merged or not required)
- [ ] Uplift template with risk/impact notes filled out if proposing uplift ([criteria](../release/RELEASE.md#uplift-criteria))
```

## After merges (what developers should verify)

- Validate outcome for your changes
  - Watch crash/ANR and error reports for regressions related to your changes after rollout
  - Be prepared to propose/prepare a hotfix via the uplift process if necessary

> [!NOTE]
> Merge-day coordination (branch locks, Matrix announcements, running scripts) is handled by release drivers. See [Merge Process](../release/RELEASE.md#merge-process) for details.

