# Konsist quality checks

This module checks repository conventions by reading Kotlin sources with Konsist. Both repository checks and tests of
the validation logic live under `src/test/kotlin/net/thunderbird/quality/`, grouped by subject (`di`, `logging`,
`testing`, and `util`). No application is started.

## Naming and structure

| Name | Purpose | Example |
|---|---|---|
| `Validate*` | Checks the real repository against a convention. | `ValidateMockitoUsage` |
| `*RuleTest` | Tests validation logic against small synthetic fixtures, independently of repository findings. | `MockitoUsageRuleTest` |
| Other `*Test` | Tests reporting or other helpers, not a repository rule. | `QualityViolationTest`, `KoinQualitySummaryTest` |

Use the same name for the class and its Kotlin file. Keep a rule's regression tests in the same subject package as its
repository check. Shared scopes and violation-reporting helpers belong in `util`.

For a new rule:

1. Add a `Validate*` check that uses a shared repository scope and reports actionable file locations through
   `QualityViolation` and `assertNoQualityViolations`.
2. Keep the validation logic callable with fixture inputs rather than making it read the repository itself.
3. Add a `*RuleTest` covering accepted inputs, rejected inputs, and relevant edge cases. Existing fixture tests use
   temporary directories with `Konsist.scopeFromExternalDirectory`; remove those directories in `finally`.

## Running checks

Run commands from the repository root.

All repository checks, rule regression tests, and helper tests:

```sh
./gradlew :quality:konsist:test
```

Repository checks only:

```sh
./gradlew :quality:konsist:test --tests '*Validate*'
```

Rule regression tests only:

```sh
./gradlew :quality:konsist:test --tests '*RuleTest'
```

Mockito check and its regression tests:

```sh
./gradlew :quality:konsist:test --tests '*ValidateMockitoUsage' --tests '*MockitoUsageRuleTest'
```

## Mockito usage

`testing/ValidateMockitoUsage.kt` permits `org.mockito.*` imports in files ending in `DiModuleTest.kt`. Other files must
use fakes unless their exact repository-relative path is in `existingMockitoUsers`.

The allowlist records existing non-DI Mockito usage, including existing test helpers. It is not permission to add more
Mockito-based non-DI tests. Remove an entry when the file is deleted, no longer imports Mockito, or becomes a DI test;
stale entries fail the check. If an existing exception is moved, update its exact path rather than adding a directory
pattern or a filename-only exception.

The check examines Kotlin imports, including aliases and wildcard imports. It does not inspect Java sources or
fully qualified Mockito calls that have no import.
