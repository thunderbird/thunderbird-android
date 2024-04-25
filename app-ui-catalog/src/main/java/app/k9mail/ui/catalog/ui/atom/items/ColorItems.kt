package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.ui.catalog.ui.atom.view.ColorContent
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

@Suppress("LongMethod")
fun LazyGridScope.colorItems() {
    sectionHeaderItem(text = "Material 3 theme colors")
    item {
        ColorContent(
            text = "Primary",
            color = MainTheme.colors.primary,
            textColor = MainTheme.colors.onPrimary,
        )
    }
    item {
        ColorContent(
            text = "On Primary",
            color = MainTheme.colors.onPrimary,
            textColor = MainTheme.colors.primary,
        )
    }
    item {
        ColorContent(
            text = "Primary Container",
            color = MainTheme.colors.primaryContainer,
            textColor = MainTheme.colors.onPrimaryContainer,
        )
    }
    item {
        ColorContent(
            text = "On Primary Container",
            color = MainTheme.colors.onPrimaryContainer,
            textColor = MainTheme.colors.primaryContainer,
        )
    }

    item {
        ColorContent(
            text = "Secondary",
            color = MainTheme.colors.secondary,
            textColor = MainTheme.colors.onSecondary,
        )
    }
    item {
        ColorContent(
            text = "On Secondary",
            color = MainTheme.colors.onSecondary,
            textColor = MainTheme.colors.secondary,
        )
    }
    item {
        ColorContent(
            text = "Secondary Container",
            color = MainTheme.colors.secondaryContainer,
            textColor = MainTheme.colors.onSecondaryContainer,
        )
    }
    item {
        ColorContent(
            text = "On Secondary Container",
            color = MainTheme.colors.onSecondaryContainer,
            textColor = MainTheme.colors.secondaryContainer,
        )
    }

    item {
        ColorContent(
            text = "Tertiary",
            color = MainTheme.colors.tertiary,
            textColor = MainTheme.colors.onTertiary,
        )
    }
    item {
        ColorContent(
            text = "On Tertiary",
            color = MainTheme.colors.onTertiary,
            textColor = MainTheme.colors.tertiary,
        )
    }
    item {
        ColorContent(
            text = "Tertiary Container",
            color = MainTheme.colors.tertiaryContainer,
            textColor = MainTheme.colors.onTertiaryContainer,
        )
    }
    item {
        ColorContent(
            text = "On Tertiary Container",
            color = MainTheme.colors.onTertiaryContainer,
            textColor = MainTheme.colors.tertiaryContainer,
        )
    }

    item {
        ColorContent(
            text = "Error",
            color = MainTheme.colors.error,
            textColor = MainTheme.colors.onError,
        )
    }
    item {
        ColorContent(
            text = "On Error",
            color = MainTheme.colors.onError,
            textColor = MainTheme.colors.error,
        )
    }
    item {
        ColorContent(
            text = "Error Container",
            color = MainTheme.colors.errorContainer,
            textColor = MainTheme.colors.onErrorContainer,
        )
    }
    item {
        ColorContent(
            text = "On Error Container",
            color = MainTheme.colors.onErrorContainer,
            textColor = MainTheme.colors.errorContainer,
        )
    }

    item {
        ColorContent(
            text = "Surface",
            color = MainTheme.colors.surface,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "On Surface",
            color = MainTheme.colors.onSurface,
            textColor = MainTheme.colors.surface,
        )
    }
    item {
        ColorContent(
            text = "On Surface Variant",
            color = MainTheme.colors.onSurfaceVariant,
            textColor = MainTheme.colors.surface,
        )
    }
    item {
        ColorContent(
            text = "Surface Container Lowest",
            color = MainTheme.colors.surfaceContainerLowest,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "Surface Container Low",
            color = MainTheme.colors.surfaceContainerLow,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "Surface Container",
            color = MainTheme.colors.surfaceContainer,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "Surface Container High",
            color = MainTheme.colors.surfaceContainerHigh,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "Surface Container Highest",
            color = MainTheme.colors.surfaceContainerHighest,
            textColor = MainTheme.colors.onSurface,
        )
    }

    item {
        ColorContent(
            text = "Inverse Surface",
            color = MainTheme.colors.inverseSurface,
            textColor = MainTheme.colors.inverseOnSurface,
        )
    }
    item {
        ColorContent(
            text = "Inverse On Surface",
            color = MainTheme.colors.inverseOnSurface,
            textColor = MainTheme.colors.inverseSurface,
        )
    }
    item {
        ColorContent(
            text = "Inverse Primary",
            color = MainTheme.colors.inversePrimary,
            textColor = MainTheme.colors.onPrimaryContainer,
        )
    }

    item {
        ColorContent(
            text = "Outline",
            color = MainTheme.colors.outline,
            textColor = MainTheme.colors.surface,
        )
    }

    item {
        ColorContent(
            text = "Outline Variant",
            color = MainTheme.colors.outlineVariant,
            textColor = MainTheme.colors.inverseSurface,
        )
    }

    item {
        ColorContent(
            text = "Surface Bright",
            color = MainTheme.colors.surfaceBright,
            textColor = MainTheme.colors.onSurface,
        )
    }
    item {
        ColorContent(
            text = "Surface Dim",
            color = MainTheme.colors.surfaceDim,
            textColor = MainTheme.colors.onSurface,
        )
    }

    sectionHeaderItem(text = "Material 3 theme custom colors")

    item {
        ColorContent(
            text = "Info",
            color = MainTheme.colors.info,
            textColor = MainTheme.colors.onInfo,
        )
    }
    item {
        ColorContent(
            text = "On Info",
            color = MainTheme.colors.onInfo,
            textColor = MainTheme.colors.info,
        )
    }
    item {
        ColorContent(
            text = "Info Container",
            color = MainTheme.colors.infoContainer,
            textColor = MainTheme.colors.onInfoContainer,
        )
    }
    item {
        ColorContent(
            text = "On Info Container",
            color = MainTheme.colors.onInfoContainer,
            textColor = MainTheme.colors.infoContainer,
        )
    }

    item {
        ColorContent(
            text = "Success",
            color = MainTheme.colors.success,
            textColor = MainTheme.colors.onSuccess,
        )
    }
    item {
        ColorContent(
            text = "On Success",
            color = MainTheme.colors.onSuccess,
            textColor = MainTheme.colors.success,
        )
    }
    item {
        ColorContent(
            text = "Success Container",
            color = MainTheme.colors.successContainer,
            textColor = MainTheme.colors.onSuccessContainer,
        )
    }
    item {
        ColorContent(
            text = "On Success Container",
            color = MainTheme.colors.onSuccessContainer,
            textColor = MainTheme.colors.successContainer,
        )
    }

    item {
        ColorContent(
            text = "Warning",
            color = MainTheme.colors.warning,
            textColor = MainTheme.colors.onWarning,
        )
    }
    item {
        ColorContent(
            text = "On Warning",
            color = MainTheme.colors.onWarning,
            textColor = MainTheme.colors.warning,
        )
    }
    item {
        ColorContent(
            text = "Warning Container",
            color = MainTheme.colors.warningContainer,
            textColor = MainTheme.colors.onWarningContainer,
        )
    }
    item {
        ColorContent(
            text = "On Warning Container",
            color = MainTheme.colors.onWarningContainer,
            textColor = MainTheme.colors.warningContainer,
        )
    }
}
