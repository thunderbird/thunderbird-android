package net.thunderbird.core.ui.compose.preference.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.ui.components.PreferenceTopBar
import net.thunderbird.core.ui.compose.preference.ui.components.dialog.PreferenceDialog
import net.thunderbird.core.ui.compose.preference.ui.components.list.PreferenceList

@Composable
internal fun PreferenceViewWithDialog(
    title: String,
    preferences: ImmutableList<Preference>,
    onPreferenceChange: (PreferenceSetting<*>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember(0) { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            PreferenceTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        ResponsiveWidthContainer {
            PreferenceList(
                preferences = preferences,
                onItemClick = { index, _ ->
                    selectedIndex = index
                    showDialog = true
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDialog) {
        val preference = preferences[selectedIndex]

        PreferenceDialog(
            preference = preference,
            onConfirmClick = { preference ->
                onPreferenceChange(preference)
                showDialog = false
            },
            onDismissClick = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }
}
