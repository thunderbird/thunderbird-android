# Thunderbird for Android Notification System - Architecture deep-dive

This system is responsible for creating and dispatching all user-facing notifications, including system tray
notifications and in-app messages.

At its core, this system uses the **Command Design Pattern**. The primary goal of this architecture is to **decouple**
the request for a notification from the underlying platform-specific code that displays it. This makes the system more
flexible, testable, and easier to extend.

## Modules

The notification system is organized in the following modules:

- **api**: Core interfaces and classes
- **impl**: The implementation module
- **testing**: The testing helper module that provides common fake implementation.

## Core Components

The architecture is divided into four main logical groups: **Client**, **Invoker**, **Command**, and **Receiver** as
shown in the diagram:

```mermaid
---
config:
  look: neo
  layout: elk
---
classDiagram
    class Notification

%% The Client that initiates the request
    namespace Client {
        class SomeAppViewModel {
            - sender: NotificationSender
            + onSendNotificationClicked()
        }
    }

%% The Invoker and its implementation
    namespace Invoker {
        class NotificationSender {
            <<interface>>
            + send(notification: Notification) Flow~Outcome~
        }
        class DefaultNotificationSender {
            - commandFactory: NotificationCommandFactory
        }

    %% The Factory for creating commands
        class NotificationCommandFactory {
            + create(notification: Notification) List~NotificationCommand~
        }
    }

%% The Command objects
    namespace Command {
        class NotificationCommand~Notification~ {
            <<abstract>>
            # notification: Notification
            # notifier: NotificationNotifier~Notification~
            + execute() Outcome
        }
        class SystemNotificationCommand
        class InAppNotificationCommand

        class Outcome
        class CommandOutcome {
            <<interface>>
        }
        class CommandOutcomeSuccess {
            <<data>>
        }
        class CommandOutcomeFailure {
            <<data>>
            + throwable: Throwable
        }
    }

%% The Receivers that perform the action
    namespace Receiver {
        class NotificationNotifier~Notification~ {
            <<interface>>
            + show(id: NotificationId)
            + dispose()
        }
        class SystemNotificationNotifier
        class InAppNotificationNotifier
    }

%% External dependencies used by Receivers
    namespace Platform Dependencies {
        class NotificationManager
    }

    class InAppNotificationEventBus
    class NotificationRegistry

%% Implementation and Inheritance
    DefaultNotificationSender --|> NotificationSender
    SystemNotificationCommand --|> NotificationCommand
    InAppNotificationCommand --|> NotificationCommand
    SystemNotificationNotifier --|> NotificationNotifier
    InAppNotificationNotifier --|> NotificationNotifier
    CommandOutcomeSuccess --|> CommandOutcome
    CommandOutcomeFailure --|> CommandOutcome
%% Core Pattern Relationships
    SomeAppViewModel "1" --* "1" NotificationSender: uses
    SomeAppViewModel ..> Notification: creates
    DefaultNotificationSender --> NotificationCommandFactory: uses
    NotificationCommandFactory ..> SystemNotificationCommand: creates
    NotificationCommandFactory ..> InAppNotificationCommand: creates
    NotificationCommand "1" --* "1" NotificationNotifier: has-a
    NotificationCommand ..> Outcome: returns
%% Receiver Dependencies
    SystemNotificationNotifier ..> NotificationManager: uses
    InAppNotificationNotifier ..> InAppNotificationEventBus: uses
    InAppNotificationNotifier ..> NotificationRegistry: uses
%% Outcome Composition
    Outcome --* "1" CommandOutcome
```

### The Client

In the classic Command Pattern, the Client is often responsible for creating the command and setting its receiver.
However, in our implementation, the Client's role is simplified.

