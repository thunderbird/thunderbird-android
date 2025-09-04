# 🏗️ Development Guide

This document summarizes project-specific conventions for developers.

For full details, see:
- [Architecture](../architecture/README.md) - modules, layering, offline-first
- [Code Quality Guide](code-quality-guide.md) - style, static analysis, security, performance
- [Testing Guide](testing-guide.md) - test strategy and frameworks
- [Code Review Guide](code-review-guide.md) – review expectations

## 📦 Modules

- Follow [Module Organization](../architecture/module-organization.md) and [Module Structure](../architecture/module-structure.md)
- Place new code in `feature:*`, `core:*`, or `library:*` modules
- **Do not** add new code in `legacy:*` modules
- Keep **API/impl separation** in all modules

## 🏛️ Architecture

- Follow [Architecture](../architecture/README.md)
- Dependencies must flow in **one direction only**
- UI built with **Jetpack Compose** + **MVI pattern**
- Domain logic implemented in **Use Cases**
- Data handled via **Repository pattern**

## ⚙️ Dependency Injection

- Use [Koin](https://insert-koin.io/) with constructor injection
- Avoid static singletons and service locators

## 🧪 Testing

- Follow the [Testing Guide](testing-guide.md) for frameworks and strategy.
- Project conventions:
  - Name the object under test `testSubject`
  - Prefer **fakes** over mocks when possible
  - Use descriptive test names and AAA (Arrange–Act–Assert) pattern

## 🎨 Code Style

- Follow the [Code Quality Guide](code-quality-guide.md) for formatting and tooling.
- Code style quick reminders
  - Prefer immutability (`val` over `var`)
  - Use extension functions for utilities
  - Keep functions small and focused

## Do’s and Don’ts

- ✅ Write modular, testable code with clear boundaries
- ✅ Document non-obvious logic and decisions (link ADRs when appropriate)
- ✅ Keep module boundaries clean
- ✅ Add or update tests for new/changed code
- ✅ Run Spotless, Detekt, and Lint checks locally before committing
- ❌ Don’t commit new code to `legacy:*` modules
- ❌ Don’t bypass the architecture and layering (e.g., UI calling data sources directly)

