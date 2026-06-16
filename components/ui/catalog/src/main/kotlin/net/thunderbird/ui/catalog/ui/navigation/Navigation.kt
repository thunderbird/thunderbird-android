package net.thunderbird.ui.catalog.ui.navigation

import androidx.navigation.NavGraphBuilder

interface Navigation<T : Route> {
    fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (T) -> Unit,
    )
}
