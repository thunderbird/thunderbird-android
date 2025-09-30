package app.k9mail.core.ui.compose.common.koin

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplicationPreview
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/**
 * Helper to make Compose previews work when some dependencies are injected via Koin.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * @Preview()
 * fun SomeComposablePreview() {
 *     koinPreview {
 *        // Declare Koin modules here
 *        factory { SomeDependency() }
 *     } WithContent {
 *     // Your preview content here
 *     }
 * }
 * ```
 */
fun koinPreview(moduleDeclaration: ModuleDeclaration): KoinPreview {
    val modules = listOf(module(moduleDeclaration = moduleDeclaration))

    return KoinPreview(modules)
}

class KoinPreview internal constructor(private val modulesList: List<Module>) {
    @Composable
    infix fun WithContent(content: @Composable () -> Unit) {
        KoinApplicationPreview(application = { modules(modulesList) }) {
            content()
        }
    }
}
