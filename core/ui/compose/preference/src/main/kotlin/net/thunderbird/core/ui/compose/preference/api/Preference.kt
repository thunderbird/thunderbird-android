package net.thunderbird.core.ui.compose.preference.api

import android.os.Parcelable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * A preference that can be displayed in a preference screen.
 */
sealed interface Preference : Parcelable

/**
 * A preference that holds a value of type [T].
 */
sealed interface PreferenceSetting<T> : Preference {
    val id: String
    val value: T
    val requiresEditView: Boolean

    @Parcelize
    data class Text(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: String,
    ) : PreferenceSetting<String> {
        @IgnoredOnParcel
        override val requiresEditView: Boolean = true
    }

    @Parcelize
    data class Color(
        override val id: String,
        val title: () -> String,
        val description: () -> String? = { null },
        val icon: () -> ImageVector? = { null },
        override val value: Int,
    ) : PreferenceSetting<Int> {
        @IgnoredOnParcel
        override val requiresEditView: Boolean = true
    }
}
