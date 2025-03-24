package net.thunderbird.ui.catalog.ui.navigation

import app.k9mail.core.ui.compose.navigation.Route
import kotlinx.serialization.Serializable

sealed interface CatalogRoute : Route {

    @Serializable
    data object Atom : CatalogRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        const val BASE_PATH = "$CATALOG_BASE_PATH/atom"
    }

    @Serializable
    data object Molecule : CatalogRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        const val BASE_PATH = "$CATALOG_BASE_PATH/molecule"
    }

    @Serializable
    data object Organism : CatalogRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        const val BASE_PATH = "$CATALOG_BASE_PATH/organism"
    }

    @Serializable
    data object Template : CatalogRoute {
        override val basePath: String = BASE_PATH

        override fun route(): String = basePath

        const val BASE_PATH = "$CATALOG_BASE_PATH/template"
    }

    companion object {
        const val CATALOG_BASE_PATH = "app://catalog"
    }
}
