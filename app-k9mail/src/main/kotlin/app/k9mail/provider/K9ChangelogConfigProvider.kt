package app.k9mail.provider

import com.fsck.k9.R
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider

class K9ChangelogConfigProvider : ChangelogConfigProvider {
    override val changelogIndexResId: Int = R.raw.changelog_index
}
