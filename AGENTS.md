# AI Agent Guide for Thunderbird for Android

This file defines requirements for AI coding agents and automated systems contributing to this repository.

AI-generated or AI-assisted contributions are acceptable only if they comply with these rules and meet the same
standards as human-written contributions.

## Applicability

These requirements apply to:

- All modules in this repository
- All pull requests created fully or partially using AI tools
- Automated refactoring, formatting, or code generation

## Repository Context

Thunderbird for Android is a privacy-focused email client.

The repository implements a white-label architecture producing:

- `app-thunderbird`: Thunderbird for Android
- `app-k9mail`: K-9 Mail

Project documentation resides in the `docs/` directory.
Architectural Decision Records (ADRs) are located in `docs/architecture/adr/`.

Agents MUST consult relevant documentation before making architectural or structural changes.

## Required Agent Workflow

Before making changes, agents MUST:

### 1. Understand the Request

- Confirm that requirements are clear and consistent with project rules
- If requirements are incomplete, ambiguous, or conflicting:
  - **Do NOT guess**
  - Document assumptions
  - Request clarification before proceeding

### 2. Research Context

- Read `README.md`, `docs/CONTRIBUTING.md`, and relevant documentation in `docs/`
- Review existing implementations in affected modules
- Check for related ADRs in `docs/architecture/adr/`
- Understand the module's role in the white-label architecture

### 3. Make Changes

- Modify **only** files directly related to the requested change
- Follow existing patterns and conventions in the affected modules
- Maintain consistency with the established architecture

### 4. Verify Changes

- Run appropriate Gradle tasks (see [Build and Verification Requirements](#build-and-verification-requirements))
- Ensure all checks pass before proposing changes

## Architectural Requirements

### Module Types

- `app-*` — Application entry points (`app-thunderbird`, `app-k9mail`)
- `app-common` — Wiring layer for features and dependency injection
- `feature:*` — User-facing features (split into `:api` and `:internal` modules per ADR-0009)
- `core:*` — Shared infrastructure and utilities (split into `:api` and `:internal` modules per ADR-0009)
- `library:*` — Reusable libraries
- `legacy:*` — Migration targets (contains original K-9 Mail codebase; avoid adding new logic here)

### API / Internal Boundary

Agents MUST:

- Depend only on other modules' `:api` modules
- Never depend on another module's `:internal` or `:internal-*` modules
- Only `app-common`, `app-thunderbird`, and `app-k9mail` may depend on `:internal` modules (for DI wiring)
- Keep implementation details internal using the `internal` modifier
- Bind implementations in `app-common` or app modules only

New code MUST NOT violate the API/internal boundary, see [ADR-0009](docs/architecture/adr/0009-api-internal-split.md).

If existing code violates this boundary, agents MUST NOT replicate the pattern and SHOULD move the code toward the
intended architecture when modifying it.

Agents MUST NOT change module structure, dependency graphs, or architectural boundaries unless explicitly requested.

## Technology Requirements

Agents MUST use:

- Kotlin for new code
- Jetpack Compose for UI (mandatory for new features)
- Atomic Design system components (see `docs/architecture/design-system.md`)
- Koin for dependency injection (constructor injection)
- Coroutines and Flow for concurrency
- MVI (Unidirectional Data Flow) pattern (see `docs/architecture/ui-architecture.md`)

Testing libraries:

- `assertk`
- `kotlinx-coroutines-test`
- Turbine

Testing policy:

- Prefer **fakes over mocks** (see [Testing Guide](docs/contributing/testing-guide.md))
- Avoid mocking frameworks unless strongly justified
- Use Arrange-Act-Assert (AAA) pattern
- Name the object under test `testSubject`

Agents MUST NOT introduce alternative frameworks.

## Coding Requirements

Agents MUST:

- Make small, focused, reviewable changes
- Prioritize privacy, security, and correctness over convenience or shorter code
- Modify only files directly related to the requested change
- Follow the exact naming and formatting conventions of the file and module being modified
- NOT reformat, modernize, or clean up unrelated code
- Avoid speculative refactoring

### UI Constraints

- Use Atomic Design components from the design system (see `docs/architecture/design-system.md`)
- Raw Material components are NOT allowed outside design system modules and the catalog app
- For existing View-based code, maintain consistency using legacy design system components
- Do NOT introduce new design systems; extend the existing system within its modules

### Logging and Privacy

Privacy is a core value of this project.

Agents MUST:

- Use `net.thunderbird.core.logging.Logger` via dependency injection
- NEVER log PII (Personally Identifiable Information)
- NEVER log credentials, passwords, or authentication tokens
- NEVER log message content or email addresses

## Security Requirements

This is a privacy-focused email client. Security is non-negotiable.

Agents MUST NOT:

- Add telemetry, analytics, ads, or tracking of any kind
- Add permissions without explicit request and justification
- Introduce insecure cryptography or unsafe networking
- Hardcode secrets, credentials, API keys, or tokens
- Modify OAuth configuration unless explicitly instructed
- Change licensing headers or license terms
- Introduce dependencies with known vulnerabilities

All external input MUST be treated as untrusted. Validate and sanitize user input.

## Build and Verification Requirements

Before proposing changes, agents MUST run the narrowest relevant Gradle tasks and ensure they pass.

### Build

- `./gradlew assemble`
- `./gradlew build`
- `./gradlew :app-thunderbird:assembleDebug`
- `./gradlew :app-k9mail:assembleDebug`

### Tests

- `./gradlew test`
- `./gradlew connectedAndroidTest`

### Code Quality

- `./gradlew lint`
- `./gradlew detekt`
- `./gradlew spotlessCheck`
- `./gradlew spotlessApply` (to fix formatting issues)

### Bug Fix Requirements

When fixing bugs, agents MUST:

- Add or update tests to cover the bug
- Ensure tests **fail before the fix** (when applicable)
- Ensure all relevant tasks **pass after the fix**
- Run at least: `./gradlew test lint detekt spotlessCheck`

### Limitations

If required tasks cannot be executed locally (e.g., no Android device/emulator for `connectedAndroidTest`):

- Agents MUST explicitly state which tasks were not run and why
- Include this information in the pull request description

## Commit Requirements

This repository uses Conventional Commits for all commit messages.

Agents MUST:

- Use appropriate prefixes (`feat:`, `fix:`, `refactor:`, `style:`, `test:`, `chore:`)
- Keep commits small and logically scoped
- Separate formatting-only changes into `style:` commits
- Separate refactoring from functional changes
- Avoid mixing behavior changes and formatting in a single commit

## Pull Request Requirements

Pull requests MUST include:

- A clear description of changes
- The reason for the change
- Exact Gradle commands used for testing
- Known risks or trade-offs
- Disclosure of AI assistance (if applicable)

AI assistance does not reduce review standards.

## Escalation

### When to Stop and Ask

If uncertain about:

- Requirements (incomplete, ambiguous, or conflicting)
- Architectural decisions
- Module boundaries or dependencies
- Technology choices
- Security implications

**Then:**

1. Stop — Do not proceed with uncertain changes
2. Document assumptions — Write down what you understand and what's unclear
3. Request clarification — Ask specific questions

### What NOT to Do

Agents MUST NOT:

- Invent architecture or design patterns
- Bypass module boundaries to "make it work"
- Prioritize elegance over established project conventions
- Guess at requirements or implementation details
- Make breaking changes without explicit approval

When in doubt, ask. It's always better to clarify than to guess wrong.
