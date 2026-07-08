# Git Commit Guide

Use [Conventional Commits](https://www.conventionalcommits.org/) to write consistent and meaningful commit messages.
This makes your work easier to review, track, and maintain for everyone involved in the project.

## ✍️ Commit Message Format

> [!IMPORTANT]
> **Enforced in CI:**
> - the PR title **and every commit subject** must match this format (checked by the [PR Sentinel workflow](https://github.com/thunderbird/thunderbird-android/blob/main/.github/workflows/pr-sentinel.yml)).
> - The `<description>` must be 70 characters or fewer.
>
> See [Automated PR checks (PR Sentinel)](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/contribution-workflow.md#-automated-pr-checks-pr-sentinel)
> for more details.

```git
<type>(<scope>): <description>

<body>

<footer(s)>
```

Components:

- `<type>`: The [type of change](#-commit-types) being made (e.g., feat, fix, docs).
- `<scope>` **(optional)**: The [scope](#optional-scope) indicates the area of the codebase affected by the change (e.g., auth, ui).
- `<description>`: Short description of the change (70 characters or less)
- `<body>` **(optional)**: Explain what changed and why, include context if helpful.
- `<footer(s)>` **(optional)**: Include issue references, breaking changes, etc.

### Examples

Basic:

```git
feat: add QR code scanner
```

With scope:

```git
feat(auth): add login functionality
```

With body and issue reference:

```git
fix(api): handle null response from login endpoint

Checks for missing tokens to prevent app crash during login.

Fixes #123
```

### 🏷️ Commit Types

|    Type    |            Use for...            |                  Example                  |
|------------|----------------------------------|-------------------------------------------|
| `feat`     | New features                     | `feat(camera): add zoom support`          |
| `fix`      | Bug fixes                        | `fix(auth): handle empty username crash`  |
| `docs`     | Documentation only               | `docs(readme): update setup instructions` |
| `style`    | Code style (no logic changes)    | `style: reformat settings screen`         |
| `refactor` | Code changes (no features/fixes) | `refactor(nav): simplify stack setup`     |
| `perf`     | Performance improvements         | `perf(list): cache adapter lookups`       |
| `test`     | Adding/editing tests             | `test(api): add unit test for login`      |
| `chore`    | Tooling, CI, dependencies        | `chore(ci): update GitHub Actions config` |
| `build`    | Build system changes             | `build(gradle): raise JVM target`         |
| `ci`       | CI configuration                 | `ci: add PR Sentinel workflow`            |
| `revert`   | Reverting previous commits       | `revert: remove feature flag`             |

### 📍Optional Scope

The **scope** is optional but recommended for clarity, especially for large changes or or when multiple areas of the
codebase are involved.

|   Scope    |   Use for...   |                 Example                  |
|------------|----------------|------------------------------------------|
| `auth`     | Authentication | `feat(auth): add login functionality`    |
| `settings` | User settings  | `feat(settings): add dark mode toggle`   |
| `build`    | Build system   | `fix(build): improve build performance`  |
| `ui`       | UI/theme       | `refactor(ui): split theme into modules` |
| `deps`     | Dependencies   | `chore(deps): bump Kotlin to 2.0.0`      |

## 🧠 Best Practices

### 1. One Commit, One Purpose

- ✅ Each commit should represent a single logical change or addition to the codebase.
- ❌ Don’t mix unrelated changes together (e.g., fixing a bug and updating docs, or changing a style and
  adding a feature).

### 2. Keep It Manageable

- ✅ Break up large changes into smaller, more manageable commits.
- ✅ If a commit changes more than 200 lines of code, consider breaking it up.
- ❌ Avoid massive, hard-to-review commits.

### 3. Keep It Working

- ✅ Each commit should leave the codebase in a buildable and testable state.
- ❌ Never commit broken code or failing tests.

### 4. Think About Reviewers (and Future You)

- ✅ Write messages for your teammates and future self, assuming they have no context.
- ✅ Explain non-obvious changes or decisions in the message body.
- ✅ Consider the commit as a documentation tool.
- ❌ Avoid jargon, acronyms, or vague messages like `update stuff`.

## Summary

- Use [Conventional Commits](#-conventional-commits) for consistency.
- Keep commit messages short, structured, and focused.
- Make each commit purposeful and self-contained.
- Write commits that make collaboration and future development easier for everyone—including you.

