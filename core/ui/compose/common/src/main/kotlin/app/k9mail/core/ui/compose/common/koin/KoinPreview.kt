package app.k9mail.core.ui.compose.common.koin

import androidx.compose.runtime.Composable
import org.koin.compose.KoinContext
import org.koin.core.Koin
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/**
 * Helper to make Compose previews work when some dependencies are injected via Koin.
 */
fun koinPreview(moduleDeclaration: ModuleDeclaration): KoinPreview {
    val koin = Koin().apply {
        loadModules(listOf(module(moduleDeclaration = moduleDeclaration)))
    }

    return KoinPreview(koin)
}

class KoinPreview internal constructor(private val koin: Koin) {
    @Composable
    infix fun WithContent(content: @Composable () -> Unit) {
        KoinContext(context = koin) {
            content()
        }
    }
}
