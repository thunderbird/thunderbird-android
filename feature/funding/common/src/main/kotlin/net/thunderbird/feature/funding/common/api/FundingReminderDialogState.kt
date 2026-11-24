package net.thunderbird.feature.funding.common.api

import androidx.annotation.DrawableRes
import net.thunderbird.core.common.resources.StringRes

data class FundingReminderDialogState(
    @StringRes val headerRes: Int?,
    @StringRes val bodyRes: Int?,
    @DrawableRes val imageRes: Int?,
    @StringRes val imageDescRes: Int?,
    val isBannerDialog: Boolean = true,
)
