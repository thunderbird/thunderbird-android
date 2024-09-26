package app.k9mail.feature.settings.import.ui

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.fragment.compose.AndroidFragment
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.settings.importing.R

@Composable
fun SettingsImportScreen(
    onImportSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_import_title),
                navigationIcon = {
                    ButtonIcon(
                        onClick = onBack,
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    )
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        SettingsImportContent(onImportSuccess, onBack, innerPadding)
    }
}

@Composable
private fun SettingsImportContent(
    onImportSuccess: () -> Unit,
    onBack: () -> Unit,
    paddingValues: PaddingValues,
) {
    val activity = LocalActivity.current as FragmentActivity

    activity.supportFragmentManager.setFragmentResultListener(
        SettingsImportFragment.FRAGMENT_RESULT_KEY,
        LocalLifecycleOwner.current,
    ) { _, result: Bundle ->
        if (result.getBoolean(SettingsImportFragment.FRAGMENT_RESULT_ACCOUNT_IMPORTED, false)) {
            onImportSuccess()
        } else {
            onBack()
        }
    }

    AndroidFragment<SettingsImportFragment>(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    )
}
