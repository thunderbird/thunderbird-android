package com.fsck.k9.preferences

import net.thunderbird.core.preferences.ConfigManager
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

interface DrawerConfigManager : ConfigManager<DrawerConfig>
