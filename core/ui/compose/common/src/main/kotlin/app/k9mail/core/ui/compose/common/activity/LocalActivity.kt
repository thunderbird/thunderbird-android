package app.k9mail.core.ui.compose.common.activity

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    error("No value for LocalActivity provided")
}

fun ComponentActivity.setActivityContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit,
) {
    setContent(parent) {
        CompositionLocalProvider(LocalActivity provides this, content = content)
    }
}
