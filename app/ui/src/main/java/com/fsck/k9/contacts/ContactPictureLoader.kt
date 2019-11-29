package com.fsck.k9.contacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.request.FutureTarget
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import com.fsck.k9.view.RecipientSelectView.Recipient
import kotlin.math.max
import timber.log.Timber

class ContactPictureLoader(
    private val context: Context,
    private val contactLetterBitmapCreator: ContactLetterBitmapCreator
) {
    private val contactsHelper: Contacts = Contacts.getInstance(context)
    private val pictureSizeInPx: Int = PICTURE_SIZE.toDip(context)
    private val backgroundCacheId: String = with(contactLetterBitmapCreator.config) {
        if (hasDefaultBackgroundColor) defaultBackgroundColor.toString() else "*"
    }

    fun setContactPicture(imageView: ImageView, address: Address) {
        Glide.with(imageView.context)
                .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                .from(Address::class.java)
                .`as`(Bitmap::class.java)
                .decoder(ContactImageBitmapDecoder())
                .signature(contactLetterBitmapCreator.signatureOf(address))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(address)
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
                .error(R.drawable.ic_contact_picture)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .into(imageView)
    }

    private fun setFallbackPicture(imageView: ImageView, address: Address) {
        Glide.with(imageView.context)
                .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                .from(Address::class.java)
                .`as`(Bitmap::class.java)
                .decoder(ContactImageBitmapDecoder(contactLetterOnly = true))
                .signature(contactLetterBitmapCreator.signatureOf(address))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(address)
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
                .load(contactPictureUri)
                .asBitmap()
                .error(R.drawable.ic_contact_picture)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .into(pictureSizeInPx, pictureSizeInPx)
                .getOrNull()
    }

    private fun getFallbackPicture(address: Address): Bitmap? {
        return Glide.with(context)
                .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                .from(Address::class.java)
                .`as`(Bitmap::class.java)
                .decoder(ContactImageBitmapDecoder(contactLetterOnly = true))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(address)
                .dontAnimate()
                .into(pictureSizeInPx, pictureSizeInPx)
                .getOrNull()
    }

    private inner class ContactImageBitmapDecoder(
        private val contactLetterOnly: Boolean = false
    ) : ResourceDecoder<Address, Bitmap> {

        override fun decode(address: Address, width: Int, height: Int): Resource<Bitmap> {
            val pool = Glide.get(context).bitmapPool

            val size = max(width, height)

            val bitmap = loadContactPicture(address) ?: createContactLetterBitmap(address, size, pool)

            return BitmapResource.obtain(bitmap, pool)
        }

        private fun loadContactPicture(address: Address): Bitmap? {
            if (contactLetterOnly) return null

            val photoUri = contactsHelper.getPhotoUri(address.address) ?: return null
            return try {
                context.contentResolver.openInputStream(photoUri).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Timber.e(e, "Couldn't load contact picture: $photoUri")
                null
            }
        }

        private fun createContactLetterBitmap(address: Address, size: Int, pool: BitmapPool): Bitmap {
            val bitmap = pool.getDirty(size, size, Bitmap.Config.ARGB_8888)
                ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            return contactLetterBitmapCreator.drawBitmap(bitmap, size, address)
        }

        override fun getId(): String {
            return "fallback-photo"
        }
    }

    private class AddressModelLoader(val backgroundCacheId: String) : ModelLoader<Address, Address> {
        override fun getResourceFetcher(address: Address, width: Int, height: Int): DataFetcher<Address> {
            return object : DataFetcher<Address> {
                override fun getId() = "${address.address}-${address.personal}-$backgroundCacheId"

                override fun loadData(priority: Priority?): Address = address

                override fun cleanup() = Unit
                override fun cancel() = Unit
            }
        }
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
