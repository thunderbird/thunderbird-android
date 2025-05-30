# 🔙 Legacy Integration

The legacy module integration diagram below explains how legacy code is integrated into the new modular architecture and
outlines the strategy for migrating legacy functionality:

- **Integration Approach**: Legacy modules are integrated through the App Common module, which provides adapters and
  bridges
- **Migration Strategy**: Legacy code is gradually migrated to new feature and core modules
- **Transitional State**: During migration, both legacy and new modules coexist, with clear integration points
- **Dependency Direction**: New modules should not depend on legacy modules; the dependency flow is one-way from legacy
  to new

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
        FEATURE1[Feature 1]
        FEATURE2[Feature 2]
        FEATURE3[Feature from Legacy]
    end

    subgraph CORE[Core]
        direction TB
        CORE1[Core 1]
        CORE2[Core 2]
        CORE3[Core from Legacy]
    end

    subgraph LIBRARY[Library]
        direction TB
        LIB1[Library 1]
        LIB2[Library 2]
    end

    subgraph LEGACY[Legacy]
        direction TB
        LEG[Legacy Code]
    end

    APP_K9--> |depends on| APP_COMMON
    APP_TB --> |depends on| APP_COMMON
    APP_COMMON --> |integrates| FEATURE1
    APP_COMMON --> |integrates| FEATURE2
    APP_COMMON --> |integrates| FEATURE3
    FEATURE1 --> |uses| CORE1
    FEATURE1 --> |uses| LIB2
    FEATURE2 --> |uses| CORE2
    FEATURE2 --> |uses| CORE3
    APP_COMMON -.-> |integrates| LEG
    LEG -.-> |migrate to| FEATURE3
    LEG -.-> |migrate to| CORE3

    classDef module fill:yellow
    classDef app fill:azure
    classDef app_common fill:#ddd
    classDef featureK9 fill:#ffcccc,stroke:#cc0000
    classDef featureTB fill:#ccccff,stroke:#0000cc
    classDef legacy fill:#F99

    class APP_K9,APP_TB app
    class APP_COMMON app_common
    class FEATURE_K9 featureK9
    class FEATURE_TB featureTB
    class LEGACY legacy
```

