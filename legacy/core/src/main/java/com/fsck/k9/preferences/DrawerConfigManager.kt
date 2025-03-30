package com.fsck.k9.preferences

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.core.preferences.ConfigManager

interface DrawerConfigManager : ConfigManager<DrawerConfig>
