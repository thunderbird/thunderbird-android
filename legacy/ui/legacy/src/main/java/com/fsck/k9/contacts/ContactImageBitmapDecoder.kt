package com.fsck.k9.contacts

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import kotlin.math.max

/**
 * [ResourceDecoder] implementation that takes a [ContactImage] and fetches the corresponding contact photo using
 * [ContactPhotoLoader] or generates a fallback image using [ContactLetterBitmapCreator].
 */
internal class ContactImageBitmapDecoder(
    private val contactPhotoLoader: ContactPhotoLoader,
    private val bitmapPool: BitmapPool,
) : ResourceDecoder<ContactImage, Bitmap> {

    override fun decode(contactImage: ContactImage, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val size = max(width, height)

        val bitmap = loadContactPhoto(contactImage) ?: createContactLetterBitmap(contactImage, size)

        return BitmapResource.obtain(bitmap, bitmapPool)
    }

    private fun loadContactPhoto(contactImage: ContactImage): Bitmap? {
        if (contactImage.contactLetterOnly) return null

        return contactPhotoLoader.loadContactPhoto(contactImage.address.address)
    }

    private fun createContactLetterBitmap(contactImage: ContactImage, size: Int): Bitmap {
        val bitmap = bitmapPool.getDirty(size, size, Bitmap.Config.ARGB_8888)
        return contactImage.contactLetterBitmapCreator.drawBitmap(bitmap, size, contactImage.address)
    }

    override fun handles(source: ContactImage, options: Options) = true
}

internal class ContactImageBitmapDecoderFactory(private val contactPhotoLoader: ContactPhotoLoader) {
    fun create(bitmapPool: BitmapPool): ContactImageBitmapDecoder {
        return ContactImageBitmapDecoder(contactPhotoLoader, bitmapPool)
    }
}
