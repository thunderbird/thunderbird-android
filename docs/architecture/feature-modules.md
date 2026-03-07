# 📦 Feature Modules and Extensions

The Thunderbird for Android project is organized into multiple feature modules, each encapsulating a specific
functionality of the application. This document provides an overview of the main feature modules, how they are
split into subfeatures, and how the application can be extended with additional features.

## 📏 Feature Module Best Practices

When developing new feature modules or extending existing ones, follow these best practices:

1. **API-First Design**: Define clear public interfaces before implementation
2. **Single Responsibility**: Each feature module should have a single, well-defined responsibility
3. **Minimal Dependencies**: Minimize dependencies between feature modules
4. **Proper Layering**: Follow Clean Architecture principles within each feature
5. **Testability**: Design features to be easily testable in isolation
6. **Documentation**: Document the purpose and usage of each feature module
7. **Consistent Naming**: Follow the established naming conventions
8. **Feature Flags**: Use feature flags for gradual rollout and A/B testing
9. **Accessibility**: Ensure all features are accessible to all users
10. **Internationalization**: Design features with internationalization in mind

By following these guidelines, the Thunderbird for Android application can maintain a clean, modular architecture while
expanding its functionality to meet user needs.

## 📋 Feature Module Overview

The application is composed of several core feature modules, each responsible for a specific aspect of the
application's functionality:

```mermaid
graph TB
    subgraph FEATURE[App Features]
        direction TB
        
        
        subgraph ROW_2[" "]
            direction LR
            SETTINGS["`**Settings**<br>App configuration`"]
            NOTIFICATION["`**Notification**<br>Push and alert handling`"]
            SEARCH["`**Search**<br>Content discovery`"]
            WIDGET["`**Widget**<br>Home screen components`"]
        end
        
        subgraph ROW_1[" "]
            direction LR
            ACCOUNT["`**Account**<br>User accounts management`"]
            MAIL["`**Mail**<br>Email handling and display`"]
            DRAWER["`**Navigation**<br>App navigation UI components`"]
            ONBOARDING["`**Onboarding**<br>User setup and introduction`"]
        end
    end

    classDef row fill: #d9ffd9, stroke: #d9ffd9, color: #d9ffd9
    classDef feature fill: #d9ffd9,stroke: #000000, color: #000000
    classDef feature_module fill: #33cc33, stroke: #000000, color:#000000
    
    class ROW_1,ROW_2 row
    class FEATURE feature
    class ACCOUNT,MAIL,NAVIGATION,ONBOARDING,SETTINGS,NOTIFICATION,SEARCH,WIDGET feature_module
```

## 🧩 Feature Module Details

### 🔑 Account Module

The Account module manages all aspects of email accounts, including setup, configuration, and authentication.

