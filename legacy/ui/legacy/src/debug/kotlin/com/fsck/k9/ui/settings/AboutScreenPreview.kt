package com.fsck.k9.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.theme2.k9mail.R
import kotlinx.collections.immutable.persistentListOf

@Composable
@PreviewLightDark
internal fun AboutScreenPreview() {
    PreviewWithThemesLightDark {
        AboutScreen(
            aboutTitle = "About K-9 Mail",
            projectTitle = "Open Source Project",
            librariesTitle = "Libraries",
            versionNumber = "17.0a1",
            appLogoResId = R.drawable.core_ui_theme2_k9mail_logo,
            libraries = fakeLibraryList,
        )
    }
}

private val fakeLibraryList = persistentListOf(
    Library(
        "Android Jetpack libraries",
        "https://developer.android.com/jetpack",
        "Apache License, Version 2.0",
    ),
    Library(
        "AndroidX Preference extended",
        "https://github.com/takisoft/preferencex-android",
        "Apache License, Version 2.0",
    ),
    Library("AppAuth for Android", "https://github.com/openid/AppAuth-Android", "Apache License, Version 2.0"),
    Library("Apache HttpComponents", "https://hc.apache.org/", "Apache License, Version 2.0"),
    Library("AutoValue", "https://github.com/google/auto", "Apache License, Version 2.0"),
    Library("CircleImageView", "https://github.com/hdodenhof/CircleImageView", "Apache License, Version 2.0"),
    Library("ckChangeLog", "https://github.com/cketti/ckChangeLog", "Apache License, Version 2.0"),
    Library("Commons IO", "https://commons.apache.org/io/", "Apache License, Version 2.0"),
    Library("ColorPicker", "https://github.com/gregkorossy/ColorPicker", "Apache License, Version 2.0"),
    Library("DateTimePicker", "https://github.com/gregkorossy/DateTimePicker", "Apache License, Version 2.0"),
    Library("Error Prone annotations", "https://github.com/google/error-prone", "Apache License, Version 2.0"),
)
