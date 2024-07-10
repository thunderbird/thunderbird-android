package com.fsck.k9.contacts

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory

/**
 * [ModelLoader] implementation that does nothing put pass through [ContactImage] to be handled by our custom
 * [ResourceDecoder] implementation, [ContactImageBitmapDecoder].
 */
class ContactImageModelLoader : ModelLoader<ContactImage, ContactImage> {
    override fun buildLoadData(
        contactImage: ContactImage,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<ContactImage> {
        return ModelLoader.LoadData(contactImage, ContactImageDataFetcher(contactImage))
    }

    override fun handles(model: ContactImage) = true
}

class ContactImageDataFetcher(private val contactImage: ContactImage) : DataFetcher<ContactImage> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ContactImage>) {
        callback.onDataReady(contactImage)
    }

    override fun getDataClass() = ContactImage::class.java

    override fun getDataSource() = DataSource.LOCAL

    override fun cleanup() = Unit

    override fun cancel() = Unit
}

class ContactImageModelLoaderFactory : ModelLoaderFactory<ContactImage, ContactImage> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ContactImage, ContactImage> {
        return ContactImageModelLoader()
    }

    override fun teardown() = Unit
}
