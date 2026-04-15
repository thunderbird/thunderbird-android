package net.thunderbird.core.common.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import org.koin.compose.koinInject

@Stable
fun interface BrandTypographyProvider {
    @Composable
    fun UsingTypography(content: @Composable () -> Unit)
}

@Composable
fun UsingBrandTypography(
    provider: BrandTypographyProvider = koinInject(),
    content: @Composable () -> Unit,
) = provider.UsingTypography(content)
