package app.k9mail.feature.navigation.drawer

interface NavigationDrawerExternalContract {

    data class DrawerConfig(
        val showUnifiedFolders: Boolean,
        val showStarredCount: Boolean,
        val showUnReadCount: Boolean,
    )

    fun interface DrawerConfigLoader {
        fun loadDrawerConfig(): DrawerConfig
    }
}
