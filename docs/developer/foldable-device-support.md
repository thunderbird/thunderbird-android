# Foldable Device Support

## Overview

This document describes the foldable device support implementation for Thunderbird Android. The feature automatically switches between single-pane and split-view layouts based on the device's fold state.

## Motivation

Foldable devices like Samsung Galaxy Fold and Google Pixel Fold offer different screen sizes depending on their posture:
- **Folded**: Smaller outer display (typically 6-7 inches)
- **Unfolded**: Large inner display (typically 7-8 inches)

Thunderbird already supports split-screen views, but these are static (Always/Never) or orientation-based (When in Landscape). Users of foldable devices must manually change the setting when switching between folded and unfolded states.

## Implementation

### Components

#### 1. SplitViewMode Enum Extension

**File**: `core/preference/api/src/commonMain/kotlin/net/thunderbird/core/preference/GeneralSettings.kt`

Added new option:

```kotlin
enum class SplitViewMode {
    ALWAYS,
    NEVER,
    WHEN_IN_LANDSCAPE,
    WHEN_UNFOLDED,  // New
}
```

#### 2. FoldableStateObserver

**File**: `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/foldable/FoldableStateObserver.kt`

Responsibilities:
- Observes `WindowInfoTracker` from Jetpack WindowManager
- Converts `WindowLayoutInfo` into simplified `FoldableState`
- Provides `StateFlow<FoldableState>` for lifecycle-aware collection
- Implements 300ms debouncing to prevent layout thrashing

**FoldableState Enum**:

```kotlin
enum class FoldableState {
    FOLDED,      // Device is folded (small screen)
    UNFOLDED,    // Device is unfolded (large screen)
    UNKNOWN,     // Not a foldable or state unknown
}
```

**State Detection**:
- `FoldingFeature.State.FLAT` → `UNFOLDED`
- `FoldingFeature.State.HALF_OPENED` → `UNFOLDED` (laptop mode)
- No `FoldingFeature` → `UNKNOWN`

#### 3. MainActivity Integration

**File**: `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`

Changes:
1. Injection of `FoldableStateObserver` via Koin
2. Lifecycle registration of observer
3. Extended `useSplitView()` with `WHEN_UNFOLDED` logic
4. Flow collection for state changes
5. Automatic `recreate()` on fold/unfold events

### Behavior

#### User Flow

1. User selects Settings → Display → Show split-screen → "When device is unfolded"
2. On folded device: Single-pane view (message list only)
3. Device unfolds:
   - `FoldableStateObserver` detects `UNFOLDED`
   - After 300ms debounce, `handleFoldableStateChange()` is called
   - Activity recreates with split-view layout
   - Message list on left, detail pane on right
4. Device folds:
   - Observer detects `FOLDED`
   - Switches to single-pane
   - Currently displayed message is preserved

#### Technical Flow

```text
WindowInfoTracker (Android System)
    ↓
WindowLayoutInfo with FoldingFeature
    ↓
FoldableStateObserver.processWindowLayoutInfo()
    ↓ (Debounce 300ms)
FoldableState (FOLDED/UNFOLDED/UNKNOWN)
    ↓
StateFlow emission
    ↓
MainActivity.handleFoldableStateChange()
    ↓
recreate() if layout switch needed
    ↓
onCreate() → useSplitView() checks currentState
    ↓
Correct layout loaded
```

### Dependencies

**gradle/libs.versions.toml**:

```toml
[versions]
androidxWindow = "1.3.0"

[libraries]
androidx-window = { module = "androidx.window:window", version.ref = "androidxWindow" }
```

**legacy/ui/legacy/build.gradle.kts**:

```kotlin
implementation(libs.androidx.window)
```

## Edge Cases & Limitations

### Edge Cases

1. **Rapid fold/unfold**:
   - Solution: 300ms debounce prevents multiple recreate calls
2. **Orientation change during fold**:
   - Both events can occur
   - `recreate()` is only called once (Android standard behavior)
3. **Multi-window mode**:
   - WindowManager provides correct information per window
   - Layout based on active window
4. **Half-open state** (laptop mode):
   - Treated as `UNFOLDED`
   - User gets split-view

### Known Limitations

1. **Layout switch via recreate()**:
   - Brief flash during transition
   - Alternative: Dynamic layout swapping (more complex, future improvement)
2. **Tablet detection**:
   - Large tablets without FoldingFeature → `UNKNOWN`
   - User should choose ALWAYS or WHEN_IN_LANDSCAPE
3. **No hinge-position utilization**:
   - `FoldingFeature.bounds` not used
   - Future: Adapt content to hinge position

## Testing

### Unit Tests

**File**: `legacy/ui/legacy/src/test/java/com/fsck/k9/ui/foldable/FoldableStateObserverTest.kt`

Tests:
- FoldableState mapping from WindowLayoutInfo
- Debouncing works correctly
- Lifecycle observation starts/stops correctly
- StateFlow emits correct values

### Manual Testing

**Emulator**: Foldable device (e.g., "7.6\" Fold-in with outer display")

Test scenarios:
1. Setting on WHEN_UNFOLDED
2. Layout switches on fold/unfold
3. Selected message persists
4. Scroll position preserved
5. Multi-window works
6. Orientation change + fold simultaneously

## Future Improvements

1. **Dynamic layout swapping** without `recreate()`:
   - Smooth transitions without restart
   - Runtime fragment container swapping
2. **Hinge-aware layouts**:
   - Content positioning around hinge
   - Avoid important UI elements at fold
3. **Tablet detection**:
   - Auto-detect large non-foldables
   - Auto-enable split-view on tablets
4. **Compose migration**:
   - Foldable-aware Composables
   - `WindowSizeClass` integration

## References

- [Jetpack WindowManager](https://developer.android.com/jetpack/androidx/releases/window)
- [Build apps for foldable devices](https://developer.android.com/guide/topics/large-screens/learn-about-foldables)
- [WindowInfoTracker API](https://developer.android.com/reference/androidx/window/layout/WindowInfoTracker)
- [FoldingFeature](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)

