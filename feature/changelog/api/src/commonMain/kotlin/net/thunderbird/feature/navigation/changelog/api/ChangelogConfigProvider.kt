package net.thunderbird.feature.navigation.changelog.api

import androidx.annotation.RawRes

interface ChangelogConfigProvider {
    @get:RawRes
    val changelogIndexResId: Int
}
