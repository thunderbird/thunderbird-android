# 👁️ Code Review Guide

This guide outlines best practices for creating and reviewing pull requests (PRs) in the Thunderbird for Android
project. It is intended to help both authors and reviewers ensure high-quality contributions.

### ✅ Quick PR checklist (for authors)

Paste this into your PR to self-check:

```markdown
- [ ] Focused scope (< ~800 LOC); clear description and rationale
- [ ] UI changes: screenshots/videos; accessibility (TalkBack, contrast, touch targets)
- [ ] Tests added/updated; CI green (see [Testing Guide](testing-guide.md))
- [ ] Architecture: business logic outside UI; module API/impl respected; DI via constructor/Koin
- [ ] Performance: no main-thread blocking; Compose recompositions reasonable; hot paths allocation-lean
- [ ] Security/privacy: inputs validated; no PII in logs; TLS; secure storage; permission changes documented
- [ ] i18n: No new localizable strings unless justified; translations policy followed
- [ ] Release train: feature flags set; uplift label + risk/impact (if applicable)
- [ ] Docs/CHANGELOG updated; issues linked (Fixes #123); PR title/commits clear
```

## 🧑‍💻 For Code Authors (self‑review checklist)

1. **Scope and clarity**
   - Keep the PR focused on a single concern; split large or mixed changes.
   - Keep PRs small (aim for <~ 800 lines of code (LOC))
   - Provide a clear description: problem, approach, rationale, alternatives considered.
   - For UI changes: include screenshots/videos for UI changes and note any UX impacts.
   - Use Draft PRs for early feedback
2. **Tests**
   - Include tests matching the change type (unit/integration/UI).
   - Use AAA pattern and use assertK. Prefer fakes over mocks.
   - Name tests descriptively; cover edge cases and error conditions.
   - See the [Testing Guide](testing-guide.md) for frameworks and best practises.
3. **Architecture & module boundaries**
   - Follow modular rules: API vs implementation separation; no leaking implementation across module boundaries.
   - Only depend on `:feature:foo:api` externally; `:feature:foo:impl` is internal.
   - Respect MVI/Compose patterns in the UI layer; keep business logic out of UI implementation.
   - Prefer constructor injection with Koin; keep constructors simple and dependencies explicit.
4. **Code quality & style**
   - Keep functions small, clear naming, avoid duplication.
   - Add KDoc for public API.
   - Run Spotless, Detekt and Lint locally.
   - Follow the [Code Quality Guide](code-quality-guide.md)
5. **Performance & threading**
   - Use coroutines with appropriate dispatchers; avoid blocking the main thread.
   - Watch allocations in hot paths, avoid unnecessary recompositions in Compose.
   - For critical changes, check baseline profiling or startup metrics.
6. **Security & privacy**
   - Validate inputs, avoid logging Personally Identifiable Information (PII).
   - Use TLS and safe storage APIs.
   - Review permission use and document your rationale to them in the PR description.
7. **Accessibility**
   - Provide `contentDescription` and TalkBack support.
   - Ensure sufficient contrast, touch targets and dynamic text sizing (up to 200%).
8. **i18n**
   - Follow strings policy: don’t modify translations here; avoid late string changes; see [managing strings](managing-strings.md).
   - No string concatenation with localized text; use placeholders.
9. **Feature flags & release train awareness**
   - Gate incomplete features behind flags aligned with branch rules [Release - Feature Flags](../release/RELEASE.md#feature-flags).
   - For uplifts: add label and risk/impact notes [Release - Branch uplifts](../release/RELEASE.md#branch-uplifts).
10. **Documentation & metadata**
    - Update relevant docs, CHANGELOG entries and add context as needed.
    - Link relevant issues using GitHub keywords so they auto-close on merge (`Fixes #123`, `Resolves #456`).
    - For commit format, see the [Git Commit Guide](git-commit-guide.md)
11. **CI status**
    - Ensure CI is green
    - Fix issues or request re-run if failures are unrelated/flaky.

## 👀 For Code Reviewers (what to look for)

1. **Correctness & requirements**
   - Does the change solve the stated problem? Any edge cases missed? Are invariants upheld?
2. **Architecture & boundaries**
   - Adheres to module API/impl separation and project architecture (UI: Compose/MVI, Domain, Data).
   - No cross‑module leaks; dependencies flow in the right direction.
3. **Readability & maintainability**
   - Code is easy to follow; good names; small functions; comments where necessary; public APIs documented.
4. **Test quality**
   - Adequate tests exist and are meaningful.
   - Negative/error paths covered.
   - Tests are deterministic and prefer fakes.
5. **Performance**
   - No obvious inefficiencies; avoids allocations on hot paths.
   - Background work is appropriate.
   - Compose recomposition reasonable.
6. **Security, privacy, and permissions**
   - No new vulnerabilities; safe defaults; least privilege permissions.
   - Secrets not committed; logs avoid personal identifiable information.
   - Permission rationale provided if applicable.
7. **Accessibility & i18n**
   - Accessible UI; strings externalized; no hard‑coded locales.
   - Respects translations policy, only english source files.
8. **Consistency & style**
   - Matches existing patterns.
   - Formatting and static analysis clean (Spotless/Detekt/Lint).
9. **Release train considerations**
   - Feature flags set correctly for target branch
   - Consider if an uplift is necessary.
10. **CI status**
    - CI is green -> good to merge.
    - If failures are unrelated or flaky, do a re-run OR leave a note.
    - Don’t merge with failing checks!

## 🤝 Review etiquette

- Be kind, specific, and actionable.
- Prefer questions over directives. Explain trade‑offs.
- Use severity tags if appropriate to weight your comments:
  - Nit: trivial style/readability; non-blocking.
  - Suggestion: improves design/maintainability; author’s call.
  - Blocking: must be addressed for correctness, safety, or architecture.
- Avoid scope creep and request follow‑ups for non‑critical issues.
- Acknowledge good practices and improvements.
- When disagreeing, provide reasoning and seek consensus.
- Use GitHub suggestions for trivial fixes where possible.

