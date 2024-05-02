# Naming Conventions for Interfaces and Their Implementations

- Pull Request: [#7794](https://github.com/thunderbird/thunderbird-android/pull/7794)

## Status

- **Accepted**

## Context

When there's an interface that has multiple implementations it's often easy enough to give meaningful names to both the
interface and the implementations (e.g. the interface `Backend` with the implementations `ImapBackend` and
`Pop3Backend`). Naming becomes harder when the interface mainly exists to allow having isolated unit tests and the
production code contains exactly one implementation of the interface.
Prior to this ADR we didn't have any naming guidelines and the names varied widely. Often when there was only one
(production) implementation, the class name used one of the prefixes `Default`, `Real`, or `K9`. None of these had any
special meaning and it wasn't clear which one to pick when creating a new interface/class pair.

## Decision

We'll be using the following guidelines for naming interfaces and their implementation classes:

1. **Interface Naming:** Name interfaces as if they were classes, using a clear and descriptive name. Avoid using the
   "IInterface" pattern.
2. **Implementation Naming:** Use a prefix that clearly indicates the relationship between the interface and
   implementation, such as `DatabaseMessageStore` or `InMemoryMessageStore` for the `MessageStore` interface.
3. **Descriptive Names:** Use descriptive names for interfaces and implementing classes that accurately reflect their
   purpose and functionality.
4. **Platform-specific Implementations:** Use the platform name as a prefix for interface implementations specific to
   that platform, e.g. `AndroidPowerManager`.
5. **App-specific Implementations:** Use the prefix `K9` for K-9 Mail and `Tb` for Thunderbird when app-specific
   implementations are needed, e.g. `K9AppNameProvider` and `TbAppNameProvider`.
6. **Flexibility:** If no brief descriptive name fits and there is only one production implementation, use the prefix
   `Default`, like `DefaultImapFolder`.

## Consequences

- **Positive Consequences**

  - Improved code readability and maintainability through consistent naming.
  - Reduced confusion and misunderstandings by using clear and descriptive names.

- **Negative Consequences**

  - Initial effort is required to rename existing classes that do not follow these naming conventions.
