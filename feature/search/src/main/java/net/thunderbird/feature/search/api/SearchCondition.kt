package net.thunderbird.feature.search.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a search condition describing what to search for in a specific field
 * and how to match it using an attribute.
 *
 * @param field The field to search in (e.g., subject, sender, date)
 * @param attribute The attribute to apply to the field (e.g., contains, equals, starts with)
 * @param value The value to match against the field and attribute
 */
@Parcelize
data class SearchCondition(
    @JvmField
    val field: MessageSearchField,

    @JvmField
    val attribute: SearchAttribute,

    @JvmField
    val value: String?,
) : Parcelable
