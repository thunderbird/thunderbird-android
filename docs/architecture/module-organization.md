# 📦 Module Organization

The Thunderbird for Android project is following a modularization approach, where the codebase is divided into multiple
distinct modules. These modules encapsulate specific functionality and can be developed, tested, and maintained
independently. This modular architecture promotes reusability, scalability, and maintainability of the codebase.

This document outlines the adopted module organization for the Thunderbird for Android project, serving as a guide for
developers to understand the codebase structure and ensure consistent architectural patterns.

## 📂 Module Overview

The modules are organized into several types, each serving a specific purpose in the overall architecture:

```mermaid
graph TB
    subgraph APP[App Modules]
        direction TB
        APP_TB["`**app-thunderbird**`"]
        APP_K9["`**app-k9mail**`"]
    end

    subgraph COMMON[App Common Module]
        direction TB
        APP_COMMON["`**app-common**`"]
    end

    subgraph FEATURE[Feature Modules]
        direction TB
        FEATURE_ACCOUNT["`**feature:account**`"]
        FEATURE_SETTINGS["`**feature:settings**`"]
        FEATURE_ONBOARDING["`**feature:onboarding**`"]
        FEATURE_MAIL["`**feature:mail**`"]
    end

    subgraph CORE[Core Modules]
        direction TB
        CORE_UI["`**core:ui**`"]
        CORE_COMMON["`**core:common**`"]
        CORE_ANDROID["`**core:android**`"]
        CORE_NETWORK["`**core:network**`"]
        CORE_DATABASE["`**core:database**`"]
        CORE_TESTING["`**core:testing**`"]
    end

    subgraph LIBRARY[Library Modules]
        direction TB
        LIB_AUTH["`**library:auth**`"]
        LIB_CRYPTO["`**library:crypto**`"]
        LIB_STORAGE["`**library:storage**`"]
    end

    subgraph LEGACY[Legacy Modules]
        direction TB
        LEGACY_K9["`**legacy**`"]
        LEGACY_MAIL["`**mail**`"]
        LEGACY_BACKEND["`**backend**`"]
    end

    APP ~~~ COMMON
    COMMON ~~~ FEATURE
    FEATURE ~~~ CORE
    CORE ~~~ LIBRARY
    LIBRARY ~~~ LEGACY

    APP --> |depends on| COMMON
    COMMON --> |depends on| FEATURE
    FEATURE --> |depends on| CORE
    CORE --> |depends on| LIBRARY
    COMMON --> |depends on<br>as legacy bridge| LEGACY

    classDef app fill: #d0e0ff, stroke: #0066cc
    classDef common fill: #d5f5d5, stroke: #00aa00
    classDef feature fill: #ffe0d0, stroke: #cc6600
    classDef core fill: #f0d0ff, stroke: #cc00cc
    classDef library fill: #fff0d0, stroke: #cc9900
    classDef legacy fill: #f0f0f0, stroke: #999999

    class APP_TB,APP_K9 app
    class APP_COMMON common
    class FEATURE_ACCOUNT,FEATURE_SETTINGS,FEATURE_ONBOARDING,FEATURE_MAIL feature
    class CORE_UI,CORE_COMMON,CORE_ANDROID,CORE_DATABASE,CORE_NETWORK,CORE_TESTING core
    class LIB_AUTH,LIB_CRYPTO,LIB_STORAGE library
    class LEGACY_MAIL,LEGACY_BACKEND,LEGACY_K9 legacy
```

### Module Types

#### 📱 App Modules

The App Modules (`app-thunderbird` and `app-k9mail`) contain the application-specific code, including:
- Application entry points and initialization logic
- Final dependency injection setup
- Navigation configuration
- Integration with feature modules solely for that application
- App-specific themes and resources (strings, themes, etc.)

#### 🔄 App Common Module

The `app-common` module acts as the central hub for shared code between both applications. This module serves as the
primary "glue" that binds various `feature` modules together, providing a seamless integration point. It also contains:
- Shared application logic
- Feature coordination
- Common dependency injection setup
- Legacy code bridges and adapters

