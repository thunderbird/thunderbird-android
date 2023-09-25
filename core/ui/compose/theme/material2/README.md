## Core - UI - Compose - Theme

This provides the `MainTheme` with dark/light variation, a wrapper for the Compose Material2 theme. It supports [CompositionLocal](https://developer.android.com/jetpack/compose/compositionlocal) changes to colors, typography, shapes and adds additionally elevations, sizes, spacings and images.

To change Material2 related properties use `MainTheme` instead of `MaterialTheme`:

- `MainTheme.colors`: Material2 colors
- `MainTheme.typography`: Material 2 typography
- `MainTheme.shapes`: Material2 shapes
- `MainTheme.spacings`: Spacings (quarter, half, default, oneHalf, double, triple, quadruple) while default is 8 dp.
- `MainTheme.sizes`: Sizes (smaller, small, medium, large, larger, huge, huger)
- `MainTheme.elevations`: Elevation, e.g. card
- `MainTheme.images`: Images used across the theme, e.g. logo

Included are two derived themes for K-9 and Thunderbird look: `K9Theme` and `ThunderbirdTheme`.

To render previews for both themes use `PreviewWithThemes`. This also includes a dark/light variation:

```
@Preview(showBackground = true)
@Composable
fun MyViewPreview() {
    PreviewWithThemes {
        MyView()
    }
}
```
