package net.thunderbird.android.provider

import android.content.Context
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.common.provider.BrandNameProvider
import net.thunderbird.android.R

class TbAppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.brand_name)
    }
}
