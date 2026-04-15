package net.thunderbird.core.common.provider

import androidx.compose.runtime.Composable

fun interface BrandTypographyProvider {
    @Composable
    fun UsingTypography(content: @Composable () -> Unit)
}
