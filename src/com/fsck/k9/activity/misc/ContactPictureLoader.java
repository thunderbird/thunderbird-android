package com.fsck.k9.activity.misc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.widget.QuickContactBadge;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.mail.Address;

public class ContactPictureLoader {
    /**
     * Resize the pictures to the following value (device-independent pixels).
     */
    private static final int PICTURE_SIZE = 40;

    /**
     * Pattern to extract the letter to be displayed as fallback image.
     */
    private static final Pattern EXTRACT_LETTER_PATTERN = Pattern.compile("[a-zA-Z]");

    /**
     * Letter to use when {@link #EXTRACT_LETTER_PATTERN} couldn't find a match.
     */
    private static final String FALLBACK_CONTACT_LETTER = "?";


    private ContentResolver mContentResolver;
    private Resources mResources;
    private Contacts mContactsHelper;
    private int mPictureSizeInPx;

    private int mDefaultBackgroundColor;

    /**
     * LRU cache of contact pictures.
     */
    private final LruCache<Address, Bitmap> mBitmapCache;

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
        mContentResolver = appContext.getContentResolver();
        mResources = appContext.getResources();
        mContactsHelper = Contacts.getInstance(appContext);

        float scale = mResources.getDisplayMetrics().density;
        mPictureSizeInPx = (int) (PICTURE_SIZE * scale);

        mDefaultBackgroundColor = defaultBackgroundColor;

        ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = activityManager.getMemoryClass();

        // Use 1/16th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 16;

