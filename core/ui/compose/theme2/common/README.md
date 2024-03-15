## Core - UI - Compose - Theme2 - Common

This provides the common `MainTheme` with dark/light variation support, a wrapper for the Compose Material 3 theme. It supports [CompositionLocal](https://developer.android.com/jetpack/compose/compositionlocal) changes to colors, typography, shapes and adds additionally elevations, sizes, spacings and images.

To change Material 3 related properties use `MainTheme` instead of `MaterialTheme`:

- `MainTheme.colors`: Material 3 color scheme
- `MainTheme.elevations`: Elevation levels as [defined](https://m3.material.io/styles/elevation/overview) in Material3
- `MainTheme.images`: Images used across the theme, e.g. logo
- `MainTheme.shapes`: Shapes as [defined](https://m3.material.io/styles/shape/overview) in Material 3
- `MainTheme.sizes`: Sizes (smaller, small, medium, large, larger, huge, huger)
- `MainTheme.spacings`: Spacings (quarter, half, default, oneHalf, double, triple, quadruple) while default is 8 dp.
- `MainTheme.typography`: Material 3 typography

To use the MainTheme, you need to provide a `ThemeConfig` with your desired colors, typography, shapes, elevations, sizes, spacings and images. The `ThemeConfig` is a data class that holds all the necessary information for the `MainTheme` to work.