```shell
feature:account
├── feature:account:api
├── feature:account:internal
├── feature:account:setup
│   ├── feature:account:setup:api
│   └── feature:account:setup:internal
├── feature:account:settings
│   ├── feature:account:settings:api
│   └── feature:account:settings:internal
├── feature:account:server
│   ├── feature:account:server:api
│   ├── feature:account:server:internal
│   ├── feature:account:server:certificate
│   │   ├── feature:account:server:certificate:api
│   │   └── feature:account:server:certificate:internal
│   ├── feature:account:server:settings
│   │   ├── feature:account:server:settings:api
│   │   └── feature:account:server:settings:internal
│   └── feature:account:server:validation
│       ├── feature:account:server:validation:api
│       └── feature:account:server:validation:internal
├── feature:account:auth
│   ├── feature:account:auth:api
│   ├── feature:account:auth:internal
│   └── feature:account:auth:oauth
│       ├── feature:account:auth:oauth:api
│       └── feature:account:auth:oauth:internal
└── feature:account:storage
    ├── feature:account:storage:api
    ├── feature:account:storage:internal
    └── feature:account:storage:legacy
        ├── feature:account:storage:legacy:api
        └── feature:account:storage:legacy:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementation details for account management
- **Setup**: New account setup wizard functionality
  - **API**: Public interfaces for account setup
  - **Internal**: Concrete implementations of setup flows
- **Settings**: Account-specific settings management
  - **API**: Public interfaces for account settings
  - **Internal**: Concrete implementations of settings functionality
- **Server**: Server configuration and management
  - **API/Internal**: Core server management interfaces and internal implementations
  - **Certificate**: SSL certificate handling
  - **Settings**: Server settings configuration
  - **Validation**: Server connection validation
- **Auth**: Authentication functionality
  - **API/Internal**: Core authentication interfaces and internal implementations
  - **OAuth**: OAuth-specific authentication implementation
- **Storage**: Account data persistence
  - **API/Internal**: Core storage interfaces and internal implementations
  - **Legacy**: Legacy storage implementation

### 📧 Mail Module

The Mail module handles core email functionality, including message display, composition, and folder management.

```shell
feature:mail
├── feature:mail:api
├── feature:mail:internal
├── feature:mail:account
│   ├── feature:mail:account:api
│   └── feature:mail:account:internal
├── feature:mail:folder
│   ├── feature:mail:folder:api
│   └── feature:mail:folder:internal
├── feature:mail:compose
│   ├── feature:mail:compose:api
│   └── feature:mail:compose:internal
└── feature:mail:message
    ├── feature:mail:message:api
    ├── feature:mail:message:internal
    ├── feature:mail:message:view
    │   ├── feature:mail:message:view:api
    │   └── feature:mail:message:view:internal
    └── feature:mail:message:list
        ├── feature:mail:message:list:api
        └── feature:mail:message:list:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for mail functionality
- **Account**: Mail-specific account interfaces and internal implementations
  - **API**: Public interfaces for mail account integration
  - **Internal**: Concrete implementations of mail account functionality
- **Folder**: Email folder management
  - **API**: Public interfaces for folder operations
  - **Implementation**: Concrete implementations of folder management
- **Compose**: Email composition functionality
  - **API**: Public interfaces for message composition
  - **Internal**: Concrete implementations of composition features
- **Message**: Message handling and display
  - **API/Internal**: Core message handling interfaces and internal implementations
  - **View**: Individual message viewing functionality
  - **List**: Message list display and interaction

### 🧭 Navigation Module

The Navigation module is part of the core UI and provides infrastructure for navigating through the application.

```shell
core:ui:navigation
```

#### Details:

- **Navigation**: Core navigation interfaces and routing
- **Route**: Type-safe route definitions
- **NavigationExtension**: Compose-specific navigation extensions

### 🗄️ Navigation Drawer Module

The Navigation Drawer module provides UI components for the main application navigation drawer, including dropdown
and other variations if any.

```shell
feature:navigation:drawer
├── feature:navigation:drawer:api
├── feature:navigation:drawer:internal
└── feature:navigation:drawer:dropdown
    ├── feature:navigation:drawer:dropdown:api
    └── feature:navigation:drawer:dropdown:internal
```

#### Subfeatures:

- **API/Internal**: Core drawer interfaces and internal implementations
- **Dropdown**: Dropdown-style navigation implementation

### 🚀 Onboarding Module

The Onboarding module guides new users through the initial setup process.

