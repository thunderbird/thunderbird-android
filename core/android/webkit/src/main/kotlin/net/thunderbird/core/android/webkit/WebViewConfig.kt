package net.thunderbird.core.android.webkit

import androidx.compose.runtime.Stable

@Stable
data class WebViewConfig(
    val useDarkMode: Boolean,
    val autoFitWidth: Boolean,
    val textZoom: Int,
)
