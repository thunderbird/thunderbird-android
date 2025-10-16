# Summary

This file is not intended for direct reading by users, but rather serves as a configuration file for the documentation
generator, in this case, **mdbook**. It defines the structure and navigation of the documentation.

[About Thunderbird for Android](about.md)

---

- [Contributing](CONTRIBUTING.md)
  - [Development Environment](contributing/development-environment.md)
  - [Contribution Workflow](contributing/contribution-workflow.md)
  - [Development Guide](contributing/development-guide.md)
  - [Code Quality Guide](contributing/code-quality-guide.md)
  - [Code Review Guide](contributing/code-review-guide.md)
  - [Git Commit Guide](contributing/git-commit-guide.md)
  - [Testing Guide](contributing/testing-guide.md)
  - [Translations](contributing/translations.md)
  - [Managing Strings](contributing/managing-strings.md)
  - [Java to Kotlin Conversion Guide](contributing/java-to-kotlin-conversion-guide.md)
- [Architecture](architecture/README.md)
  - [Module Organization](architecture/module-organization.md)
  - [Module Structure](architecture/module-structure.md)
  - [Feature Modules](architecture/feature-modules.md)
  - [UI Architecture](architecture/ui-architecture.md)
  - [Theme System](architecture/theme-system.md)
  - [Design System](architecture/design-system.md)
  - [User Flows](architecture/user-flows.md)
  - [Legacy Module Integration](architecture/legacy-module-integration.md)
  - [Architecture Decision Records](architecture/adr/README.md)
    - [Accepted]()
      - [0001 - Switch From Java to Kotlin](architecture/adr/0001-switch-from-java-to-kotlin.md)
      - [0002 - UI - Wrap Material Components in Atomic Design System](architecture/adr/0002-ui-wrap-material-components-in-atomic-design-system.md)
      - [0003 - Test - Switch Test Assertions From Truth to Assertk](architecture/adr/0003-switch-test-assertions-from-truth-to-assertk.md)
      - [0004 - Naming Conventions for Interfaces and Their Implementations](architecture/adr/0004-naming-conventions-for-interfaces-and-their-implementations.md)
      - [0005 - Central Project Configuration](architecture/adr/0005-central-project-configuration.md)
      - [0006 - White Label Architecture](architecture/adr/0006-white-label-architecture.md)
      - [0007 - Project Structure](architecture/adr/0007-project-structure.md)
      - [0008 - Change Shared Module package to `net.thunderbird`](architecture/adr/0008-change-shared-modules-package-name.md)
    - [Proposed]()
    - [Rejected]()
- [User Guide]()
  - [Setup]()
    - [Installing ADB](user-guide/setup/installing-adb.md)
  - [Troubleshooting]()
    - [Collecting Debug Logs](user-guide/troubleshooting/collecting-debug-logs.md)
    - [Find your app version](user-guide/troubleshooting/find-your-app-version.md)
- [Release](ci/README.md)
  - [Release Process](ci/RELEASE.md)
  - [Release Automation](ci/AUTOMATION.md)
  - [Developer Release Checklist](release/developer-checklist.md)
  - [Manual Release (historical)](ci/HISTORICAL_RELEASE.md)
- [Security]()
  - [Threat Modeling Guide](security/threat-modeling-guide.md)

---

[How to Document](HOW-TO-DOCUMENT.md)
