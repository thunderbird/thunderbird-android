package app.k9mail.feature.onboarding.welcome.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.feature.thundermail.ui.brandBackground

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    appNameProvider: AppNameProvider,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        WelcomeContent(
            onStartClick = onStartClick,
            appName = appNameProvider.appName,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(innerPadding),
        )
    }
}
