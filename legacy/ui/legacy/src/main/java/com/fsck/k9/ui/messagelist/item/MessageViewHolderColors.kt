package com.fsck.k9.ui.messagelist.item

import android.content.res.Resources.Theme
import androidx.annotation.ColorInt
import com.fsck.k9.ui.resolveColorAttribute
import com.google.android.material.R as MaterialR

data class MessageViewHolderColors(
    @get:ColorInt
    val active: Int,
    @get:ColorInt
    val activeBackground: Int,
    @get:ColorInt
    val selected: Int,
    @get:ColorInt
    val selectedBackground: Int,
    @get:ColorInt
    val regular: Int,
    @get:ColorInt
    val regularBackground: Int,
    @get:ColorInt
    val read: Int,
    @get:ColorInt
    val readBackground: Int,
    @get:ColorInt
    val unread: Int,
    @get:ColorInt
    val unreadBackground: Int,
    @get:ColorInt
    val previewText: Int,
    @get:ColorInt
    val previewActiveText: Int,
    @get:ColorInt
    val previewSelectedText: Int,
) {
    companion object Companion {
        fun resolveColors(theme: Theme): MessageViewHolderColors {
            return MessageViewHolderColors(
                active = theme.resolveColorAttribute(MaterialR.attr.colorOnSecondaryContainer),
                activeBackground = theme.resolveColorAttribute(MaterialR.attr.colorSecondaryContainer),
                selected = theme.resolveColorAttribute(MaterialR.attr.colorOnSurfaceVariant),
                selectedBackground = theme.resolveColorAttribute(MaterialR.attr.colorSurfaceVariant),
                regular = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
                regularBackground = theme.resolveColorAttribute(MaterialR.attr.colorSurface),
                read = theme.resolveColorAttribute(MaterialR.attr.colorOutline),
                readBackground = theme.resolveColorAttribute(MaterialR.attr.colorSurfaceContainerHigh),
                unread = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
                unreadBackground = theme.resolveColorAttribute(MaterialR.attr.colorSurface),
                previewText = theme.resolveColorAttribute(MaterialR.attr.colorOutline),
                previewActiveText = theme.resolveColorAttribute(MaterialR.attr.colorOnSecondary),
                previewSelectedText = theme.resolveColorAttribute(MaterialR.attr.colorOnSurfaceVariant),
            )
        }
    }
}
