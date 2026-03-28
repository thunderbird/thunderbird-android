package com.fsck.k9.ui.changelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import de.cketti.changelog.ReleaseItem
import kotlinx.collections.immutable.persistentListOf

@Composable
@PreviewLightDark
fun ChangelogScreenPreview() {
    PreviewWithThemesLightDark {
        ChangelogScreen(
            releaseItems = persistentListOf(
                ReleaseItem.newInstance(
                    904,
                    "6.0.904",
                    "2024-06-27",
                    listOf(
                        "Fixed crash when opening an attachment",
                        "Updated Portuguese translation",
                        "Enhanced About screen UI",
                    ),
                ),
                ReleaseItem.newInstance(
                    903,
                    "6.0.903",
                    "2024-05-18",
                    listOf(
                        "Improved About screen Compose layout",
                        "Updated French translation",
                        "Minor UI polish",
                    ),
                ),
                ReleaseItem.newInstance(
                    902,
                    "6.0.902",
                    "2024-05-02",
                    listOf(
                        "Fixed alignment issue in About screen",
                        "Updated German translation",
                        "Improved accessibility labels",
                    ),
                ),
                ReleaseItem.newInstance(
                    901,
                    "6.0.901",
                    "2024-04-20",
                    listOf(
                        "Optimized Compose rendering for About screen",
                        "Updated Spanish translation",
                        "Reduced app startup time",
                    ),
                ),
                ReleaseItem.newInstance(
                    900,
                    "6.0.900",
                    "2024-04-05",
                    listOf(
                        "Initial About screen migration to Jetpack Compose",
                        "Updated French translation",
                        "Improved theme consistency",
                    ),
                ),
                ReleaseItem.newInstance(
                    899,
                    "6.0.899",
                    "2024-03-22",
                    listOf(
                        "Improved attachment preview performance",
                        "Updated Italian translation",
                        "Minor bug fixes",
                    ),
                ),
                ReleaseItem.newInstance(
                    888,
                    "6.0.888",
                    "2024-03-10",
                    listOf(
                        "Improved settings screen stability",
                        "Updated Japanese translation",
                        "UI improvements",
                    ),
                ),
                ReleaseItem.newInstance(
                    887,
                    "6.0.887",
                    "2024-02-28",
                    listOf(
                        "Improved notification handling",
                        "Updated Korean translation",
                        "General stability improvements",
                    ),
                ),
            ),
            showRecentChanges = true,
            onShowRecentChangesCheck = {},
        )
    }
}
