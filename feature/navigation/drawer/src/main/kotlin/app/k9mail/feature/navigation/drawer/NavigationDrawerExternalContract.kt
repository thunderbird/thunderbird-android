package app.k9mail.feature.navigation.drawer

import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig

interface NavigationDrawerExternalContract {

    fun interface DrawerConfigLoader {
        fun loadDrawerConfig(): DrawerConfig
    }
}
