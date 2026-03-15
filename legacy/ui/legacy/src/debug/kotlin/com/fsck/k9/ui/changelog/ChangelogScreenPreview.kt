package com.fsck.k9.ui.changelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import kotlinx.collections.immutable.persistentListOf

@Composable
@PreviewLightDark
fun ChangelogScreenPreview() {
    PreviewWithThemesLightDark {
        ChangelogScreen(
            releaseItems = releases,
            showRecentChanges = true,
            onShowRecentChangesCheck = {},
        )
    }
}

val releases = persistentListOf(

    ReleaseUiModel(
        version = "18.0b3",
        date = "2026-03-31",
        isLatest = true,
        changes = mapOf(
            ChangeType.FIXED.name to listOf(
                "Crash occurred when retrieving images from large messages",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "18.0b2",
        date = "2026-03-18",
        isLatest = false,
        changes = mapOf(
            ChangeType.FIXED.name to listOf(
                "Crash occurred when attaching attachment",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "18.0b1",
        date = "2026-03-16",
        isLatest = false,
        changes = mapOf(
            ChangeType.NEW.name to listOf(
                "Add support for PNG avatars",
                "Add setting to display ISO date and time in message list",
                "Used fixed-width font for composing when user chooses fixed-width for viewing",
            ),
            ChangeType.CHANGED.name to listOf(
                "Attachment summary moved from message bottom to header for visibility",
            ),
            ChangeType.FIXED.name to listOf(
                "EHLO parsing exception appeared in logcat when sending email",
                "\"Delete (from notification)\" setting did not retain state",
                "Find folder search hint text was cut off on smaller screens",
                "User was not notified if they were offline when sending message",
                "Warned \"Sent folder not found\" when uploading sent messages was disabled",
                "Loading bar appeared detached from header, leaving visible gap",
                "CC/BCC fields were not expanded by default for Reply All",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "17.0b4",
        date = "2026-02-24",
        isLatest = false,
        changes = mapOf(
            ChangeType.FIXED.name to listOf(
                "Application navigation and account switching could crash",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "17.0b3",
        date = "2026-02-17",
        isLatest = false,
        changes = mapOf(
            ChangeType.FIXED.name to listOf(
                "Application could crash when rendering message list",
                "Avatar image updates did not propagate immediately",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "17.0b2",
        date = "2026-02-04",
        isLatest = false,
        changes = mapOf(
            ChangeType.CHANGED.name to listOf(
                "Crash occurred in 17.0b1 split view mode while in landscape",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "17.0b1",
        date = "2026-01-05",
        isLatest = false,
        changes = mapOf(
            ChangeType.CHANGED.name to listOf(
                "Account avatar customization",
                "Foldable device support with split-screen",
                "Email messages can be printed",
                "Unified folder account indicator identifies message ownership",
                "Display account avatar in message notifications",
                "Improved email rendering enabled enabled for beta",
                "Deleted/read messages in unified inbox may not update until manual refresh",
                "Edge-to-edge regressions affected account setup screens",
                "Tap behavior was unreliable in recipient fields",
                "Incorrect icon was displayed for 'Find folder'",
                "Device orientiation change duplicated recipient field text",
                "Copied link text incorrectly included CSS",
                "Unselecting 'Colorize contact pictures' setting did not persist",
                "Some general settings were not preserved after export and import",
            ),
        ),
    ),

    ReleaseUiModel(
        version = "16.0b4",
        date = "2026-01-04",
        isLatest = false,
        changes = mapOf(
            ChangeType.CHANGED.name to listOf(
                "OAuth SMTP authentication failed with some Microsoft servers",
            ),
        ),
    ),
)
