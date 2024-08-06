package com.fsck.k9.ui.account

import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import app.k9mail.legacy.ui.account.AccountFallbackImageProvider
import app.k9mail.legacy.ui.account.AccountImage
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.fsck.k9.contacts.ContactPhotoLoader

/**
 * A custom [ModelLoader] so we can use [AccountImageDataFetcher] to load the account image.
 */
internal class AccountImageModelLoader(
    private val contactPhotoLoader: ContactPhotoLoader,
    private val accountFallbackImageProvider: AccountFallbackImageProvider,
) : ModelLoader<AccountImage, Bitmap> {
    override fun buildLoadData(
        accountImage: AccountImage,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<Bitmap> {
        val dataFetcher = AccountImageDataFetcher(
            contactPhotoLoader,
            accountFallbackImageProvider,
            accountImage,
        )
        return ModelLoader.LoadData(accountImage, dataFetcher)
    }

    override fun handles(model: AccountImage) = true
}

/**
 * Load an account image.
 *
 * Uses [ContactPhotoLoader] to try to load the user's contact photo (using the account's email address). If there's no
 * such contact or it doesn't have a picture use the fallback image provided by [AccountFallbackImageProvider].
 *
 * We're not using Glide's own fallback mechanism because negative responses aren't cached and the next time the
 * account image is requested another attempt will be made to load the contact photo.
 */
internal class AccountImageDataFetcher(
    private val contactPhotoLoader: ContactPhotoLoader,
    private val accountFallbackImageProvider: AccountFallbackImageProvider,
    private val accountImage: AccountImage,
) : DataFetcher<Bitmap> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        val bitmap = loadAccountImage() ?: createFallbackBitmap()
        callback.onDataReady(bitmap)
    }

    private fun loadAccountImage(): Bitmap? {
        return contactPhotoLoader.loadContactPhoto(accountImage.email)
    }

    private fun createFallbackBitmap(): Bitmap {
        return accountFallbackImageProvider.getDrawable(accountImage.color).toBitmap()
    }

    override fun getDataClass() = Bitmap::class.java

    override fun getDataSource() = DataSource.LOCAL

    override fun cleanup() = Unit

    override fun cancel() = Unit
}

internal class AccountImageModelLoaderFactory(
    private val contactPhotoLoader: ContactPhotoLoader,
    private val accountFallbackImageProvider: AccountFallbackImageProvider,
) : ModelLoaderFactory<AccountImage, Bitmap> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AccountImage, Bitmap> {
        return AccountImageModelLoader(contactPhotoLoader, accountFallbackImageProvider)
    }

    override fun teardown() = Unit
}