        mBitmapCache = new LruCache<Address, Bitmap>(cacheSize) {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            protected int sizeOf(Address key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    return bitmap.getByteCount();
                }

                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    /**
     * Load a contact picture and display it using the supplied {@link QuickContactBadge} instance.
     *
     * <p>
     * If a picture is found in the cache, it is displayed in the {@code QuickContactBadge}
     * immediately. Otherwise a {@link ContactPictureRetrievalTask} is started to try to load the
     * contact picture in a background thread. Depending on the result the contact picture or a
     * fallback picture is then stored in the bitmap cache.
     * </p>
     *
     * @param address
     *         The {@link Address} instance holding the email address that is used to search the
     *         contacts database.
     * @param badge
     *         The {@code QuickContactBadge} instance to receive the picture.
     *
     * @see #mBitmapCache
     * @see #calculateFallbackBitmap(Address)
     */
    public void loadContactPicture(Address address, QuickContactBadge badge) {
        Bitmap bitmap = getBitmapFromCache(address);
        if (bitmap != null) {
            // The picture was found in the bitmap cache
            badge.setImageBitmap(bitmap);
        } else if (cancelPotentialWork(address, badge)) {
            // Query the contacts database in a background thread and try to load the contact
            // picture, if there is one.
            ContactPictureRetrievalTask task = new ContactPictureRetrievalTask(badge, address);
            AsyncDrawable asyncDrawable = new AsyncDrawable(mResources,
                    calculateFallbackBitmap(address), task);
            badge.setImageDrawable(asyncDrawable);
            try {
                task.exec();
            } catch (RejectedExecutionException e) {
                // We flooded the thread pool queue... use a fallback picture
                badge.setImageBitmap(calculateFallbackBitmap(address));
            }
        }
    }

    private int calcUnknownContactColor(Address address) {
        if (mDefaultBackgroundColor != 0) {
            return mDefaultBackgroundColor;
        }

        int val = address.getAddress().toLowerCase(Locale.getDefault()).hashCode();
        int rgb = CONTACT_DUMMY_COLORS_ARGB[Math.abs(val) % CONTACT_DUMMY_COLORS_ARGB.length];
        return rgb;
    }

    private String calcUnknownContactLetter(Address address) {
        String letter = null;
        String personal = address.getPersonal();
        String str = (personal != null) ? personal : address.getAddress();

        Matcher m = EXTRACT_LETTER_PATTERN.matcher(str);
        if (m.find()) {
            letter = m.group(0).toUpperCase(Locale.US);
        }

        return (StringUtils.isNullOrEmpty(letter)) ?
                FALLBACK_CONTACT_LETTER : letter.substring(0, 1);
    }

    /**
     * Calculates a bitmap with a color and a capital letter for contacts without picture.
     */
    private Bitmap calculateFallbackBitmap(Address address) {
        Bitmap result = Bitmap.createBitmap(mPictureSizeInPx, mPictureSizeInPx,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);

        int rgb = calcUnknownContactColor(address);
        result.eraseColor(rgb);

        String letter = calcUnknownContactLetter(address);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setARGB(255, 255, 255, 255);
        paint.setTextSize(mPictureSizeInPx * 3 / 4); // just scale this down a bit
        Rect rect = new Rect();
        paint.getTextBounds(letter, 0, 1, rect);
        float width = paint.measureText(letter);
        canvas.drawText(letter,
                (mPictureSizeInPx / 2f) - (width / 2f),
                (mPictureSizeInPx / 2f) + (rect.height() / 2f), paint);

        return result;
    }

    private void addBitmapToCache(Address key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            mBitmapCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromCache(Address key) {
        return mBitmapCache.get(key);
    }

    /**
     * Checks if a {@code ContactPictureRetrievalTask} was already created to load the contact
     * picture for the supplied {@code Address}.
     *
     * @param address
     *         The {@link Address} instance holding the email address that is used to search the
     *         contacts database.
     * @param badge
     *         The {@code QuickContactBadge} instance that will receive the picture.
     *
     * @return {@code true}, if the contact picture should be loaded in a background thread.
     *         {@code false}, if another {@link ContactPictureRetrievalTask} was already scheduled
     *         to load that contact picture.
     */
    private boolean cancelPotentialWork(Address address, QuickContactBadge badge) {
        final ContactPictureRetrievalTask task = getContactPictureRetrievalTask(badge);

        if (task != null && address != null) {
            if (!address.equals(task.getAddress())) {
                // Cancel previous task
                task.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }

        // No task associated with the QuickContactBadge, or an existing task was cancelled
        return true;
    }

    private ContactPictureRetrievalTask getContactPictureRetrievalTask(QuickContactBadge badge) {
        if (badge != null) {
           Drawable drawable = badge.getDrawable();
           if (drawable instanceof AsyncDrawable) {
               AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
               return asyncDrawable.getContactPictureRetrievalTask();
           }
        }

        return null;
    }


    /**
     * Load a contact picture in a background thread.
     */
    class ContactPictureRetrievalTask extends AsyncTask<Void, Void, Bitmap> {
        private final WeakReference<QuickContactBadge> mQuickContactBadgeReference;
        private final Address mAddress;

        ContactPictureRetrievalTask(QuickContactBadge badge, Address address) {
            mQuickContactBadgeReference = new WeakReference<QuickContactBadge>(badge);
            mAddress = new Address(address);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void exec(Void... args) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
            } else {
                execute(args);
            }
        }

        public Address getAddress() {
            return mAddress;
        }

        @Override
        protected Bitmap doInBackground(Void... args) {
            final String email = mAddress.getAddress();
            final Uri photoUri = mContactsHelper.getPhotoUri(email);
            Bitmap bitmap = null;
            if (photoUri != null) {
                try {
                    InputStream stream = mContentResolver.openInputStream(photoUri);
                    if (stream != null) {
                        try {
                            Bitmap tempBitmap = BitmapFactory.decodeStream(stream);
                            if (tempBitmap != null) {
                                bitmap = Bitmap.createScaledBitmap(tempBitmap, mPictureSizeInPx,
                                        mPictureSizeInPx, true);
                                if (tempBitmap != bitmap) {
                                    tempBitmap.recycle();
                                }
                            }
                        } finally {
                            try { stream.close(); } catch (IOException e) { /* ignore */ }
                        }
                    }
                } catch (FileNotFoundException e) {
                    /* ignore */
                }

            }

            if (bitmap == null) {
                bitmap = calculateFallbackBitmap(mAddress);
            }

            // Save the picture of the contact with that email address in the bitmap cache
            addBitmapToCache(mAddress, bitmap);

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mQuickContactBadgeReference != null) {
                QuickContactBadge badge = mQuickContactBadgeReference.get();
                if (badge != null && getContactPictureRetrievalTask(badge) == this) {
                    badge.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * {@code Drawable} subclass that stores a reference to the {@link ContactPictureRetrievalTask}
     * that is trying to load the contact picture.
     *
     * <p>
     * The reference is used by {@link ContactPictureLoader#cancelPotentialWork(Address,
     * QuickContactBadge)} to find out if the contact picture is already being loaded by a
     * {@code ContactPictureRetrievalTask}.
     * </p>
     */
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ContactPictureRetrievalTask> mAsyncTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, ContactPictureRetrievalTask task) {
            super(res, bitmap);
            mAsyncTaskReference = new WeakReference<ContactPictureRetrievalTask>(task);
        }

        public ContactPictureRetrievalTask getContactPictureRetrievalTask() {
            return mAsyncTaskReference.get();
        }
    }
}
