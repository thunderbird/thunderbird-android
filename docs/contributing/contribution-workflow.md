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
- Look for issues labeled [good first issue](https://github.com/thunderbird/thunderbird-android/labels/good%20first%20issue) if you're new to the project
- Avoid issues labeled [unconfirmed](https://github.com/thunderbird/thunderbird-android/labels/unconfirmed) as they are not yet ready for contributions

### Requesting New Features / Ideas

We don’t track new ideas or feature requests in GitHub Issues. Mozilla connect is where feature proposals, product
decisions, and larger design conversations happen.

- Start a discussion in [Mozilla Connect - Ideas](https://connect.mozilla.org/t5/ideas/idb-p/ideas/label-name/thunderbird%20android)
- Once a feature is accepted and work is planned, maintainers will create the corresponding GitHub issue(s).

### Reporting Bugs

If you’ve found a bug that’s not yet tracked:

- Open a [new GitHub issue](https://github.com/thunderbird/thunderbird-android/issues/new/choose)
- Use the bug template and provide detailed reproduction steps.

### Discussing Your Plan

Before coding:
1. Comment on the GitHub issue you want to work on.
2. Explain your intended approach.
3. For non-trivial changes, you may be asked to create a **[User Journey](../engineering/user-journeys/README.md)**, **[RFC](../engineering/rfcs/README.md)**, **[ADR](../engineering/adr/README.md)**, or **[Technical Design](../engineering/technical-designs/README.md)** to reach consensus before implementation.
4. Wait for maintainer feedback to ensure alignment and avoid duplicate work.

If the work appears larger than a single issue or pull request, or needs coordination across multiple features, tasks,
or contributors, ask maintainers whether it should be organized as a GitHub milestone issue. Milestone issues are
created by core maintainers. See the [Engineering Delivery Planning guide](../engineering/delivery-planning.md) for
milestone, feature, and task issue structure.

If the work does not match a dedicated milestone issue, maintainers may link it to a quarterly catch-all milestone issue
for community contributions or Android foundations work.

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
5. Click **Create pull request**

### Pull Request Description

Write a clear and concise description for your pull request:

1. Reference the issue number (e.g., "Fixes #123", "Resolves #456")
2. Summarize the changes you made
3. Explain your approach and any important decisions
4. Include screenshots or videos for UI changes
5. Mention any related issues or pull requests

Example:

```markdown
## Contribution Summary

Linked Issue/Ticket: #123
RFC / Technical Design (if applicable):

#### Description

This PR adds email validation to the login form. It:
- Implements regex-based validation for email inputs
- Shows error messages for invalid emails
- Adds unit tests for the validation logic

#### Screen Shots

[Screenshot of error message]

#### Testing

1. Enter an invalid email (e.g., "test@")
2. Verify that an error message appears
3. Enter a valid email
4. Verify that the error message disappears
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