##### What Should Go in App Common

The app-common module should contain:

1. **Shared Application Logic**: Code that's needed by both app modules but isn't specific to any one feature.
   - Example: `BaseApplication` provides common application initialization, language management, and theme setup.
   - This avoids duplication between app-thunderbird and app-k9mail.
2. **Feature Integration Code**: Code that connects different features together.
   - Example: Code that coordinates between account and mail features.
   - This maintains separation between features while allowing them to work together.
3. **Common Dependency Injection Setup**: Koin modules that configure dependencies shared by both applications.
   - Example: `AppCommonModule` includes legacy modules and app-common specific modules.
   - This ensures consistent dependency configuration across both applications.
4. **Legacy Code Bridges/Adapters**: Implementations of interfaces defined in feature modules that delegate to legacy code.
   - Example: `DefaultAccountProfileLocalDataSource` implements `AccountProfileLocalDataSource` from `feature:account:core` and delegates to legacy account code.
   - These bridges isolate legacy code and prevent direct dependencies on it from feature modules.

##### What Should NOT Go in App Common

The following should NOT be placed in app-common:

1. **Feature-Specific Business Logic**: Business logic that belongs to a specific feature domain should be in that feature's module.
   - Example: Mail composition logic should be in `feature:mail`, not in app-common.
   - This maintains clear separation of concerns and feature independence.
2. **UI Components**: UI components should be in core:ui or in feature modules.
   - Example: A custom button component should be in core:ui, while a mail-specific UI component should be in feature:mail.
   - This ensures proper layering and reusability.
3. **Direct Legacy Code**: Legacy code should remain in legacy modules, with app-common providing bridges.
   - Example: Don't move legacy mail code into app-common; instead, create a bridge in app-common.
   - This maintains the separation between legacy and modern code.
4. **New Feature Implementations**: New features should be implemented in feature modules, not in app-common.
   - Example: A new calendar feature should be in `feature:calendar`, not in app-common.
   - This ensures features can evolve independently.

##### Decision Criteria for New Contributors

When deciding whether code belongs in app-common or a feature module, consider:

1. **Is it shared between both applications?** If yes, it might belong in app-common.
2. **Is it specific to a single feature domain?** If yes, it belongs in that feature module.
3. **Does it bridge to legacy code?** If yes, it belongs in app-common.
4. **Does it coordinate between multiple features?** If yes, it might belong in app-common.
5. **Is it a new feature implementation?** If yes, create a new feature module instead.

Remember that app-common should primarily contain integration code, shared application logic, and bridges to legacy code. Feature-specific logic should be in feature modules, even if used by both applications.

#### ✨ Feature Modules

The `feature:*` modules are independent and encapsulate distinct user-facing feature domains. They are designed to be
reusable and can be integrated into any application module as needed.

