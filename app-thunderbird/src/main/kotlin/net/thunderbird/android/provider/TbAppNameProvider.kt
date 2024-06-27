package net.thunderbird.android.provider

import android.content.Context
import app.k9mail.core.common.provider.AppNameProvider
import net.thunderbird.android.R

class TbAppNameProvider(
    context: Context,
) : AppNameProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }
}
