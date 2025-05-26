# 📖 Glossary of Terms

This glossary provides definitions for technical terms, project-specific terminology, and abbreviations used throughout the Thunderbird for Android project documentation.

## A

### AAPT (Android Asset Packaging Tool)

A build tool that compiles resources into a binary format that can be efficiently loaded by the Android system.

### AAA (Arrange-Act-Assert)

A pattern for organizing unit tests into three sections: Arrange (set up test conditions), Act (perform the action being tested), and Assert (verify the expected outcomes).

### AAR (Android Archive)

A file format used for Android libraries that contains compiled code (as a JAR file) and resources.

### ADR (Architecture Decision Record)

A document that captures an important architectural decision made along with its context and consequences.

### AOSP (Android Open Source Project)

The open-source project that maintains and develops Android.

### API (Application Programming Interface)

A set of definitions, protocols, and tools for building software and applications.

### API Module

A module that contains public interfaces, data models, and contracts that define a module's capabilities and can be depended upon by other modules.

### APK (Android Package)

The package file format used by the Android operating system for distribution and installation of mobile apps.

### AssertK

A fluent assertion library for Kotlin that provides a rich set of assertions for testing.

## B

### Build Variant

A combination of build type and product flavor that determines how an app is built and packaged.

## C

### Clean Architecture

An architectural pattern that separates software into concentric layers (UI, domain, and data) with dependencies pointing inward, promoting separation of concerns and testability.

### Compose

Jetpack Compose is Android's modern toolkit for building native UI. It simplifies and accelerates UI development on Android.

### Conflict Resolution

A strategy for resolving conflicts that occur when data is modified both locally and remotely in an offline-first application.

### Coroutines

A Kotlin feature that simplifies asynchronous programming by making asynchronous code sequential.

### Cross-Cutting Concerns

Aspects of a system that affect multiple components, such as logging, error handling, and security.

## D

### DAO (Data Access Object)

A pattern that provides an abstract interface to a database or other persistence mechanism.

### Data Source Pattern

A design pattern that abstracts the source of data behind a clean API, allowing the application to retrieve data from different sources (local, remote) through a consistent interface.

### Dependency Injection

A technique whereby one object supplies the dependencies of another object.

### Domain Model

A conceptual model of the domain that incorporates both behavior and data.

## E

### E2E (End-to-End) Encryption

A system of communication where only the communicating users can read the messages.

### ESMTP (Extended Simple Mail Transfer Protocol)

An extension of the Simple Mail Transfer Protocol (SMTP) that adds features like authentication.

## F

### Fake Implementation

A test implementation of an interface or class that provides controlled behavior for testing purposes, preferred over mocks in this project.

### Flow

In Kotlin, a type that can emit multiple values sequentially, as opposed to suspend functions that return only a single value.

### Fragment

A portion of the user interface in an Android app, representing a behavior or a portion of the UI.

## I

### IMAP (Internet Message Access Protocol)

An Internet standard protocol used by email clients to retrieve email messages from a mail server.

### Implementation Module

A module that contains concrete implementations of interfaces defined in an API module, along with internal components, data sources, and UI components.

### Intent

A messaging object in Android that is used to request an action from another app component.

## J

### JVM (Java Virtual Machine)

An execution environment that enables a computer to run Java programs as well as programs written in other languages that are also compiled to Java bytecode.

## K

### Koin

A lightweight dependency injection framework for Kotlin.

### Kotlin

A modern programming language that runs on the JVM and is fully interoperable with Java.

## L

### LiveData

An observable data holder class that is lifecycle-aware, meaning it respects the lifecycle of other app components.

## M

### Material Design

A design system developed by Google that provides guidelines for visual, motion, and interaction design across platforms and devices.

### MIME (Multipurpose Internet Mail Extensions)

An Internet standard that extends the format of email messages to support text in character sets other than ASCII, as well as attachments.

### Modularization

An approach to software development where the codebase is divided into multiple distinct modules, each encapsulating specific functionality that can be developed, tested, and maintained independently.

### MVI (Model-View-Intent)

An architectural pattern for building user interfaces, particularly in Android applications, that provides a unidirectional data flow and clear separation between UI state and UI logic.

## O

### OAuth

An open standard for access delegation, commonly used as a way for Internet users to grant websites or applications access to their information on other websites.

### Offline-First Approach

A design approach where applications are built to work without an internet connection first, with online functionality as an enhancement. It involves local data storage, background synchronization, and operation queueing.

### One-Way Dependencies

A principle in modular architecture where dependencies flow in one direction only, preventing circular dependencies between modules.

### OpenPGP

An encryption standard that provides cryptographic privacy and authentication for data communication.

### Operation Queueing

A technique used in offline-first applications to queue operations that require network connectivity and execute them when a connection becomes available.

## P

### POP3 (Post Office Protocol 3)

A standard protocol used by email clients to retrieve email from a mail server.

### Product Flavor

A customization of an Android app that can be built differently for different clients, brands, or versions.

## R

### Repository Pattern

A design pattern that isolates the data layer from the rest of the app and provides a clean API for data access.

### Room

A persistence library that provides an abstraction layer over SQLite to allow for more robust database access.

## S

### Single Source of Truth

A principle where data is stored in only one place and all other components access it from that single source, ensuring consistency across the application.

### SMTP (Simple Mail Transfer Protocol)

An Internet standard for email transmission.

### SSL/TLS (Secure Sockets Layer/Transport Layer Security)

Cryptographic protocols designed to provide communications security over a computer network.

### StateFlow

A state-holder observable flow that emits the current and new state updates to its collectors.

## T

### TFA (Thunderbird for Android)

The Android version of the Thunderbird email client.

### Timber

A logging library for Android that provides utility on top of Android's normal Log class.

## U

### UI (User Interface)

The space where interactions between humans and machines occur.

### Unidirectional Data Flow

A pattern where data flows in one direction only, typically from the ViewModel to the UI, making the application state more predictable and easier to debug.

### Use Case

A specific situation in which a product or service could potentially be used, often used in software development to describe how a user might interact with a system. In Clean Architecture, a use case encapsulates a single business operation or action.

### UUID (Universally Unique Identifier)

A 128-bit label used for information in computer systems.

## V

### ViewModel

A class designed to store and manage UI-related data in a lifecycle conscious way.

### ViewBinding

A feature that allows you to more easily write code that interacts with views.

## W

### White Label

A product or service produced by one company that other companies rebrand to make it appear as if they had made it.

### WorkManager

An Android Jetpack library that makes it easy to schedule deferrable, asynchronous tasks that are expected to run even if the app exits or the device restarts.
