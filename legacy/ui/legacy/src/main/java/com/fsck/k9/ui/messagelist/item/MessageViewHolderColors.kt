package com.fsck.k9.ui.messagelist.item

import android.content.res.Resources.Theme
import androidx.annotation.ColorInt
import com.fsck.k9.ui.resolveColorAttribute
import com.google.android.material.R as MaterialR

data class MessageViewHolderColors(
    @ColorInt
    val active: Int,
    @ColorInt
    val activeBackground: Int,
    @ColorInt
    val selected: Int,
    @ColorInt
    val selectedBackground: Int,
    @ColorInt
    val regular: Int,
    @ColorInt
    val regularBackground: Int,
    @ColorInt
    val read: Int,
    @ColorInt
    val readBackground: Int,
    @ColorInt
    val unread: Int,
    @ColorInt
    val unreadBackground: Int,
    @ColorInt
    val previewText: Int,
    @ColorInt
    val previewActiveText: Int,
    @ColorInt
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
