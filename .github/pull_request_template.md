**Thank you for your contribution!**

### Prior to submitting a pull request, please familiarize yourself with...

- Our [Engineering docs](https://github.com/thunderbird/thunderbird-android/tree/main/docs/engineering)
  - Including the [Architecture Decision Records](https://github.com/thunderbird/thunderbird-android/tree/main/docs/engineering/adr)
- Our [Architecture docs](https://github.com/thunderbird/thunderbird-android/tree/main/docs/architecture)
- Read [Mozilla’s Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/)
- Check out our [contribution code quality guides](https://github.com/thunderbird/thunderbird-android/tree/main/docs/contributing), especially our [git commit guide](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/git-commit-guide.md), which can also help you write your pull request title

> [!IMPORTANT]
> Pull requests may take a few days to weeks to review. We’re a small team and prioritize contributions aligned with our [current roadmap](https://developer.thunderbird.net/planning/android-roadmap). 
> You can help us by categorizing your pull request with labels. 
> We appreciate you working with us and will get to reviewing your contribution as soon as we can!

> [!NOTE]
> **Automated PR Sentinel checks (required):** a linked issue, a Conventional Commit title, Conventional Commit 
> messages on every commit (no `Co-authored-by:` trailers), no merge commits (rebase instead), and a completed 
> **AI Disclosure**. PRs that stay non-compliant are **auto-closed 3 days** after the first Sentinel notice. 
> See the [Contribution Workflow](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/contribution-workflow.md#-automated-pr-checks-pr-sentinel).

As always, thank you for the contribution!

# \~\~After reading, delete this line and the above to use the template for your pull request\~\~
-------------------------------------------------------------------------------------------------
## Contribution Summary

Linked Issue/Ticket: _<Please include the issue number with the closing keyword, e.g. Closes #1234>_

RFC / Technical Design (if applicable): _<Remove if not applicable>_

### Description

_<Please provide a detailed description of your contribution here>_

### Screenshots

_<If your pull request makes any UI changes, include screenshots displaying those changes here; Remove section if not applicable>_

### Testing

_<Please provide the steps to reproduce the issue, so we can review it properly>_

## AI Disclosure

Select **one** of the following (mandatory)

- [ ] This contribution does not include any changes created or assisted by AI.
- [ ] This contribution includes changes assisted by AI.
- [ ] This contribution includes changes created by AI.

## Contribution Checklist

- [ ] I have read, and I affirm that my contribution adheres to [Mozilla’s Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/)
- [ ] This contribution is in Kotlin where possible
- [ ] This contribution does not use merge commits
- [ ] This contribution adheres to the existing codestyle (run `gradlew spotlessCheck` to check and `gradlew spotlessApply` to format your source code; will be checked by CI).
- [ ] This contribution does not break existing unit tests (run `gradlew testDebugUnitTest`; will be checked by CI).
- [ ] This contribution includes tests for any new functionality and maintains tests for any updated functionality.
- [ ] This contribution adheres to our [Engineering process](https://github.com/thunderbird/thunderbird-android/tree/main/docs/engineering) (RFC/Technical Design/ADR)
- [ ] This PR has a descriptive title and body that accurately outlines all changes made and contains a reference to any issues that it fixes (e.g. _Closes #XXX_ or _Fixes #XXX_).
