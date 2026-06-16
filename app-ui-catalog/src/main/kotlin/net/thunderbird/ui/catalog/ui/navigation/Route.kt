package net.thunderbird.ui.catalog.ui.navigation

interface Route {
    val basePath: String

    fun route(): String
}
