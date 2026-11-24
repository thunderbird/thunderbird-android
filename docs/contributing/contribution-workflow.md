# 🤝 Contribution Workflow

The contribution workflow for the Thunderbird for Android project explains the process of contributing code, from
finding an issue to getting your pull request merged.

## ✅ Quick Workflow

```markdown
- [ ] Find an issue (or open a bug report)
- [ ] Review [Engineering process](../engineering/README.md) for user journey, RFC, ADR, or Technical Design requirements
- [ ] Fork → clone → add upstream remote
- [ ] Create a descriptive branch from `main`
- [ ] Make focused changes + update docs/tests
- [ ] Run `./gradlew check` locally (matches CI)
- [ ] Commit with Conventional Commits (Fix: #123)
- [ ] Push branch to your fork
- [ ] Open a pull request with description/screenshots
- [ ] Respond to review feedback → update branch
- [ ] Once merged: delete branch, sync fork, celebrate 🎉
```

## 🔍 Finding an Issue to Work On

### Exploring Issues

Before starting work, find an appropriate issue to work on:

- Browse the [GitHub Issues](https://github.com/thunderbird/thunderbird-android/issues) for open issues
- Prefer issues labeled [status: help wanted](https://github.com/thunderbird/thunderbird-android/labels/status%3A%20help%20wanted)
  or [good first issue](https://github.com/thunderbird/thunderbird-android/labels/good%20first%20issue) if you're new
  to the project
- Do not take issues labeled [tb-team](https://github.com/thunderbird/thunderbird-android/labels/tb-team); they are reserved
  for maintainers
- Avoid issues labeled [unconfirmed](https://github.com/thunderbird/thunderbird-android/labels/unconfirmed) as they are not yet ready for contributions

### Requesting New Features / Ideas

We don’t track new ideas or feature requests in GitHub Issues. Mozilla Connect is where feature proposals, product
decisions, and larger design conversations happen.

- Start a discussion in [Mozilla Connect - Ideas](https://connect.mozilla.org/t5/ideas/idb-p/ideas/label-name/thunderbird%20android)
- Once a feature is accepted and work is planned, maintainers will create the corresponding GitHub issue(s).

### Working From GitHub Issues

GitHub Bug Issues track confirmed defects. GitHub Feature Issues and GitHub Task Issues track work that maintainers have
accepted and planned. New feature proposals still start in Mozilla Connect; maintainers create GitHub issues after a
proposal is accepted and scheduled.

External contributors should start from existing confirmed or planned issues:

- Use GitHub Bug Issues for confirmed defects.
- Use GitHub Feature Issues for accepted user-visible work.
- Use GitHub Task Issues for accepted supporting engineering work, such as refactoring, test infrastructure,
  documentation, investigation, or technical planning.
- Prefer issues labeled `status: help wanted` or `good first issue`.
- Do not work on bug issues labeled `unconfirmed`; they still need maintainer triage.
- Do not take issues labeled `tb-team`; they are reserved for maintainers.
- Comment on the issue before coding and explain the part you want to work on.
- Wait until a maintainer assigns the issue to you before starting work.
- Do not open pull requests for large, cross-cutting, or unclear work without maintainer assignment and alignment in the
  relevant issue.

If there is no matching issue:

- New feature ideas belong in [Mozilla Connect - Ideas](https://connect.mozilla.org/t5/ideas/idb-p/ideas/label-name/thunderbird%20android), not GitHub Issues.
- Bugs should be reported with the GitHub bug template.
- For technical work related to an existing issue, ask in that issue whether the contribution fits the current scope.
- If there is no related issue and the work is not a bug or Mozilla Connect feature idea, use the
  [Matrix development channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) to ask where the work belongs before
  starting.

Maintainers decide whether new GitHub Feature Issues, GitHub Task Issues, or GitHub Milestone Issues are needed.

### Reporting Bugs

If you’ve found a bug that’s not yet tracked:

- Open a [new GitHub issue](https://github.com/thunderbird/thunderbird-android/issues/new/choose)
- Use the bug template and provide detailed reproduction steps.

### Discussing Your Plan

Before coding:
1. Comment on the GitHub issue you want to work on.
2. Explain your intended approach.
3. For non-trivial changes, you may be asked to create a **[User Journey](../engineering/user-journeys/README.md)**, **[RFC](../engineering/rfcs/README.md)**, **[ADR](../engineering/adr/README.md)**, or **[Technical Design](../engineering/technical-designs/README.md)** to reach consensus before implementation.
4. Wait for a maintainer to assign the issue to you before starting work.

## 🍴 Forking and Cloning

To contribute code, you'll need to work with your own fork of the repository:

1. Go to the [Thunderbird for Android repository](https://github.com/thunderbird/thunderbird-android)
2. Click the **Fork** button in the top-right corner
3. Select your GitHub account as the destination for the fork
4. Wait for GitHub to create your fork

## 📥 Cloning Your Fork

After forking, clone your fork to your local machine:

```bash
# Clone your fork
git clone https://github.com/YOUR-USERNAME/thunderbird-android.git

# Navigate to the project directory
cd thunderbird-android

# Add the upstream repository as a remote to your fork
git remote add upstream https://github.com/thunderbird/thunderbird-android.git
```

Replace `YOUR-USERNAME` with your GitHub username.

## 🌿 Creating a Branch

Always create a new branch from the latest `main`:

```bash
# Ensure you're on the main branch
git checkout main

# Pull latest changes
git pull upstream main

# Create a new branch
git checkout -b fix-issue-123
```

Use a descriptive branch name that reflects the issue you're addressing, such as:
- `fix-issue-123`
- `add-feature-xyz`
- `improve-performance-abc`

## 💻 Making Changes

When making changes:

1. Follow the [Code Quality Guide](code-quality-guide.md) for styling and tooling.
2. Keep your changes focused on the specific issue.
3. Write clear, concise, and well-documented code
4. Document non-obvious logic and update docs if needed.
5. Add or update tests (see [Testing Guide](testing-guide.md))

### ✍️ Commit Best Practices

- Write clear commit messages following the [Git Commit Guide](git-commit-guide.md)
- Use [Conventional Commits](git-commit-guide.md#-commit-message-format)
- Make small, focused commits that address a single concern
- Reference the issue number in your commit message (e.g., "Fix #123: Add validation for email input")

Example of a good commit message:

```git
fix(email): add validation for email input

Add regex pattern for email validation.
Display error message for invalid emails.
Add unit tests for validation logic.
Fixes #123
```

## 🧪 Testing and Checks

Before submitting your changes:

1. Run the existing tests to ensure you haven't broken anything:

   ```bash
   ./gradlew test
   ```
2. Write new tests for your changes:
   - Unit tests for business logic
   - Integration tests for component interactions
   - UI tests for user interface changes
3. Ensure all tests pass:

   ```bash
   ./gradlew check
   ```
4. Run lint checks to ensure code quality:

   ```bash
   ./gradlew lint
   ```

For more detailed information about testing, see the [Testing Guide](testing-guide.md).

## 📤 Pushing Changes

Once your changes are ready:

```bash
# Push your branch to your fork
git push origin your-branch-name
```

If you rebased:

```bash
git push --force-with-lease origin your-branch-name
```

## 📬 Submitting a Pull Request

To submit your changes for review:

1. Go to the [Thunderbird for Android repository](https://github.com/thunderbird/thunderbird-android)
2. Click **Pull requests** -> **New pull request** -> **compare across forks**
3. Set:
   - Base repo: `thunderbird/thunderbird-android`
   - Base branch: `main`
   - Head repo: your fork & branch
4. Select your fork and branch as the source
5. Make sure your [Pull Request Description](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing-workflow.md#pull-request-description)
   is compliant with our guidelines
6. Click **Create pull request**

### Pull Request Description

Before start writing the description, read the Pull Request template that will be prompted for you.

Write a clear and concise description for your pull request:

1. Reference the issue number (e.g., "Fixes #123", "Resolves #456")
2. Summarize the changes you made
3. Explain your approach and any important decisions
4. Include screenshots or videos for UI changes
5. Mention any related issues or pull requests

#### Mandatory fields

When writing your Pull Request Description, you must fill all the mandatory fields:

- **Linked Issue/Ticket**
  - Must use closing keywords, e.g. Closes, Fixes, Resolves, etc. See [Linking a pull request to an issue using a keyword](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword)
    for available keywords.
- **Description**
- **AI Disclosure**
  - Must check at least one of the checkboxes
- **Contribution Checklist**
  - Must follow all the instructions of the checklist and then mark as completed by checking all.

An automated PR Sentinel will be checking if you are following the requirements and will flag the PR if not.
See [Automated PR checks (PR Sentinel)](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/contribution-workflow.md#-automated-pr-checks-pr-sentinel)
for more details.

#### Example

```markdown
## Contribution Summary

Linked Issue/Ticket: Closes #123
RFC / Technical Design (if applicable):

#### Description

This PR adds email validation to the login form. It:
- Implements regex-based validation for email inputs
- Shows error messages for invalid emails
- Adds unit tests for the validation logic

#### Screenshots

[Screenshot of the error message]

#### Testing

1. Enter an invalid email (e.g., "test@")
2. Verify that an error message appears
3. Enter a valid email
4. Verify that the error message disappears

## AI Disclosure

Select **one** of the following (mandatory)

- [x] This contribution does not include any changes created or assisted by AI.
- [ ] This contribution includes changes assisted by AI.
- [ ] This contribution includes changes created by AI.

## Contribution Checklist

- [x] I have read, and I affirm that my contribution adheres to [Mozilla’s Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/)
- [x] This contribution is in Kotlin where possible
- [x] This contribution does not use merge commits
- [x] This contribution adheres to the existing codestyle (run `gradlew spotlessCheck` to check and `gradlew spotlessApply` to format your source code; will be checked by CI).
- [x] This contribution does not break existing unit tests (run `gradlew testDebugUnitTest`; will be checked by CI).
- [x] This contribution includes tests for any new functionality, and maintains tests for any updated functionality.
- [x] This contribution adheres to our [Engineering process](https://github.com/thunderbird/thunderbird-android/tree/main/docs/engineering) (RFC/Technical Design/ADR)
- [x] This PR has a descriptive title and body that accurately outlines all changes made, and contains a reference to any issues that it fixes (e.g. _Closes #XXX_ or _Fixes #XXX_).

```

## 👀 Code Review Process

After submitting your pull request:

1. Automated checks will run to verify your changes once approved by a maintainer.
2. Maintainers and other contributors will review your code.
3. They may suggest changes.
4. Respond to feedback and make necessary changes.
5. Push additional commits to your branch as needed.
6. Once approved, a maintainer will merge your pull request.

👉 For expectations and etiquette, see [Code Review Guide](code-review-guide.md).

### 🤖 Automated PR checks (PR Sentinel)

Every PR is validated automatically to verify if the PR is ready for review. To be reviewable it must have:

1. A **linked issue** in the description (e.g. `Closes #123`).
2. A **Conventional Commit title** (see the [Git Commit Guide](git-commit-guide.md)).
3. **Conventional Commit messages** on every commit, with **no `Co-authored-by:` trailers**.
4. A completed **AI Disclosure** section (exactly one option selected).
5. **No merge commits** — rebase onto the base branch instead of merging.

When something is missing, PR Sentinel posts a single comment listing it and labels the PR
`pr-sentinel: needs updates`. If it stays unresolved, the bot posts a closing warning after **1 day**
and **auto-closes the PR after 3 days**.

Push a fix (or edit the description), and the comment is removed, the `pr-sentinel: needs updates`
label is swapped for **`pr-sentinel: ready for review`**, and the check turns green.

**Draft PRs are skipped** until marked ready for review, and **bot PRs** (Dependabot, Renovate, …) are exempt.
Everyone else — **including maintainers and members** — must comply.

## 🔄 Keeping Your Fork Updated

To keep your fork in sync with the main repository:

```bash
# Fetch changes from the upstream repository
git fetch upstream

# Checkout your local main branch
git checkout main

# Merge changes from upstream/main into your local main branch
git merge upstream/main

# Push the updated main branch to your fork
git push origin main
```

If you're working on a branch and need to update it with the latest changes from main:

```bash
# Checkout your branch
git checkout your-branch-name

# Rebase your branch on the latest upstream main
git rebase upstream/main

# Force push the updated branch to your fork
git push --force-with-lease origin your-branch-name
```

## 🔁 Iterative Development

Most pull requests go through several rounds of feedback and changes:

1. Submit initial implementation
2. Receive feedback
3. Make changes
4. Request re-review
5. Repeat until approved

## 📝 After Your Pull Request is Merged

After your pull request is merged:

1. Delete your branch on GitHub.
2. Update your local repository:

   ```bash
   git checkout main
   git pull upstream main
   git push origin main
   ```
3. Delete your local branch:

   ```bash
   git branch -d your-branch-name
   ```
4. Celebrate your contribution! 🎉

## 🚫 Common Issues and Solutions

### Merge Conflicts

If your branch has conflicts with the main branch:

```bash
git fetch upstream
git checkout fix-issue-123
git rebase upstream/main
# resolve conflicts, then
git add .
git rebase --continue
git push --force-with-lease origin fix-issue-123
```

### Failed CI Checks

If continuous integration checks fail:

1. Check the CI logs to understand the failure.
2. Fix the issues locally.
3. Commit and push your changes.
4. CI checks will automatically run again.

## 🙏 Contribution Etiquette

- Be respectful and professional in all interactions.
- Follow the [Mozilla Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/)
- Be patient with the review process.
- Help review other contributors' pull requests.
- Ask questions if you're unsure about something.
- Thank others for their help and feedback.

👉 See [Code review guide](code-review-guide.md) for more details.
