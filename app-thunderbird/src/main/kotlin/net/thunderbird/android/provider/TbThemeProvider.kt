package net.thunderbird.android.provider

import net.thunderbird.android.R
import net.thunderbird.core.ui.theme.api.ThemeProvider

internal class TbThemeProvider : ThemeProvider {
    override val appThemeResourceId = R.style.Theme_Thunderbird_DayNight
    override val appLightThemeResourceId = R.style.Theme_Thunderbird_Light
    override val appDarkThemeResourceId = R.style.Theme_Thunderbird_Dark
    override val dialogThemeResourceId = R.style.Theme_Thunderbird_DayNight_Dialog
    override val translucentDialogThemeResourceId = R.style.Theme_Thunderbird_DayNight_Dialog_Translucent
}
