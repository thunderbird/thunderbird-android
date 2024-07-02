package com.fsck.k9.ui

import com.fsck.k9.ui.base.ThemeProvider

// TODO: Move this class and the theme resources to the main app module
class K9ThemeProvider : ThemeProvider {
    override val appThemeResourceId = R.style.Theme_K9_DayNight
    override val appLightThemeResourceId = R.style.Theme_K9_Light
    override val appDarkThemeResourceId = R.style.Theme_K9_Dark
    override val dialogThemeResourceId = R.style.Theme_K9_DayNight_Dialog
    override val translucentDialogThemeResourceId = R.style.Theme_K9_DayNight_Dialog_Translucent
}
