package com.fsck.k9.contacts

import android.graphics.*

import com.fsck.k9.mail.Address

/**
 * Draw a `Bitmap` containing the "contact letter" obtained by [ContactLetterExtractor].
 */
class ContactLetterBitmapCreator(
        private val letterExtractor: ContactLetterExtractor,
        val config: ContactLetterBitmapConfig
) {
    fun drawBitmap(bitmap: Bitmap, pictureSizeInPx: Int, address: Address): Bitmap {
        val canvas = Canvas(bitmap)

        val backgroundColor = calcUnknownContactBackgroundColor(address)
        bitmap.eraseColor(backgroundColor)

        val foregroundColor = calcUnknownContactForegroundColor(address)

        val letter = letterExtractor.extractContactLetter(address)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = foregroundColor
            textSize = (pictureSizeInPx * 3 / 4).toFloat() // just scale this down a bit
        }

        val rect = Rect()
        paint.getTextBounds(letter, 0, 1, rect)

        val width = paint.measureText(letter)
        canvas.drawText(letter,
                pictureSizeInPx / 2f - width / 2f,
                pictureSizeInPx / 2f + rect.height() / 2f, paint)

        return bitmap
    }

    private fun calcUnknownContactBackgroundColor(address: Address): Int {
        if (config.hasDefaultColor) {
            return config.defaultBackgroundColor
        }

        val hash = address.hashCode()
        val colorIndex = (hash and Integer.MAX_VALUE) % BACKGROUND_COLORS.size
        return BACKGROUND_COLORS[colorIndex]
    }

    private fun calcUnknownContactForegroundColor(address: Address): Int {
        if (config.hasDefaultColor) {
           return config.defaultForegroundColor
        }

        val hash = address.hashCode()
        val colorIndex = (hash and Integer.MAX_VALUE) % BACKGROUND_COLORS.size
        return FOREGROUND_COLORS[colorIndex]
    }

    companion object {
        private val BACKGROUND_COLORS = intArrayOf(
                0xffb3e5fcL.toInt(),
                0xffc8e6c9L.toInt(),
                0xffd1c4e9L.toInt(),
                0xfffdefbaL.toInt(),
                0xffffccbcL.toInt()
        )
        private val FOREGROUND_COLORS = intArrayOf(
                0xff86acd7L.toInt(),
                0xff9eb7a3L.toInt(),
                0xffa191cfL.toInt(),
                0xffecbe8cL.toInt(),
                0xffcb9f92L.toInt()
        )

    }
}
