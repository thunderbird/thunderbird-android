package net.thunderbird.core.ui.setting.dialog.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.ui.setting.dialog.ui.components.SettingTopBar
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.SettingDialog
import net.thunderbird.core.ui.setting.dialog.ui.components.list.SettingList

@Composable
internal fun DialogSettingView(
    title: String,
    settings: Settings,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SettingTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        ResponsiveWidthContainer { contentPadding ->
            SettingList(
                settings = settings,
                onItemClick = { index, _ ->
                    selectedIndex = index
                    showDialog = true
                },
                onSettingValueChange = onSettingValueChange,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(contentPadding),
            )
        }
    }

    if (showDialog) {
        val setting = settings[selectedIndex]

        SettingDialog(
            setting = setting,
            onConfirmClick = { setting ->
                onSettingValueChange(setting)
                showDialog = false
            },
            onDismissClick = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }
}
