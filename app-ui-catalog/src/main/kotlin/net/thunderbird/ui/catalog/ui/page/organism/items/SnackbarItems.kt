package net.thunderbird.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarDuration
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHost
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.rememberSnackbarHostState
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.coroutines.launch

@Composable
fun SnackbarItems(modifier: Modifier = Modifier) {
    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            SnackbarSubsection(title = "Without action") {
                ButtonText(
                    text = "Show snackbar",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Snackbar message",
                                actionLabel = null,
                            )
                        }
                    },
                )
            }
            SnackbarSubsection(title = "With action") {
                ButtonText(
                    text = "Show snackbar",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Snackbar message",
                                actionLabel = "The action",
                            )
                        }
                    },
                )
            }
            SnackbarDuration.entries.forEach { duration ->
                SnackbarSubsection(title = "With ${duration.name} duration") {
                    ButtonText(
                        text = "Show snackbar",
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Snackbar message with ${duration.name} of duration",
                                    duration = duration,
                                )
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SnackbarSubsection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.double,
                top = MainTheme.spacings.default,
                end = MainTheme.spacings.double,
            ),
    ) {
        TextTitleMedium(text = title)
        DividerHorizontal()
        content()
    }
}
