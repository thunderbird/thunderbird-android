package com.fsck.k9.contacts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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

        val backgroundColor = calcUnknownContactColor(address)
        bitmap.eraseColor(backgroundColor)

        val letter = letterExtractor.extractContactLetter(address)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            setARGB(255, 255, 255, 255)
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

    private fun calcUnknownContactColor(address: Address): Int {
        if (config.hasDefaultBackgroundColor) {
            return config.defaultBackgroundColor
        }

        val hash = address.hashCode()
        val colorIndex = (hash and Integer.MAX_VALUE) % BACKGROUND_COLORS.size
        return BACKGROUND_COLORS[colorIndex]
    }

    companion object {
        private val BACKGROUND_COLORS = intArrayOf(
                0xff33B5E5L.toInt(),
                0xffAA66CCL.toInt(),
                0xff99CC00L.toInt(),
                0xffFFBB33L.toInt(),
                0xffFF4444L.toInt(),
                0xff0099CCL.toInt(),
                0xff9933CCL.toInt(),
                0xff669900L.toInt(),
                0xffFF8800L.toInt(),
                0xffCC0000L.toInt()
        )
    }
}
