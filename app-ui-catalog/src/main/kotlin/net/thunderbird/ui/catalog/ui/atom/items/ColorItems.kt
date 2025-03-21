package net.thunderbird.ui.catalog.ui.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.ui.atom.view.ColorContent
import net.thunderbird.ui.catalog.ui.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.common.list.wideItem

@Suppress("LongMethod")
fun LazyGridScope.colorItems() {
    sectionHeaderItem(text = "Material 3 theme colors")
    wideItem {
        ColorContent(
            text = "Primary",
            color = MainTheme.colors.primary,
            textColor = MainTheme.colors.onPrimary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Primary",
            color = MainTheme.colors.onPrimary,
            textColor = MainTheme.colors.primary,
        )
    }
    wideItem {
        ColorContent(
            text = "Primary Container",
            color = MainTheme.colors.primaryContainer,
            textColor = MainTheme.colors.onPrimaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Primary Container",
            color = MainTheme.colors.onPrimaryContainer,
            textColor = MainTheme.colors.primaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Secondary",
            color = MainTheme.colors.secondary,
            textColor = MainTheme.colors.onSecondary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Secondary",
            color = MainTheme.colors.onSecondary,
            textColor = MainTheme.colors.secondary,
        )
    }
    wideItem {
        ColorContent(
            text = "Secondary Container",
            color = MainTheme.colors.secondaryContainer,
            textColor = MainTheme.colors.onSecondaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Secondary Container",
            color = MainTheme.colors.onSecondaryContainer,
            textColor = MainTheme.colors.secondaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Tertiary",
            color = MainTheme.colors.tertiary,
            textColor = MainTheme.colors.onTertiary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Tertiary",
            color = MainTheme.colors.onTertiary,
            textColor = MainTheme.colors.tertiary,
        )
    }
    wideItem {
        ColorContent(
            text = "Tertiary Container",
            color = MainTheme.colors.tertiaryContainer,
            textColor = MainTheme.colors.onTertiaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Tertiary Container",
            color = MainTheme.colors.onTertiaryContainer,
            textColor = MainTheme.colors.tertiaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Error",
            color = MainTheme.colors.error,
            textColor = MainTheme.colors.onError,
        )
    }
    wideItem {
        ColorContent(
            text = "On Error",
            color = MainTheme.colors.onError,
            textColor = MainTheme.colors.error,
        )
    }
    wideItem {
        ColorContent(
            text = "Error Container",
            color = MainTheme.colors.errorContainer,
            textColor = MainTheme.colors.onErrorContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Error Container",
            color = MainTheme.colors.onErrorContainer,
            textColor = MainTheme.colors.errorContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Surface",
            color = MainTheme.colors.surface,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "On Surface",
            color = MainTheme.colors.onSurface,
            textColor = MainTheme.colors.surface,
        )
    }
    wideItem {
        ColorContent(
            text = "On Surface Variant",
            color = MainTheme.colors.onSurfaceVariant,
            textColor = MainTheme.colors.surface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Lowest",
            color = MainTheme.colors.surfaceContainerLowest,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Low",
            color = MainTheme.colors.surfaceContainerLow,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container",
            color = MainTheme.colors.surfaceContainer,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container High",
            color = MainTheme.colors.surfaceContainerHigh,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Highest",
            color = MainTheme.colors.surfaceContainerHighest,
            textColor = MainTheme.colors.onSurface,
        )
    }

    wideItem {
        ColorContent(
            text = "Inverse Surface",
            color = MainTheme.colors.inverseSurface,
            textColor = MainTheme.colors.inverseOnSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Inverse On Surface",
            color = MainTheme.colors.inverseOnSurface,
            textColor = MainTheme.colors.inverseSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Inverse Primary",
            color = MainTheme.colors.inversePrimary,
            textColor = MainTheme.colors.onPrimaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Outline",
            color = MainTheme.colors.outline,
            textColor = MainTheme.colors.surface,
        )
    }

    wideItem {
        ColorContent(
            text = "Outline Variant",
            color = MainTheme.colors.outlineVariant,
            textColor = MainTheme.colors.inverseSurface,
        )
    }

    wideItem {
        ColorContent(
            text = "Surface Bright",
            color = MainTheme.colors.surfaceBright,
            textColor = MainTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Dim",
            color = MainTheme.colors.surfaceDim,
            textColor = MainTheme.colors.onSurface,
        )
    }

    sectionHeaderItem(text = "Material 3 theme custom colors")

    wideItem {
        ColorContent(
            text = "Info",
            color = MainTheme.colors.info,
            textColor = MainTheme.colors.onInfo,
        )
    }
    wideItem {
        ColorContent(
            text = "On Info",
            color = MainTheme.colors.onInfo,
            textColor = MainTheme.colors.info,
        )
    }
    wideItem {
        ColorContent(
            text = "Info Container",
            color = MainTheme.colors.infoContainer,
            textColor = MainTheme.colors.onInfoContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Info Container",
            color = MainTheme.colors.onInfoContainer,
            textColor = MainTheme.colors.infoContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Success",
            color = MainTheme.colors.success,
            textColor = MainTheme.colors.onSuccess,
        )
    }
    wideItem {
        ColorContent(
            text = "On Success",
            color = MainTheme.colors.onSuccess,
            textColor = MainTheme.colors.success,
        )
    }
    wideItem {
        ColorContent(
            text = "Success Container",
            color = MainTheme.colors.successContainer,
            textColor = MainTheme.colors.onSuccessContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Success Container",
            color = MainTheme.colors.onSuccessContainer,
            textColor = MainTheme.colors.successContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Warning",
            color = MainTheme.colors.warning,
            textColor = MainTheme.colors.onWarning,
        )
    }
    wideItem {
        ColorContent(
            text = "On Warning",
            color = MainTheme.colors.onWarning,
            textColor = MainTheme.colors.warning,
        )
    }
    wideItem {
        ColorContent(
            text = "Warning Container",
            color = MainTheme.colors.warningContainer,
            textColor = MainTheme.colors.onWarningContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Warning Container",
            color = MainTheme.colors.onWarningContainer,
            textColor = MainTheme.colors.warningContainer,
        )
    }
}
