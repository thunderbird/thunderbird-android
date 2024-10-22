package app.k9mail.provider

import android.content.Context
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.common.provider.BrandNameProvider
import com.fsck.k9.R
import com.fsck.k9.preferences.FilePrefixProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class K9AppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider, FilePrefixProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val filePrefix: String
        get() {
            val now = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return "k9_${dateFormat.format(now.time)}"
        }
}