Feature implementation modules (e.g., `:feature:account:impl`) should ideally not depend directly on other feature
implementation modules. Instead, they should depend on the public `:api` module of other features (e.g.,
`:feature:someOtherFeature:api`) to access their functionality through defined contracts, see
[module structure](module-structure.md#-api-module) for more details.

When features are complex, they can be split into smaller sub feature modules, addressing specific aspects or
functionality within a feature domain:

- `:feature:account:api`: Public interfaces for account management
- `:feature:account:settings:api`: Public interfaces for account settings
- `:feature:account:settings:impl`: Concrete implementations of account settings

#### 🧰 Core Modules

The `core:*` modules contain foundational functionality used across the application:

- **core:ui**: UI components, themes, and utilities
- **core:common**: Common utilities and extensions
- **core:network**: Networking utilities and API client infrastructure
- **core:database**: Database infrastructure and utilities
- **core:testing**: Testing utilities

Core modules should only contain generic, reusable components that have no specific business logic.
Business objects (e.g., account, mail, etc.) should live in their respective feature modules.

#### 📚 Library Modules

The `library:*` modules are for specific implementations that might be used across various features or applications.
They could be third-party integrations or complex utilities and eventually shared across multiple projects.

#### 🔙 Legacy Modules

The `legacy:*` modules that are still required for the project to function, but don't follow the new project structure.
These modules should not be used for new development. The goal is to migrate the functionality of these modules to the
new structure over time.

Similarly the `mail:*` and `backend:*` modules are legacy modules that contain the old mail and backend implementations.
These modules are being gradually replaced by the new feature modules.

The `legacy` modules are integrated into the `app-common` module, allowing them to be used by other parts of the app.
The glue code for bridging legacy code to the new modular architecture is also located in the `app-common` module. See
[module legacy integration](legacy-module-integration.md) for more details.

## 🔗 Module Dependencies

The module dependency diagram below illustrates how different modules interact with each other in the project,
showing the dependencies and integration points between modules:

- **App Modules**: Depend on the App Common module for shared functionality and selectively integrate feature modules
- **App Common**: Integrates various feature modules to provide a cohesive application
- **Feature Modules**: Use core modules and libraries for their implementation, may depend on other feature api modules
- **App-Specific Features**: Some features are integrated directly by specific apps (K-9 Mail or Thunderbird)

Rules for module dependencies:
- **One-Way Dependencies**: Modules should not depend on each other in a circular manner
- **API-Implementation Separation**: Modules should depend on `api` modules, not `implementation` modules, see [module structure](module-structure.md#module-structure)
- **Feature Integration**: Features should be integrated through the `app-common` module, which acts as the central integration hub
- **Dependency Direction**: Dependencies should flow from app modules to common, then to features, and finally to core and libraries

```mermaid
graph TB
    subgraph APP[App]
        direction TB
        APP_K9["`**:app-k9mail**<br>K-9 Mail`"]
        APP_TB["`**:app-thunderbird**<br>Thunderbird for Android`"]
    end

    subgraph COMMON[App Common]
        direction TB
        APP_COMMON["`**:app-common**<br>Integration Code`"]
    end

    subgraph FEATURE[Feature]
        direction TB
        FEATURE_ACCOUNT_API[feature:account:api]
        FEATURE_ACCOUNT_IMPL[feature:account:impl]
        FEATURE_SETTINGS_API[feature:settings:api]
        FEATURE_SETTINGS_API[feature:settings:api]
        FEATURE_K9[feature:k9OnlyFeature:impl]
        FEATURE_TB[feature:tfaOnlyFeature:impl]
    end

    subgraph CORE[Core]
        direction TB
        CORE_UI_API[core:ui:api]
        CORE_COMMON_API[core:common:api]
    end

    subgraph LIBRARY[Library]
        direction TB
        LIB_AUTH[library:auth]
        LIB_STORAGE[library:storage]
    end

    APP_K9 --> |depends on| APP_COMMON
    APP_TB --> |depends on| APP_COMMON
    APP_COMMON --> |uses| FEATURE_ACCOUNT_API
    APP_COMMON --> |injects/uses impl of| FEATURE_ACCOUNT_IMPL
    FEATURE_ACCOUNT_IMPL --> FEATURE_ACCOUNT_API
    APP_COMMON --> |uses| FEATURE_SETTINGS_API
    APP_K9 --> |injects/uses impl of| FEATURE_K9
    APP_TB --> |injects/uses impl of| FEATURE_TB
    FEATURE_ACCOUNT_API --> |uses| CORE_UI_API
    FEATURE_SETTINGS_API --> |uses| CORE_COMMON_API
    FEATURE_TB --> |uses| LIB_AUTH
    FEATURE_K9 --> |uses| LIB_STORAGE
    CORE_COMMON_API --> |uses| LIB_STORAGE

    classDef module fill:yellow
    classDef app fill:azure
    classDef app_common fill:#ddd
    classDef featureK9 fill:#ffcccc,stroke:#cc0000
    classDef featureTB fill:#ccccff,stroke:#0000cc
    class APP_K9 app
    class APP_TB app
    class APP_COMMON app_common
    class FEATURE_K9 featureK9
    class FEATURE_TB featureTB
```

