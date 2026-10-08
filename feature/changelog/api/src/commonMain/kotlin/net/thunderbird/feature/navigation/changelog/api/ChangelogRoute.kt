package net.thunderbird.feature.navigation.changelog.api

import kotlinx.serialization.Serializable
import net.thunderbird.core.ui.navigation.Route

@Serializable
data class ChangelogRoute(val changeLogMode: ChangeLogMode) : Route {
    override val basePath: String = BASE_PATH
    override fun route() = "$basePath/$changeLogMode"

    companion object {
        const val BASE_PATH = "app://changelog"
    }
}

enum class ChangeLogMode {
    CHANGE_LOG,
    RECENT_CHANGES,
}
