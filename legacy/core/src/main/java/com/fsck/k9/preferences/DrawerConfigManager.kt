package com.fsck.k9.preferences

import net.thunderbird.core.preference.PreferenceManager
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

interface DrawerConfigManager : PreferenceManager<DrawerConfig>
