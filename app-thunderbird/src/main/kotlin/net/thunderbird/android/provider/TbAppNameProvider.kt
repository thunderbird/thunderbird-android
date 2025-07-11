package net.thunderbird.android.provider

import android.content.Context
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.android.R
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider

internal class TbAppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider, FilePrefixProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.brand_name)
    }

    override val filePrefix: String = "thunderbird"
}
