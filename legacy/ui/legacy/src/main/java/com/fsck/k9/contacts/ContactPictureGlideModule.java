package com.fsck.k9.contacts;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.LibraryGlideModule;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.ui.account.AccountImage;
import com.fsck.k9.ui.account.AccountImageModelLoaderFactory;
import org.jetbrains.annotations.NotNull;

@GlideModule
public class ContactPictureGlideModule extends LibraryGlideModule {
    @Override
    public void registerComponents(@NotNull Context context, @NotNull Glide glide, @NotNull Registry registry) {
        registerContactImage(glide, registry);
        registerAccountImage(registry);
    }

    private void registerContactImage(@NotNull Glide glide, @NotNull Registry registry) {
        registry.append(ContactImage.class, ContactImage.class, new ContactImageModelLoaderFactory());

        ContactImageBitmapDecoderFactory factory = DI.get(ContactImageBitmapDecoderFactory.class);
        ContactImageBitmapDecoder contactImageBitmapDecoder = factory.create(glide.getBitmapPool());
        registry.append(ContactImage.class, Bitmap.class, contactImageBitmapDecoder);
    }

    private void registerAccountImage(@NotNull Registry registry) {
        AccountImageModelLoaderFactory factory = DI.get(AccountImageModelLoaderFactory.class);
        registry.append(AccountImage.class, Bitmap.class, factory);
    }
}
