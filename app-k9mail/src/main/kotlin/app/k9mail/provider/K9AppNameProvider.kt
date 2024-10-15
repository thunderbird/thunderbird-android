package app.k9mail.provider

import android.content.Context
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.common.provider.BrandNameProvider
import com.fsck.k9.R

class K9AppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.app_name)
    }
}
