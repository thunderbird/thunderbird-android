package net.thunderbird.core.android.common.resources

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import net.thunderbird.core.common.resources.ResourceManager

internal class AndroidResourceManager(
    private val context: Context,
) : ResourceManager {
    override fun stringResource(@StringRes resourceId: Int): String = context.resources.getString(resourceId)

    override fun stringResource(@StringRes resourceId: Int, vararg formatArgs: Any?): String =
        context.resources.getString(resourceId, *formatArgs)

    override fun pluralsString(
        @PluralsRes resourceId: Int,
        quantity: Int,
        vararg formatArgs: Any?,
    ): String = context.resources.getQuantityString(resourceId, quantity, *formatArgs)
}
