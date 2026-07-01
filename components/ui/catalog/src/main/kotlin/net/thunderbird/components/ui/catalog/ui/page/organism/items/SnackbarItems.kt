package net.thunderbird.components.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.organism.snackbar.SnackbarDuration
import net.thunderbird.components.ui.bolt.organism.snackbar.SnackbarHost
import net.thunderbird.components.ui.bolt.organism.snackbar.rememberSnackbarHostState
import net.thunderbird.components.ui.bolt.template.Scaffold
import kotlinx.coroutines.launch
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
                start = BoltTheme.spacings.double,
                top = BoltTheme.spacings.default,
                end = BoltTheme.spacings.double,
            ),
    ) {
        TextTitleMedium(text = title)
        DividerHorizontal()
        content()
    }
}