```shell
feature:onboarding
├── feature:onboarding:api
├── feature:onboarding:internal
├── feature:onboarding:main
│   ├── feature:onboarding:main:api
│   └── feature:onboarding:main:internal
├── feature:onboarding:welcome
│   ├── feature:onboarding:welcome:api
│   └── feature:onboarding:welcome:internal
├── feature:onboarding:permissions
│   ├── feature:onboarding:permissions:api
│   └── feature:onboarding:permissions:internal
└── feature:onboarding:migration
    ├── feature:onboarding:migration:api
    ├── feature:onboarding:migration:internal
    ├── feature:onboarding:migration:thunderbird
    │   ├── feature:onboarding:migration:thunderbird:api
    │   └── feature:onboarding:migration:thunderbird:internal
    └── feature:onboarding:migration:noop
        ├── feature:onboarding:migration:noop:api
        └── feature:onboarding:migration:noop:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for onboarding
- **Main**: Main onboarding flow
  - **API**: Public interfaces for the main onboarding process
  - **Internal**: Concrete implementations of the onboarding flow
- **Welcome**: Welcome screens and initial user experience
  - **API**: Public interfaces for welcome screens
  - **Internal**: Concrete implementations of welcome screens
- **Permissions**: Permission request handling
  - **API**: Public interfaces for permission management
  - **Internal**: Concrete implementations of permission requests
- **Migration**: Data migration from other apps
  - **API/Internal**: Core migration interfaces and internal implementations
  - **Thunderbird**: Thunderbird-specific migration implementation
  - **Noop**: No-operation implementation for testing

### ⚙️ Settings Module

The Settings module provides interfaces for configuring application behavior.

```shell
feature:settings
├── feature:settings:api
├── feature:settings:internal
├── feature:settings:import
│   ├── feature:settings:import:api
│   └── feature:settings:import:internal
└── feature:settings:ui
    ├── feature:settings:ui:api
    └── feature:settings:ui:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for settings
- **Import**: Settings import functionality
  - **API**: Public interfaces for settings import
  - **Internal**: Concrete implementations of import functionality
- **UI**: Settings user interface components
  - **API**: Public interfaces for settings UI
  - **Internal**: Concrete implementations of settings screens

### 🔔 Notification Module

The Notification module handles push notifications and alerts for new emails and events.

```shell
feature:notification
├── feature:notification:api
├── feature:notification:internal
├── feature:notification:email
│   ├── feature:notification:email:api
│   └── feature:notification:email:internal
└── feature:notification:push
    ├── feature:notification:push:api
    └── feature:notification:push:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for notifications
- **Email**: Email-specific notification handling
  - **API**: Public interfaces for email notifications
  - **Internal**: Concrete implementations of email alerts
- **Push**: Push notification handling
  - **API**: Public interfaces for push notifications
  - **Internal**: Concrete implementations of push notification processing

### 🔍 Search Module

The Search module provides functionality for searching through emails and contacts.

```shell
feature:search
├── feature:search:api
├── feature:search:internal
├── feature:search:email
│   ├── feature:search:email:api
│   └── feature:search:email:internal
├── feature:search:contact
│   ├── feature:search:contact:api
│   └── feature:search:contact:internal
└── feature:search:ui
    ├── feature:search:ui:api
    └── feature:search:ui:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for search functionality
- **Email**: Email-specific search capabilities
  - **API**: Public interfaces for email search
  - **Internal**: Concrete implementations of email search
- **Contact**: Contact search functionality
  - **API**: Public interfaces for contact search
  - **Internal**: Concrete implementations of contact search
- **UI**: Search user interface components
  - **API**: Public interfaces for search UI
  - **Internal**: Concrete implementations of search screens

### 📱 Widget Module

The Widget module provides home screen widgets for quick access to email functionality.

```shell
feature:widget
├── feature:widget:api
├── feature:widget:internal
├── feature:widget:message-list
│   ├── feature:widget:message-list:api
│   └── feature:widget:message-list:internal
├── feature:widget:message-list-glance
│   ├── feature:widget:message-list-glance:api
│   └── feature:widget:message-list-glance:internal
├── feature:widget:shortcut
│   ├── feature:widget:shortcut:api
│   └── feature:widget:shortcut:internal
└── feature:widget:unread
    ├── feature:widget:unread:api
    └── feature:widget:unread:internal
```

#### Subfeatures:

- **API/Internal**: Core public interfaces and internal implementations for widgets
- **Message List**: Email list widget
  - **API**: Public interfaces for message list widget
  - **Internal**: Concrete implementations of message list widget
- **Message List Glance**: Glanceable message widget
  - **API**: Public interfaces for glance widget
  - **Internal**: Concrete implementations of glance widget
- **Shortcut**: App shortcut widgets
  - **API**: Public interfaces for shortcut widgets
  - **Internal**: Concrete implementations of shortcut widgets
- **Unread**: Unread message counter widget
  - **API**: Public interfaces for unread counter widget
  - **Internal**: Concrete implementations of unread counter widget

