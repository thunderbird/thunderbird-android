package com.fsck.k9.provider

import app.k9mail.core.ui.theme.api.ThemeProvider
import com.fsck.k9.R

class K9ThemeProvider : ThemeProvider {
    override val appThemeResourceId = R.style.Theme_K9_DayNight
    override val appLightThemeResourceId = R.style.Theme_K9_Light
    override val appDarkThemeResourceId = R.style.Theme_K9_Dark
    override val dialogThemeResourceId = R.style.Theme_K9_DayNight_Dialog
    override val translucentDialogThemeResourceId = R.style.Theme_K9_DayNight_Dialog_Translucent
}
