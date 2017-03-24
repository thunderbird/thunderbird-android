package com.fsck.k9.activity.misc;


import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fsck.k9.Account;
import com.fsck.k9.Account.UseGravatar;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.view.RecipientSelectView.Recipient;


public class ContactPictureLoader {
    /**
     * Resize the pictures to the following value (device-independent pixels).
     */
    private static final int PICTURE_SIZE = 40;

    /**
     * Pattern to extract the letter to be displayed as fallback image.
     */
    private static final Pattern EXTRACT_LETTER_PATTERN = Pattern.compile("\\p{L}\\p{M}*");

    /**
     * Letter to use when {@link #EXTRACT_LETTER_PATTERN} couldn't find a match.
     */
    private static final String FALLBACK_CONTACT_LETTER = "?";


    private Resources mResources;
    private Contacts mContactsHelper;
    private int mPictureSizeInPx;

    private int mDefaultBackgroundColor;

    /**
     * @see <a href="http://developer.android.com/design/style/color.html">Color palette used</a>
     */
    private final static int CONTACT_DUMMY_COLORS_ARGB[] = {
        0xff33B5E5,
        0xffAA66CC,
        0xff99CC00,
        0xffFFBB33,
        0xffFF4444,
        0xff0099CC,
        0xff9933CC,
        0xff669900,
        0xffFF8800,
        0xffCC0000
    };

    @VisibleForTesting
    protected static String calcUnknownContactLetter(Address address) {
        String letter = null;
        String personal = address.getPersonal();
        String str = (personal != null) ? personal : address.getAddress();
        Matcher m = EXTRACT_LETTER_PATTERN.matcher(str);
        if (m.find()) {
            letter = m.group(0).toUpperCase(Locale.US);
        }

        return (TextUtils.isEmpty(letter)) ?
                FALLBACK_CONTACT_LETTER : letter;
    }

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
        Context appContext = context.getApplicationContext();
        mResources = appContext.getResources();
        mContactsHelper = Contacts.getInstance(appContext);

        float scale = mResources.getDisplayMetrics().density;
        mPictureSizeInPx = (int) (PICTURE_SIZE * scale);

        mDefaultBackgroundColor = defaultBackgroundColor;

    }

    public void loadContactPicture(Account account, final Address address, final ImageView imageView) {
        Uri photoUri = mContactsHelper.getPhotoUri(address.getAddress());
        loadContactPicture(account, photoUri, address, imageView);
    }

    public void loadContactPicture(Account account, Recipient recipient, ImageView imageView) {
        loadContactPicture(account, recipient.photoThumbnailUri, recipient.address, imageView);
    }

    private void loadFallbackPicture(Account account, final Address address, final ImageView imageView) {
        final Context context = imageView.getContext();

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
                .listener(new RequestListener<FallbackGlideParams, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, FallbackGlideParams model, Target<GlideDrawable> target,
                            boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, FallbackGlideParams model,
                            Target<GlideDrawable> target,
                            boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);

        if (account.getUseGravatar() == UseGravatar.ALWAYS || (account.getUseGravatar() == UseGravatar.ON_WIFI && isOnWiFi(imageView.getContext()))) {
            Glide.with(context)
                    .load(getGravatarUri(address.getAddress()))
                    .listener(new RequestListener<Uri, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, Uri model, Target<GlideDrawable> target,
                                boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, Uri model,
                                Target<GlideDrawable> target,
                                boolean isFromMemoryCache, boolean isFirstResource) {
                            Glide.with(context)
                                    .load(getGravatarUri(address.getAddress()))
                                    .into(imageView);
                            return false;
                        }
                    })
                    .preload(mPictureSizeInPx, mPictureSizeInPx);
        }
    }

    private void loadContactPicture(final Account account, Uri photoUri, final Address address, final ImageView imageView) {
        if (photoUri != null) {
            RequestListener<Uri, GlideDrawable> noPhotoListener = new RequestListener<Uri, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, Uri model, Target<GlideDrawable> target,
                        boolean isFirstResource) {
                    loadFallbackPicture(account, address, imageView);
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
            loadFallbackPicture(account, address, imageView);
        }
    }

    private boolean isOnWiFi(Context context) {
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            return connectivityManager.isActiveNetworkMetered();
        } else {
            NetworkType networkType =
                    NetworkType.fromConnectivityManagerType(connectivityManager.getActiveNetworkInfo().getType());
            return networkType == NetworkType.WIFI;
        }

    }

    private int calcUnknownContactColor(Address address) {
        if (mDefaultBackgroundColor != 0) {
            return mDefaultBackgroundColor;
        }

        int val = address.hashCode();
        int colorIndex = (val & Integer.MAX_VALUE) % CONTACT_DUMMY_COLORS_ARGB.length;
        return CONTACT_DUMMY_COLORS_ARGB[colorIndex];
    }

    private Bitmap drawTextAndBgColorOnBitmap(Bitmap bitmap, FallbackGlideParams params) {
        Canvas canvas = new Canvas(bitmap);

        int rgb = calcUnknownContactColor(params.address);
        bitmap.eraseColor(rgb);

        String letter = calcUnknownContactLetter(params.address);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
        paint.setARGB(255, 255, 255, 255);
        paint.setTextSize(mPictureSizeInPx * 3 / 4); // just scale this down a bit
        Rect rect = new Rect();
        paint.getTextBounds(letter, 0, 1, rect);
        float width = paint.measureText(letter);
        canvas.drawText(letter,
                (mPictureSizeInPx / 2f) - (width / 2f),
                (mPictureSizeInPx / 2f) + (rect.height() / 2f), paint);

        return bitmap;
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
            drawTextAndBgColorOnBitmap(bitmap, source);
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

    private Uri getGravatarUri(String email) {
        return Uri.parse("http://www.gravatar.com/avatar/" + getMD5String(email) + "?d=404");
    }

    private String getMD5String(String from) {
        try {
            final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(from.getBytes(), 0, from.length());
            String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        }
        return null;
    }
}
