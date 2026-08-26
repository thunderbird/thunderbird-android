package net.thunderbird.android.provider

import net.thunderbird.android.R
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider

class TbChangelogConfigProvider : ChangelogConfigProvider {
    override val changelogIndexResId: Int = R.raw.changelog_index
}
