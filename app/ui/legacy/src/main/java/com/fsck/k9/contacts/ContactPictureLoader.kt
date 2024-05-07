package com.fsck.k9.contacts

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.WorkerThread
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.FutureTarget
import com.fsck.k9.mail.Address
import com.fsck.k9.view.RecipientSelectView.Recipient

class ContactPictureLoader(
    private val context: Context,
    private val contactLetterBitmapCreator: ContactLetterBitmapCreator,
) {
    private val pictureSizeInPx: Int = PICTURE_SIZE.toDip(context)
    private val backgroundCacheId: String = with(contactLetterBitmapCreator.config) {
        if (hasDefaultBackgroundColor) defaultBackgroundColor.toString() else "*"
    }

    fun setContactPicture(imageView: ImageView, address: Address) {
        Glide.with(imageView.context)
            .load(createContactImage(address, contactLetterOnly = false))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .into(imageView)
    }

    fun setContactPicture(imageView: ImageView, recipient: Recipient) {
        val contactPictureUri = recipient.photoThumbnailUri
        if (contactPictureUri != null) {
            setContactPicture(imageView, contactPictureUri)
        } else {
            setFallbackPicture(imageView, recipient.address)
        }
    }

    private fun setContactPicture(imageView: ImageView, contactPictureUri: Uri) {
        Glide.with(imageView.context)
            .load(contactPictureUri)
            .placeholder(Icons.Filled.ContactPicture)
            .error(Icons.Filled.ContactPicture)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .into(imageView)
    }

    private fun setFallbackPicture(imageView: ImageView, address: Address) {
        Glide.with(imageView.context)
            .load(createContactImage(address, contactLetterOnly = true))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .into(imageView)
    }

    @WorkerThread
    fun getContactPicture(recipient: Recipient): Bitmap? {
        val contactPictureUri = recipient.photoThumbnailUri
        val address = recipient.address

        return if (contactPictureUri != null) {
            getContactPicture(contactPictureUri)
        } else {
            getFallbackPicture(address)
        }
    }

    private fun getContactPicture(contactPictureUri: Uri): Bitmap? {
        return Glide.with(context)
            .asBitmap()
            .load(contactPictureUri)
            .error(Icons.Filled.ContactPicture)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .submit(pictureSizeInPx, pictureSizeInPx)
            .getOrNull()
    }

    private fun getFallbackPicture(address: Address): Bitmap? {
        return Glide.with(context)
            .asBitmap()
            .load(createContactImage(address, contactLetterOnly = true))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .dontAnimate()
            .submit(pictureSizeInPx, pictureSizeInPx)
            .getOrNull()
    }

    private fun createContactImage(address: Address, contactLetterOnly: Boolean): ContactImage {
        return ContactImage(
            contactLetterOnly = contactLetterOnly,
            backgroundCacheId = backgroundCacheId,
            contactLetterBitmapCreator = contactLetterBitmapCreator,
            address = address,
        )
    }

    private fun <T> FutureTarget<T>.getOrNull(): T? {
        return try {
            get()
        } catch (e: Exception) {
            null
        }
    }

    private fun Int.toDip(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    companion object {
        /**
         * Resize the pictures to the following value (device-independent pixels).
         */
        private const val PICTURE_SIZE = 40
    }
}
