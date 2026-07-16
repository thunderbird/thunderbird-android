package net.thunderbird.components.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.catalog.ui.page.atom.view.ColorContent
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.wideItem

@Suppress("LongMethod")
fun LazyGridScope.colorItems() {
    sectionHeaderItem(text = "Material 3 theme colors")
    wideItem {
        ColorContent(
            text = "Primary",
            color = BoltTheme.colors.primary,
            textColor = BoltTheme.colors.onPrimary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Primary",
            color = BoltTheme.colors.onPrimary,
            textColor = BoltTheme.colors.primary,
        )
    }
    wideItem {
        ColorContent(
            text = "Primary Container",
            color = BoltTheme.colors.primaryContainer,
            textColor = BoltTheme.colors.onPrimaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Primary Container",
            color = BoltTheme.colors.onPrimaryContainer,
            textColor = BoltTheme.colors.primaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Secondary",
            color = BoltTheme.colors.secondary,
            textColor = BoltTheme.colors.onSecondary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Secondary",
            color = BoltTheme.colors.onSecondary,
            textColor = BoltTheme.colors.secondary,
        )
    }
    wideItem {
        ColorContent(
            text = "Secondary Container",
            color = BoltTheme.colors.secondaryContainer,
            textColor = BoltTheme.colors.onSecondaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Secondary Container",
            color = BoltTheme.colors.onSecondaryContainer,
            textColor = BoltTheme.colors.secondaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Tertiary",
            color = BoltTheme.colors.tertiary,
            textColor = BoltTheme.colors.onTertiary,
        )
    }
    wideItem {
        ColorContent(
            text = "On Tertiary",
            color = BoltTheme.colors.onTertiary,
            textColor = BoltTheme.colors.tertiary,
        )
    }
    wideItem {
        ColorContent(
            text = "Tertiary Container",
            color = BoltTheme.colors.tertiaryContainer,
            textColor = BoltTheme.colors.onTertiaryContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Tertiary Container",
            color = BoltTheme.colors.onTertiaryContainer,
            textColor = BoltTheme.colors.tertiaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Error",
            color = BoltTheme.colors.error,
            textColor = BoltTheme.colors.onError,
        )
    }
    wideItem {
        ColorContent(
            text = "On Error",
            color = BoltTheme.colors.onError,
            textColor = BoltTheme.colors.error,
        )
    }
    wideItem {
        ColorContent(
            text = "Error Container",
            color = BoltTheme.colors.errorContainer,
            textColor = BoltTheme.colors.onErrorContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Error Container",
            color = BoltTheme.colors.onErrorContainer,
            textColor = BoltTheme.colors.errorContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Surface",
            color = BoltTheme.colors.surface,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "On Surface",
            color = BoltTheme.colors.onSurface,
            textColor = BoltTheme.colors.surface,
        )
    }
    wideItem {
        ColorContent(
            text = "On Surface Variant",
            color = BoltTheme.colors.onSurfaceVariant,
            textColor = BoltTheme.colors.surface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Lowest",
            color = BoltTheme.colors.surfaceContainerLowest,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Low",
            color = BoltTheme.colors.surfaceContainerLow,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container",
            color = BoltTheme.colors.surfaceContainer,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container High",
            color = BoltTheme.colors.surfaceContainerHigh,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Container Highest",
            color = BoltTheme.colors.surfaceContainerHighest,
            textColor = BoltTheme.colors.onSurface,
        )
    }

    wideItem {
        ColorContent(
            text = "Inverse Surface",
            color = BoltTheme.colors.inverseSurface,
            textColor = BoltTheme.colors.inverseOnSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Inverse On Surface",
            color = BoltTheme.colors.inverseOnSurface,
            textColor = BoltTheme.colors.inverseSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Inverse Primary",
            color = BoltTheme.colors.inversePrimary,
            textColor = BoltTheme.colors.onPrimaryContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Outline",
            color = BoltTheme.colors.outline,
            textColor = BoltTheme.colors.surface,
        )
    }

    wideItem {
        ColorContent(
            text = "Outline Variant",
            color = BoltTheme.colors.outlineVariant,
            textColor = BoltTheme.colors.inverseSurface,
        )
    }

    wideItem {
        ColorContent(
            text = "Surface Bright",
            color = BoltTheme.colors.surfaceBright,
            textColor = BoltTheme.colors.onSurface,
        )
    }
    wideItem {
        ColorContent(
            text = "Surface Dim",
            color = BoltTheme.colors.surfaceDim,
            textColor = BoltTheme.colors.onSurface,
        )
    }

    sectionHeaderItem(text = "Material 3 theme custom colors")

    wideItem {
        ColorContent(
            text = "Info",
            color = BoltTheme.colors.info,
            textColor = BoltTheme.colors.onInfo,
        )
    }
    wideItem {
        ColorContent(
            text = "On Info",
            color = BoltTheme.colors.onInfo,
            textColor = BoltTheme.colors.info,
        )
    }
    wideItem {
        ColorContent(
            text = "Info Container",
            color = BoltTheme.colors.infoContainer,
            textColor = BoltTheme.colors.onInfoContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Info Container",
            color = BoltTheme.colors.onInfoContainer,
            textColor = BoltTheme.colors.infoContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Success",
            color = BoltTheme.colors.success,
            textColor = BoltTheme.colors.onSuccess,
        )
    }
    wideItem {
        ColorContent(
            text = "On Success",
            color = BoltTheme.colors.onSuccess,
            textColor = BoltTheme.colors.success,
        )
    }
    wideItem {
        ColorContent(
            text = "Success Container",
            color = BoltTheme.colors.successContainer,
            textColor = BoltTheme.colors.onSuccessContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Success Container",
            color = BoltTheme.colors.onSuccessContainer,
            textColor = BoltTheme.colors.successContainer,
        )
    }

    wideItem {
        ColorContent(
            text = "Warning",
            color = BoltTheme.colors.warning,
            textColor = BoltTheme.colors.onWarning,
        )
    }
    wideItem {
        ColorContent(
            text = "On Warning",
            color = BoltTheme.colors.onWarning,
            textColor = BoltTheme.colors.warning,
        )
    }
    wideItem {
        ColorContent(
            text = "Warning Container",
            color = BoltTheme.colors.warningContainer,
            textColor = BoltTheme.colors.onWarningContainer,
        )
    }
    wideItem {
        ColorContent(
            text = "On Warning Container",
            color = BoltTheme.colors.onWarningContainer,
            textColor = BoltTheme.colors.warningContainer,
        )
    }
}
