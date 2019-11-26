package com.fsck.k9.contacts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.bumptech.glide.load.Key
import com.bumptech.glide.signature.StringSignature
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.helper.MaterialColors

/**
 * Draw a `Bitmap` containing the "contact letter" obtained by [ContactLetterExtractor].
 */
class ContactLetterBitmapCreator(
        private val letterExtractor: ContactLetterExtractor,
        val config: ContactLetterBitmapConfig
) {
    fun drawBitmap(bitmap: Bitmap, pictureSizeInPx: Int, address: Address): Bitmap {
        val canvas = Canvas(bitmap)

        val backgroundColor = calcUnknownContactColor(address)
        bitmap.eraseColor(backgroundColor)

        val letter = letterExtractor.extractContactLetter(address)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            setARGB(255, 255, 255, 255)
            textSize = pictureSizeInPx.toFloat() * 0.65f
        }

        val rect = Rect()
        paint.getTextBounds(letter, 0, 1, rect)

        val width = paint.measureText(letter)
        canvas.drawText(letter,
                pictureSizeInPx / 2f - width / 2f,
                pictureSizeInPx / 2f + rect.height() / 2f, paint)

        return bitmap
    }

    private fun calcUnknownContactColor(address: Address): Int {
        if (config.hasDefaultBackgroundColor) {
            return config.defaultBackgroundColor
        }

        val hash = address.hashCode()
        if (config.useDarkTheme) {
            val colorIndex = (hash and Integer.MAX_VALUE) % BACKGROUND_COLORS_DARK.size
            return BACKGROUND_COLORS_DARK[colorIndex]
        } else {
            val colorIndex = (hash and Integer.MAX_VALUE) % BACKGROUND_COLORS_LIGHT.size
            return BACKGROUND_COLORS_LIGHT[colorIndex]
        }
    }

    fun signatureOf(address: Address): Key {
        val letter = letterExtractor.extractContactLetter(address)
        val backgroundColor = calcUnknownContactColor(address)
        return StringSignature(letter + backgroundColor)
    }

    companion object {
        private val BACKGROUND_COLORS_LIGHT = intArrayOf(
                MaterialColors.RED_300,
                MaterialColors.DEEP_PURPLE_300,
                MaterialColors.LIGHT_BLUE_300,
                MaterialColors.GREEN_300,
                MaterialColors.DEEP_ORANGE_300,
                MaterialColors.BLUE_GREY_300,
                MaterialColors.PINK_300,
                MaterialColors.INDIGO_300,
                MaterialColors.CYAN_300,
                MaterialColors.AMBER_400,
                MaterialColors.BROWN_300,
                MaterialColors.PURPLE_300,
                MaterialColors.BLUE_300,
                MaterialColors.TEAL_300,
                MaterialColors.ORANGE_400)

        private val BACKGROUND_COLORS_DARK = intArrayOf(
                MaterialColors.RED_600,
                MaterialColors.DEEP_PURPLE_600,
                MaterialColors.LIGHT_BLUE_600,
                MaterialColors.GREEN_600,
                MaterialColors.DEEP_ORANGE_600,
                MaterialColors.BLUE_GREY_600,
                MaterialColors.PINK_600,
                MaterialColors.INDIGO_600,
                MaterialColors.CYAN_600,
                MaterialColors.AMBER_600,
                MaterialColors.BROWN_600,
                MaterialColors.PURPLE_600,
                MaterialColors.BLUE_600,
                MaterialColors.TEAL_600,
                MaterialColors.ORANGE_600)
    }
}
