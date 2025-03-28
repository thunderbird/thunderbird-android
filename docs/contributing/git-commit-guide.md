# Git Commit Guide

Commits are an essential part of an open-source project, as they document the history of changes and provide context for
future contributors and reviewers. A well-structured commit history makes it easier to understand how and why the
codebase evolved, aiding in debugging, reviewing, and knowledge transfer. This outlines best practices for
creating meaningful commits.

By grouping related changes, splitting large changes, using consistent naming conventions, and crafting clear commit
messages, contributors can significantly improve the quality of an open-source project's commit history. Keeping these
best practices in mind will help streamline development and make it easier for everyone involved to understand and
build upon the work effectively.

## Best Practices for Meaningful Commits

### 1. Group Related Changes

Each commit should represent a single logical change. A commit should:

- Contain only related modifications.
- Be small enough to understand but large enough to encompass a meaningful unit of change.
- Avoid including unrelated changes, such as formatting updates along with functional modifications.

### 2. Split Large Changes

If a change touches multiple aspects of the code or is too large to be reviewed easily, break it into multiple smaller
commits:

- Separate refactoring from feature implementation.
- Isolate bug fixes from new features.
- Ensure that each commit is functional and does not leave the repository in a broken state.

#### Rule of Thumb for Large Commits

- If a commit changes more than **200–300 lines of code**, consider splitting it.
- If multiple files across **different components or concerns** are modified, break them into separate commits.
- If reviewing the commit would take **longer than 10–15 minutes**, it's likely too big.

### 3. Use Consistent Commit Message Naming

Commit messages should start with a clear action verb that describes the change:

- **`Add`**: For new features, tests, or documentation.
- **`Change`**: For modifications to existing functionality.
- **`Fix`**: For bug fixes and patches.
- **`Remove`**: For deletions or removals of code or dependencies.
- **`Refactor`**: For restructuring code without changing functionality.
- **`Bump`**: For dependency version updates.

**Example:**

```
Add validation to login input to prevent SQL injection

Change login input validation to sanitize user inputs properly before being processed. Prevents potential SQL injection vulnerabilities.

Fixes #123.
```

### 4. Write Descriptive Commit Messages

A good commit message follows this structure:

- **Title (short summary, 50 characters max)**
- **Body (optional, but recommended for context)**
  - Explain *what* the change does and *why* it was needed.
  - Describe any important details, such as edge cases addressed.
  - Reference relevant issue numbers or discussions.

Avoid generic descriptions like "Fix bug" or "Update code"

### 5. Ensure Commits Are Self-Contained

Each commit should:

- Be complete and independently buildable.
- Not introduce temporary hacks or broken functionality.
- Ideally, pass tests to maintain project integrity.

### 6. Help Reviewers and Future Developers

Consider the commit history as a documentation tool. Someone reviewing past changes should be able to:

- Follow the logical evolution of a feature or fix.
- Understand *how* and *why* a functionality changed over time.
- Easily revert a commit if needed without breaking unrelated functionality.

### 7. Use Atomic Commits

Atomic commits mean that each commit represents a single unit of change that can be reverted independently. This
practice ensures that if an issue arises, it is easier to pinpoint and undo the specific problematic change.
