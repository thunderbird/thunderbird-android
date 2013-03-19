package com.fsck.k9.activity.misc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.widget.QuickContactBadge;
import com.fsck.k9.helper.Contacts;

public class ContactPictureLoader {
    /**
     * Resize the pictures to the following value (device-independent pixels).
     */
    private static final int PICTURE_SIZE = 40;

    /**
     * Maximum number of email addresses to store in {@link #mUnknownContactsCache}.
     */
    private static final int MAX_UNKNOWN_CONTACTS = 1000;

    /**
     * Used as lightweight dummy value for entries in {@link #mUnknownContactsCache}.
     */
    private static final int[] DUMMY_INT_ARRAY = new int[0];


    private ContentResolver mContentResolver;
    private Resources mResources;
    private Contacts mContactsHelper;
    private Bitmap mDefaultPicture;
    private int mPictureSizeInPx;

    /**
     * LRU cache of contact pictures.
     */
    private final LruCache<String, Bitmap> mBitmapCache;

    /**
     * LRU cache of email addresses that don't belong to a contact we have a picture for.
     *
     * <p>
     * We don't store the default picture for unknown contacts or contacts without a picture in
     * {@link #mBitmapCache}, because that would lead to an unnecessarily complex implementation of
     * the {@code LruCache.sizeOf()} method. Instead, we save the email addresses we know don't
     * belong to one of our contacts with a picture. Knowing this, we can avoid querying the
     * contacts database for those addresses and immediately return the default picture.
     * </p>
     */
    private final LruCache<String, int[]> mUnknownContactsCache;


    public ContactPictureLoader(Context context, int defaultPictureResource) {
        Context appContext = context.getApplicationContext();
        mContentResolver = appContext.getContentResolver();
        mResources = appContext.getResources();
        mContactsHelper = Contacts.getInstance(appContext);
        mDefaultPicture = BitmapFactory.decodeResource(mResources, defaultPictureResource);

        float scale = mResources.getDisplayMetrics().density;
        mPictureSizeInPx = (int) (PICTURE_SIZE * scale);

        ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = activityManager.getMemoryClass();

        // Use 1/16th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 16;

        mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    return bitmap.getByteCount();
                }

                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };

        mUnknownContactsCache = new LruCache<String, int[]>(MAX_UNKNOWN_CONTACTS);
    }

    /**
     * Load a contact picture and display it using the supplied {@link QuickContactBadge} instance.
     *
     * <p>
     * If the supplied email address doesn't belong to any of our contacts, the default picture is
     * returned. If the picture is found in the cache, it is displayed in the
     * {@code QuickContactBadge} immediately. Otherwise a {@link ContactPictureRetrievalTask} is
     * started to try to load the contact picture in a background thread. The picture is then
     * stored in the bitmap cache or the email address is stored in the "unknown contacts cache" if
     * it doesn't belong to one of our contacts.
     * </p>
     *
     * @param email
     *         The email address that is used to search the contacts database.
     * @param badge
     *         The {@code QuickContactBadge} instance to receive the picture.
     *
     * @see #mBitmapCache
     * @see #mUnknownContactsCache
     */
    public void loadContactPicture(String email, QuickContactBadge badge) {
        Bitmap bitmap = getBitmapFromCache(email);
        if (bitmap != null) {
            // The picture was found in the bitmap cache
            badge.setImageBitmap(bitmap);
        } else if (isEmailInUnknownContactsCache(email)) {
            // This email address doesn't belong to a contact we have a picture for. Use the
            // default picture.
            badge.setImageBitmap(mDefaultPicture);
        } else if (cancelPotentialWork(email, badge)) {
            // Query the contacts database in a background thread and try to load the contact
            // picture, if there is one.
            ContactPictureRetrievalTask task = new ContactPictureRetrievalTask(badge);
            AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, mDefaultPicture, task);
            badge.setImageDrawable(asyncDrawable);
            try {
                task.exec(email);
            } catch (RejectedExecutionException e) {
                // We flooded the thread pool queue... fall back to using the default picture
                badge.setImageBitmap(mDefaultPicture);
            }
        }
    }

    private void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            mBitmapCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        return mBitmapCache.get(key);
    }

    private void addEmailToUnknownContactsCache(String key) {
        if (!isEmailInUnknownContactsCache(key)) {
            mUnknownContactsCache.put(key, DUMMY_INT_ARRAY);
        }
    }

    private boolean isEmailInUnknownContactsCache(String key) {
        return mUnknownContactsCache.get(key) != null;
    }

    /**
     * Checks if a {@code ContactPictureRetrievalTask} was already created to load the contact
     * picture for the supplied email address.
     *
     * @param email
     *         The email address to check the contacts database for.
     * @param badge
     *         The {@code QuickContactBadge} instance that will receive the picture.
     *
     * @return {@code true}, if the contact picture should be loaded in a background thread.
     *         {@code false}, if another {@link ContactPictureRetrievalTask} was already scheduled
     *         to load that contact picture.
     */
    private boolean cancelPotentialWork(String email, QuickContactBadge badge) {
        final ContactPictureRetrievalTask task = getContactPictureRetrievalTask(badge);

        if (task != null && email != null) {
            String emailFromTask = task.getEmail();
            if (!email.equals(emailFromTask)) {
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
    class ContactPictureRetrievalTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<QuickContactBadge> mQuickContactBadgeReference;
        private String mEmail;

        ContactPictureRetrievalTask(QuickContactBadge badge) {
            mQuickContactBadgeReference = new WeakReference<QuickContactBadge>(badge);
        }

        public void exec(String... args) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
            } else {
                execute(args);
            }
        }

        public String getEmail() {
            return mEmail;
        }

        @Override
        protected Bitmap doInBackground(String... args) {
            String email = args[0];
            mEmail = email;
            final Uri x = mContactsHelper.getPhotoUri(email);
            Bitmap bitmap = null;
            if (x != null) {
                try {
                    InputStream stream = mContentResolver.openInputStream(x);
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
                bitmap = mDefaultPicture;

                // Remember that we don't have a contact picture for this email address
                addEmailToUnknownContactsCache(email);
            } else {
                // Save the picture of the contact with that email address in the memory cache
                addBitmapToCache(email, bitmap);
            }

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
     * The reference is used by {@link ContactPictureLoader#cancelPotentialWork(String,
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
