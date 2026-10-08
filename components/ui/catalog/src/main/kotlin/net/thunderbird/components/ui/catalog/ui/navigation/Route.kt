package net.thunderbird.components.ui.catalog.ui.navigation

interface Route {
    val basePath: String

    fun route(): String
}
