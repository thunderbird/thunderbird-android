package com.fsck.k9.contacts


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.support.annotation.WorkerThread
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.load.resource.file.FileToStreamDecoder
import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Address
import com.fsck.k9.view.RecipientSelectView.Recipient


class ContactPictureLoader(
        private val context: Context,
        private val contactLetterBitmapCreator: ContactLetterBitmapCreator
) {
    private val contactsHelper: Contacts = Contacts.getInstance(context)
    private val pictureSizeInPx: Int = PICTURE_SIZE.toDip(context)
    private val backgroundCacheId: String = with(contactLetterBitmapCreator.config) {
        if (hasDefaultBackgroundColor) defaultBackgroundColor.toString() else "*"
    }


    fun loadContactPicture(address: Address, imageView: ImageView) {
        val photoUri = contactsHelper.getPhotoUri(address.address)
        loadContactPicture(photoUri, address, imageView)
    }

    fun loadContactPicture(recipient: Recipient, imageView: ImageView) {
        loadContactPicture(recipient.photoThumbnailUri, recipient.address, imageView)
    }

    private fun loadFallbackPicture(address: Address, imageView: ImageView) {
        val context = imageView.context

        Glide.with(context)
                .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                .from(Address::class.java)
                .`as`(Bitmap::class.java)
                .transcode(BitmapToGlideDrawableTranscoder(context), GlideDrawable::class.java)
                .decoder(ContactLetterBitmapDecoder())
                .encoder(BitmapEncoder(Bitmap.CompressFormat.PNG, 0))
                .cacheDecoder(FileToStreamDecoder(StreamBitmapDecoder(context)))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(address)
                // for some reason, following 2 lines fix loading issues.
                .dontAnimate()
                .override(pictureSizeInPx, pictureSizeInPx)
                .into(imageView)
    }

    private fun loadContactPicture(photoUri: Uri?, address: Address, imageView: ImageView) {
        if (photoUri != null) {
            val noPhotoListener = object : RequestListener<Uri, GlideDrawable> {
                override fun onException(
                        e: Exception,
                        model: Uri,
                        target: Target<GlideDrawable>,
                        isFirstResource: Boolean
                ): Boolean {
                    loadFallbackPicture(address, imageView)
                    return true
                }

                override fun onResourceReady(
                        resource: GlideDrawable,
                        model: Uri,
                        target: Target<GlideDrawable>,
                        isFromMemoryCache: Boolean,
                        isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            }

            Glide.with(imageView.context)
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .listener(noPhotoListener)
                    // for some reason, following 2 lines fix loading issues.
                    .dontAnimate()
                    .override(pictureSizeInPx, pictureSizeInPx)
                    .into(imageView)
        } else {
            loadFallbackPicture(address, imageView)
        }
    }

    fun loadContactPictureIcon(recipient: Recipient): Bitmap? {
        return loadContactPicture(recipient.photoThumbnailUri, recipient.address)
    }

    @WorkerThread
    private fun loadContactPicture(photoUri: Uri?, address: Address): Bitmap? {
        val bitmapTarget: FutureTarget<Bitmap>
        if (photoUri != null) {
            bitmapTarget = Glide.with(context)
                    .load(photoUri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .into(pictureSizeInPx, pictureSizeInPx)
        } else {
            bitmapTarget = Glide.with(context)
                    .using(AddressModelLoader(backgroundCacheId), Address::class.java)
                    .from(Address::class.java)
                    .`as`(Bitmap::class.java)
                    .decoder(ContactLetterBitmapDecoder())
                    .encoder(BitmapEncoder(CompressFormat.PNG, 0))
                    .cacheDecoder(FileToStreamDecoder(StreamBitmapDecoder(context)))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .load(address)
                    .dontAnimate()
                    .into(pictureSizeInPx, pictureSizeInPx)
        }

        return loadIgnoringErrors(bitmapTarget)
    }

    private inner class ContactLetterBitmapDecoder : ResourceDecoder<Address, Bitmap> {
        override fun decode(address: Address, width: Int, height: Int): Resource<Bitmap> {
            val pool = Glide.get(context).bitmapPool
            val bitmap: Bitmap =
                    pool.getDirty(pictureSizeInPx, pictureSizeInPx, Bitmap.Config.ARGB_8888) ?:
                    Bitmap.createBitmap(pictureSizeInPx, pictureSizeInPx, Bitmap.Config.ARGB_8888)

            contactLetterBitmapCreator.drawBitmap(bitmap, pictureSizeInPx, address)

            return BitmapResource.obtain(bitmap, pool)
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

    @WorkerThread
    private fun <T> loadIgnoringErrors(target: FutureTarget<T>): T? {
        return try {
            target.get()
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