## 🔄 Supporting Feature Modules

In addition to the core email functionality, the application includes several supporting feature modules:

### 🔎 Autodiscovery Module

The Autodiscovery module automatically detects email server settings.

#### Subfeatures:

- **API** (`feature:autodiscovery:api`): Public interfaces
- **Autoconfig** (`feature:autodiscovery:autoconfig`): Automatic configuration
- **Service** (`feature:autodiscovery:service`): Service implementation
- **Demo** (`feature:autodiscovery:demo`): Demonstration implementation

### 💰 Funding Module

The Funding module handles in-app financial contributions and funding options.

#### Subfeatures:

- **API** (`feature:funding:api`): Public interfaces
- **Google Play** (`feature:funding:googleplay`): Google Play billing integration
- **Link** (`feature:funding:link`): External funding link handling
- **Noop** (`feature:funding:noop`): No-operation implementation

### 🔄 Migration Module

The Migration module handles data migration between different email clients.

#### Subfeatures:

- **Provider** (`feature:migration:provider`): Migration data providers
- **QR Code** (`feature:migration:qrcode`): QR code-based migration
- **Launcher** (`feature:migration:launcher`): Migration launcher
  - **API** (`feature:migration:launcher:api`): Launcher interfaces
  - **Noop** (`feature:migration:launcher:noop`): No-operation implementation
  - **Thunderbird** (`feature:migration:launcher:thunderbird`): Thunderbird-specific implementation

### 📊 Telemetry Module

The Telemetry module handles usage analytics and reporting.

#### Subfeatures:

- **API** (`feature:telemetry:api`): Public interfaces
- **Noop** (`feature:telemetry:noop`): No-operation implementation
- **Glean** (`feature:telemetry:glean`): Mozilla Glean integration

## 🔌 Extending with Additional Features

The modular architecture of Thunderbird for Android allows for easy extension with additional features. To give you an
idea how the app could be extended when building a new feature, here are some theoretical examples along with their
structure:

### 📅 Calendar Feature

A Calendar feature could be added to integrate calendar functionality with email.

```shell
feature:calendar
├── feature:calendar:api
├── feature:calendar:internal
├── feature:calendar:event
│   ├── feature:calendar:event:api
│   └── feature:calendar:event:internal
└── feature:calendar:sync
    ├── feature:calendar:sync:api
    └── feature:calendar:sync:internal
```

### 🗓️ Appointments Feature

An Appointments feature could manage meetings and appointments.

```shell
feature:appointment
├── feature:appointment:api
├── feature:appointment:internal
├── feature:appointment:scheduler
│   ├── feature:appointment:scheduler:api
│   └── feature:appointment:scheduler:internal
└── feature:appointment:notification
    ├── feature:appointment:notification:api
    └── feature:appointment:notification:internal
```

## 🔗 Feature Relationships

Features in the application interact with each other through well-defined APIs. The diagram below illustrates the
relationships between different features:

```mermaid
graph TB
    subgraph CORE[Core Features]
        ACCOUNT[Account]
        MAIL[Mail]
    end

    subgraph EXTENSIONS[Potential Extensions]
        CALENDAR[Calendar]
        APPOINTMENT[Appointments]
    end

    MAIL --> |uses| ACCOUNT

    CALENDAR --> |integrates with| MAIL
    CALENDAR --> |uses| ACCOUNT
    APPOINTMENT --> |uses| ACCOUNT
    APPOINTMENT --> |integrates with| CALENDAR
    APPOINTMENT --> |uses| MAIL

    linkStyle default stroke:#999,stroke-width:2px

    classDef core fill:#e8c8ff,stroke:#000000,color:#000000
    classDef core_module fill:#c090ff,stroke:#000000,color:#000000
    classDef extension fill:#d0e0ff,stroke:#000000,color:#000000
    classDef extension_module fill:#8090ff,stroke:#000000,color:#000000
    class CORE core
    class ACCOUNT,MAIL,NAVIGATION,SETTINGS core_module
    class EXTENSIONS extension
    class CALENDAR,TODO,SYNC,NOTES,APPOINTMENT extension_module
```

