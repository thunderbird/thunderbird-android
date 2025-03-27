package net.thunderbird.feature.account.settings.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.ui.PreferenceView

@Composable
fun AccountSettingsScreen(
    accountId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember {
        mutableStateOf(
            listOf<Preference>(
                PreferenceSetting.Text(
                    id = "1",
                    title = { "Title 1" },
                    description = { "Description 1" },
                    icon = { Icons.Outlined.Delete },
                    value = "Value 1",
                ),
                PreferenceSetting.Text(
                    id = "2",
                    title = { "Title 2" },
                    description = { "Description 2" },
                    icon = { Icons.Outlined.Delete },
                    value = "Value 2",
                ),
                PreferenceSetting.Text(
                    id = "3",
                    title = { "Title 3" },
                    description = { "Description 3" },
                    icon = { Icons.Outlined.Folder },
                    value = "Value 3",
                ),
            ),
        )
    }

    BackHandler(onBack = onBack)

    PreferenceView(
        title = "Account settings",
        subtitle = accountId,
        preferences = preferences.value.toImmutableList(),
        onPreferenceChange = { preference ->
            preferences.value = updatePreference(
                preferences = preferences.value,
                preference = preference,
            )
        },
        onBack = onBack,
        modifier = modifier,
    )
}

private fun updatePreference(
    preferences: List<Preference>,
    preference: PreferenceSetting<*>,
): List<Preference> {
    return preferences.map {
        if (it is PreferenceSetting<*> && it.id == preference.id) {
            preference
        } else {
            it
        }
    }
}