- **Implementation:** Any `ViewModel` (e.g., `ProfileViewModel`, `SettingsViewModel`).
- **Responsibilities:**
    - Constructs a concrete `Notification` data object based on user action or business logic.
    - Holds a reference to the `NotificationSender` (the [Invoker](#the-invoker)).
    - Calls `notificationSender.send()` to initiate the request.
    - Consumes the `Flow<Outcome>` to react to the result.

### The Invoker

The **Invoker** holds a command and asks it to be executed. It is completely decoupled from the action itself.

- **Implementation:** `NotificationSender` (Interface) and `DefaultNotificationSender` (Concrete Class).
- **Responsibilities:**
    - The `DefaultNotificationSender` implements the `NotificationSender` interface.
    - It uses the `NotificationCommandFactory` to get the correct command instances.
    - It calls the `execute()` method on the command list it receives from the factory.

### The Command

The **Command** object encapsulates all the information required to act.

- **Implementation:** `NotificationCommand` (abstract base), with concrete classes like `SystemNotificationCommand` and
  `InAppNotificationCommand`.
- **Responsibilities:**
    - Binds together a `Notification` (the payload) and a `NotificationNotifier` (the Receiver).
    - Provides a common `execute()` interface that the Invoker can call without knowing the specific details of the
      command.

### The Receiver

The **Receiver** knows how to perform the work required to carry out the request. It's where the business logic lives.

- **Implementation:** `NotificationNotifier` (interface), with concrete classes like `SystemNotificationNotifier` and
  `InAppNotificationNotifier`.
- **Responsibilities:**
    - Contains the platform-specific implementation for displaying a notification.
    - `SystemNotificationNotifier` uses the Android `NotificationManager`.
    - `InAppNotificationNotifier` uses the `InAppNotificationEventBus` to tell the app that a notification is available
      to be displayed. The `InAppNotificationScaffold` listens for this event and shows the notification.

## The Notification Data Model

The notification data model is represented by the `Notification` data model, which acts as the central payload for all
operations. Following, we will breakdown the notification model for better clarity.

### Breakdown - The Notification model

The notification model is composed by the following components:

* The `Notification` interface, which defines the common properties that all notifications must have. This is a sealed
  interface and can't be implemented outside it's package/module.
* The `SystemNotification` is a subtype of `Notification` that represents a notification displayed by the system, adding
  it's own set of properties which is described in the SystemNotification model section.
* The `InAppNotification` is a subtype of `Notification` that represents a notification displayed within the
  application, adding it's own set of properties which is described in the SystemNotification model section.
* The `AppNotification` is an abstract class that provides default properties implementation to easy the app
  notification. **This is the class you should extend** whenever creating a new Notification type.

The below diagram describes these components more detailed:

```mermaid
classDiagram
    class Notification {
        <<interface>>
        + accountUuid: String
        + title: String
        + accessibilityText: String
        + contentText: String?
        + severity: NotificationSeverity
        + createdAt: LocalDateTime
        + actions: Set~NotificationAction~
        + icon: NotificationIcon
    }
    class AppNotification {
        <<abstract>>
        + accessibilityText: String
        + createdAt: LocalDateTime
        + actions: Set~NotificationAction~
    }
    class SystemNotification {
        <<interface>>
    }
    class InAppNotification {
        <<interface>>
    }
    class NotificationSeverity {
        <<enumeration>>
        Fatal
        Critical
        Temporary
        Warning
        Information
    }
    class NotificationAction {
        <<data>>
        + icon: NotificationIcon?
        + title: String
    }
    class NotificationIcon {
        <<data>>
        + systemNotificationIcon: SystemNotificationIcon?
        + inAppNotificationIcon: ImageVector?
    }

    Notification --> NotificationSeverity
    Notification --> NotificationAction
    Notification --> NotificationIcon
    NotificationAction --> NotificationIcon
    Notification <|-- AppNotification
    Notification <|-- SystemNotification
    Notification <|-- InAppNotification
```

The properties of the `Notification` interface are:

* `accountUuid`: The UUID of the account that owns the notification. This can be used to filter notifications when
  deciding to display them.
* `title`: The title of the notification.
* `accessibilityText`: The text to be used for accessibility purposes.
* `contentText`: The main content text of the notification, can be null.
* `severity`: The severity level of the notification.
* `createdAt`: The date and time when the notification was created.
* `actions`: A set of actions that can be performed on the notification.
* `icon`: The notification icon.

### Breakdown - The System Notification model

System notifications have their own particularities, which are described in this section. Aside from all the properties
included in the `Notification` interface, the `SystemNotification` also includes:

* `subText`: An optional secondary text that appears below the title, can be null.
* `channel`: The notification channel which the notification belongs to.
* `systemNotificationStyle`: The style of the notification which will explain to Android OS how to display the
  notification. Defaults to `SystemNotificationStyle.Undefined`. For more information about the styles, see
  the [notification styles' documentation](notification-styles.md)
* `asLockscreenNotification()`: A method to convert the `SystemNotification` to a `LockscreenNotification`. You should
  only override this if you need to display a different notification when displaying in the lockscreen, e.g. hiding the
  sender's mail, notification content, etc.

```mermaid
classDiagram
    class Notification {
        <<interface>>
    }
    class SystemNotification {
        <<interface>>
        + subText: String?
        + channel: NotificationChannel
        + systemNotificationStyle: SystemNotificationStyle
        + asLockscreenNotification(): LockscreenNotification?
    }
    class LockscreenNotification {
        <<data>>
        + notification: SystemNotification
        + lockscreenNotificationAppearance: LockscreenNotificationAppearance
    }
    class LockscreenNotificationAppearance {
        <<enumeration>>
        None
        AppName
        Public
        MessageCount
    }
    class NotificationChannel {
        <<interface>>
        + id: String
        + name: StringResource
        + description: StringResource
        + importance: NotificationChannelImportance
    }
    class NotificationChannelImportance {
        <<enumeration>>
        None
        Min
        Low
        Default
        High
    }
    class SystemNotificationStyle {
        <<enumeration>>
        BigTextStyle
        InboxStyle
        Undefined
    }
    class `SystemNotificationStyle.BigTextStyle` {
        <<data>>
        + text: String
    }
    class `SystemNotificationStyle.InboxStyle` {
        <<data>>
        + bigContentTitle: String
        + summary: String
        + lines: List~CharSequence~
    }

    Notification <|-- SystemNotification
    SystemNotification --> SystemNotificationStyle
    SystemNotification --> NotificationChannel
    SystemNotification <--> LockscreenNotification
    LockscreenNotification --> LockscreenNotificationAppearance
    SystemNotificationStyle <|-- `SystemNotificationStyle.BigTextStyle`
    SystemNotificationStyle <|-- `SystemNotificationStyle.InboxStyle`
    NotificationChannel --> NotificationChannelImportance
```

### Breakdown - The In-App Notification model

In-app notifications have their own particularities, which are described in this section. Aside from all the properties
included in the `Notification` interface, the `InAppNotification` also includes:

* `inAppNotificationStyle`: The style of the in-app notification. This will explain to the UI how to display the
  notification. For more information about the styles, see the [notification styles' documentation](notification-styles.md).

```mermaid
classDiagram
    class Notification {
        <<interface>>
    }
    class InAppNotification {
        <<interface>>
        + inAppNotificationStyle: InAppNotificationStyle
    }
    class InAppNotificationStyle {
        <<enumeration>>
        BannerGlobalNotification
        BannerInlineNotification
        DialogNotification
        SnackbarNotification
        Undefined
    }
    class `InAppNotificationStyle.SnackbarNotification` {
        <<data>>
        + duration: SnackbarDuration
    }

    Notification <|-- InAppNotification
    InAppNotification --> InAppNotificationStyle
    InAppNotificationStyle <|-- `InAppNotificationStyle.SnackbarNotification`
```

## Summary and Diagram

* **Core `Notification` Interface**: At the top level is the `Notification` interface, which contains properties common
  to all notification types, such as `title`, `text`, `severity`, and a list of `NotificationAction`s. The
  `Notification` interface should never be directly implemented.
* **Abstract `AppNotification` Class**: This is an abstract class that provides default properties implementation to
  easy the app notification creation.
* **Specialized Notification Types**: To handle platform differences, the base interface is extended by two specialized
  interfaces:
    * **`SystemNotification`**: Represents a standard Android OS notification. It includes properties for
      Android-specific features like the `NotificationChannel` and `NotificationChannelImportance`. Additionally it also
      let you change how to display the notification via `SystemNotificationStyle`.
    * **`InAppNotification`**: Represents a message shown inside the app's UI. It includes its own
      `InAppNotificationStyle`.
* **Flexible Styling and Actions**: A key feature of the model is its use of polymorphism for styling. This allows the
  UI to be defined by data, not hard-coded logic.
    * `SystemNotificationStyle` can be a `BigTextStyle` or `InboxStyle`, mapping to native Android features.
    * `InAppNotificationStyle` can be `BannerGlobalNotification`, `BannerInlineNotification`, `DialogNotification`, and
      `SnackbarNotification`.

The whole notification model is represented by the following diagram:
```mermaid
classDiagram
    class Notification {
        <<interface>>
        + accountUuid: String
        + title: String
        + accessibilityText: String
        + contentText: String?
        + severity: NotificationSeverity
        + createdAt: LocalDateTime
        + actions: Set~NotificationAction~
        + icon: NotificationIcon
    }
    class AppNotification {
        <<abstract>>
        + accessibilityText: String
        + createdAt: LocalDateTime
        + actions: Set~NotificationAction~
    }
    class NotificationSeverity {
        <<enumeration>>
        Fatal
        Critical
        Temporary
        Warning
        Information
    }
    class NotificationAction {
        <<data>>
        + icon: NotificationIcon?
        + title: String
    }
    class NotificationIcon {
        <<data>>
        + systemNotificationIcon: SystemNotificationIcon?
        + inAppNotificationIcon: ImageVector?
    }
    class SystemNotification {
        <<interface>>
        + subText: String?
        + channel: NotificationChannel
        + systemNotificationStyle: SystemNotificationStyle
        + asLockscreenNotification(): LockscreenNotification?
    }
    class LockscreenNotification {
        <<data>>
        + notification: SystemNotification
        + lockscreenNotificationAppearance: LockscreenNotificationAppearance
    }
    class LockscreenNotificationAppearance {
        <<enumeration>>
        None
        AppName
        Public
        MessageCount
    }
    class InAppNotification {
        <<interface>>
        + inAppNotificationStyle: InAppNotificationStyle
    }
    class NotificationChannel {
        <<interface>>
        + id: String
        + name: StringResource
        + description: StringResource
        + importance: NotificationChannelImportance
    }
    class NotificationChannelImportance {
        <<enumeration>>
        None
        Min
        Low
        Default
        High
    }
    class SystemNotificationStyle {
        <<enumeration>>
        BigTextStyle
        InboxStyle
        Undefined
    }
    class `SystemNotificationStyle.BigTextStyle` {
        <<data>>
        + text: String
    }
    class `SystemNotificationStyle.InboxStyle` {
        <<data>>
        + bigContentTitle: String
        + summary: String
        + lines: List~CharSequence~
    }
    class InAppNotificationStyle {
        <<enumeration>>
        BannerGlobalNotification
        BannerInlineNotification
        DialogNotification
        SnackbarNotification
        Undefined
    }
    class `InAppNotificationStyle.SnackbarNotification` {
        <<data>>
        + duration: SnackbarDuration
    }

    Notification --> NotificationSeverity
    Notification --> NotificationAction
    Notification --> NotificationIcon
    NotificationAction --> NotificationIcon
    Notification <|-- AppNotification
    Notification <|-- SystemNotification
    Notification <|-- InAppNotification
    SystemNotification --> SystemNotificationStyle
    SystemNotification --> NotificationChannel
    SystemNotification <--> LockscreenNotification
    LockscreenNotification --> LockscreenNotificationAppearance
    SystemNotificationStyle <|-- `SystemNotificationStyle.BigTextStyle`
    SystemNotificationStyle <|-- `SystemNotificationStyle.InboxStyle`
    NotificationChannel --> NotificationChannelImportance
    InAppNotification --> InAppNotificationStyle
    InAppNotificationStyle <|-- `InAppNotificationStyle.SnackbarNotification`
```

