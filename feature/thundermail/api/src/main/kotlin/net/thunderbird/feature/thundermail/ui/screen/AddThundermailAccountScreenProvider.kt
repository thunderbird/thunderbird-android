package net.thunderbird.feature.thundermail.ui.screen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
fun interface AddThundermailAccountScreenProvider {
    @Composable
    fun Content(
        header: @Composable ColumnScope.() -> Unit,
        onSignWithThundermailClick: () -> Unit,
        onScanQrCodeClick: () -> Unit,
        onSetupAnotherAccountClick: () -> Unit,
        modifier: Modifier,
    )
}
