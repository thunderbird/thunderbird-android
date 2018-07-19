package com.fsck.k9.activity.misc;


import java.io.IOException;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fsck.k9.contacts.ContactLetterBitmapCreator;
import com.fsck.k9.contacts.ContactLetterExtractor;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;


public class ContactPictureLoader {
    /**
     * Resize the pictures to the following value (device-independent pixels).
     */
    private static final int PICTURE_SIZE = 40;


    private final Context context;
    private final ContactLetterBitmapCreator contactLetterBitmapCreator;
    private Contacts mContactsHelper;
    private int mPictureSizeInPx;


    /**
     * Constructor.
     *
     * @param context
     *         A {@link Context} instance.
     * @param defaultBackgroundColor
     *         The ARGB value to be used as background color for the fallback picture. {@code 0} to
     *         use a dynamically calculated background color.
     */
    public ContactPictureLoader(Context context, int defaultBackgroundColor) {
        this.context = context.getApplicationContext();
        mContactsHelper = Contacts.getInstance(this.context);

        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;
        mPictureSizeInPx = (int) (PICTURE_SIZE * scale);

        ContactLetterExtractor contactLetterExtractor = new ContactLetterExtractor();
        contactLetterBitmapCreator = new ContactLetterBitmapCreator(contactLetterExtractor, defaultBackgroundColor);
    }

    public void loadContactPicture(final Address address, final ImageView imageView) {
        Uri photoUri = mContactsHelper.getPhotoUri(address.getAddress());
        loadContactPicture(photoUri, address, imageView);
    }

    public void loadContactPicture(Recipient recipient, ImageView imageView) {
        loadContactPicture(recipient.photoThumbnailUri, recipient.address, imageView);
    }

    private void loadFallbackPicture(Address address, ImageView imageView) {
        Context context = imageView.getContext();

        Glide.with(context)
                .using(new FallbackGlideModelLoader(), FallbackGlideParams.class)
                .from(FallbackGlideParams.class)
                .as(Bitmap.class)
                .transcode(new BitmapToGlideDrawableTranscoder(context), GlideDrawable.class)
                .decoder(new FallbackGlideBitmapDecoder(context))
                .encoder(new BitmapEncoder(Bitmap.CompressFormat.PNG, 0))
                .cacheDecoder(new FileToStreamDecoder<>(new StreamBitmapDecoder(context)))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(new FallbackGlideParams(address))
                // for some reason, following 2 lines fix loading issues.
                .dontAnimate()
                .override(mPictureSizeInPx, mPictureSizeInPx)
                .into(imageView);
    }

    private void loadContactPicture(Uri photoUri, final Address address, final ImageView imageView) {
        if (photoUri != null) {
            RequestListener<Uri, GlideDrawable> noPhotoListener = new RequestListener<Uri, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, Uri model, Target<GlideDrawable> target,
                        boolean isFirstResource) {
                    loadFallbackPicture(address, imageView);
                    return true;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, Uri model,
                        Target<GlideDrawable> target,
                        boolean isFromMemoryCache, boolean isFirstResource) {
                    return false;
                }
            };

            Glide.with(imageView.getContext())
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .listener(noPhotoListener)
                    // for some reason, following 2 lines fix loading issues.
                    .dontAnimate()
                    .override(mPictureSizeInPx, mPictureSizeInPx)
                    .into(imageView);
        } else {
            loadFallbackPicture(address, imageView);
        }
    }

    public Bitmap loadContactPictureIcon(Recipient recipient) {
        return loadContactPicture(recipient.photoThumbnailUri, recipient.address);
    }

    @WorkerThread
    private Bitmap loadContactPicture(Uri photoUri, Address address) {
        FutureTarget<Bitmap> bitmapTarget;
        if (photoUri != null) {
            bitmapTarget = Glide.with(context)
                    .load(photoUri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .into(mPictureSizeInPx, mPictureSizeInPx);
        } else {
            bitmapTarget = Glide.with(context)
                    .using(new FallbackGlideModelLoader(), FallbackGlideParams.class)
                    .from(FallbackGlideParams.class)
                    .as(Bitmap.class)
                    .decoder(new FallbackGlideBitmapDecoder(context))
                    .encoder(new BitmapEncoder(CompressFormat.PNG, 0))
                    .cacheDecoder(new FileToStreamDecoder<>(new StreamBitmapDecoder(context)))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .load(new FallbackGlideParams(address))
                    .dontAnimate()
                    .into(mPictureSizeInPx, mPictureSizeInPx);
        }

        return loadIgnoringErors(bitmapTarget);
    }

    private class FallbackGlideBitmapDecoder implements ResourceDecoder<FallbackGlideParams, Bitmap> {
        private final Context context;

        FallbackGlideBitmapDecoder(Context context) {
            this.context = context;
        }

        @Override
        public Resource<Bitmap> decode(FallbackGlideParams source, int width, int height) throws IOException {
            BitmapPool pool = Glide.get(context).getBitmapPool();
            Bitmap bitmap = pool.getDirty(mPictureSizeInPx, mPictureSizeInPx, Bitmap.Config.ARGB_8888);
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(mPictureSizeInPx, mPictureSizeInPx, Bitmap.Config.ARGB_8888);
            }

            Address address = source.address;
            contactLetterBitmapCreator.drawBitmap(bitmap, mPictureSizeInPx, address);
            return BitmapResource.obtain(bitmap, pool);
        }

        @Override
        public String getId() {
            return "fallback-photo";
        }
    }

    private class FallbackGlideParams {
        final Address address;

        FallbackGlideParams(Address address) {
            this.address = address;
        }

        public String getId() {
            return String.format(Locale.ROOT, "%s-%s", address.getAddress(), address.getPersonal());
        }
    }

    private class FallbackGlideModelLoader implements ModelLoader<FallbackGlideParams, FallbackGlideParams> {
        @Override
        public DataFetcher<FallbackGlideParams> getResourceFetcher(final FallbackGlideParams model, int width,
                int height) {

            return new DataFetcher<FallbackGlideParams>() {

                @Override
                public FallbackGlideParams loadData(Priority priority) throws Exception {
                    return model;
                }

                @Override
                public void cleanup() {

                }

                @Override
                public String getId() {
                    return model.getId();
                }

                @Override
                public void cancel() {

                }
            };
        }
    }

    @WorkerThread
    @Nullable
    private <T> T loadIgnoringErors(FutureTarget<T> target) {
        try {
            return target.get();
        } catch (Exception e) {
            return null;
        }
    }

}
