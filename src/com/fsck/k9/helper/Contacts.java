package com.fsck.k9.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.content.Intent;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;

/**
 * Helper class to access the contacts stored on the device. This is needed
 * because the original contacts API introduced with SDK 1 was deprecated with
 * SDK 5 and will eventually be removed in newer SDK versions.
 * A class that uses the latest contacts API available on the device will be
 * loaded at runtime.
 *
 * @see ContactsSdk5
 * @see ContactsSdk5p
 */
public abstract class Contacts {
    /**
     * Instance of the SDK specific class that interfaces with the contacts
     * API.
     */
    private static Contacts sInstance = null;

    /**
     * Get SDK specific instance of the Contacts class.
     *
     * @param context A {@link Context} instance.
     * @return Appropriate {@link Contacts} instance for this device.
     */
    public static Contacts getInstance(Context context) {
        if (sInstance == null) {
            /*
             * Check the version of the SDK we are running on. Choose an
             * implementation class designed for that version of the SDK.
             */
            String className = null;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
                /*
                 * The new API was introduced with SDK 5. But Android versions < 2.2
                 * need some additional code to be able to search for phonetic names.
                 */
                className = "com.fsck.k9.helper.ContactsSdk5p";
            } else {
                className = "com.fsck.k9.helper.ContactsSdk5";
            }

            /*
             * Find the required class by name and instantiate it.
             */
            try {
                Class <? extends Contacts > clazz =
                    Class.forName(className).asSubclass(Contacts.class);

                Constructor <? extends Contacts > constructor = clazz.getConstructor(Context.class);
                sInstance = constructor.newInstance(context);
            } catch (ClassNotFoundException e) {
                Log.e(K9.LOG_TAG, "Couldn't find class: " + className, e);
            } catch (InstantiationException e) {
                Log.e(K9.LOG_TAG, "Couldn't instantiate class: " + className, e);
            } catch (IllegalAccessException e) {
                Log.e(K9.LOG_TAG, "Couldn't access class: " + className, e);
            } catch (NoSuchMethodException e) {
                Log.e(K9.LOG_TAG, "Couldn't find constructor of class: " + className, e);
            } catch (IllegalArgumentException e) {
                Log.e(K9.LOG_TAG, "Wrong arguments for constructor of class: " + className, e);
            } catch (InvocationTargetException e) {
                Log.e(K9.LOG_TAG, "Couldn't invoke constructor of class: " + className, e);
            }
        }

        return sInstance;
    }


    protected Context mContext;
    protected ContentResolver mContentResolver;
    protected Boolean mHasContactPicker;

    /**
     * Constructor
     *
     * @param context A {@link Context} instance.
     */
    protected Contacts(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    /**
     * Start the activity to add information to an existing contact or add a
     * new one.
     *
     * @param email An {@link Address} instance containing the email address
     *              of the entity you want to add to the contacts. Optionally
     *              the instance also contains the (display) name of that
     *              entity.
     */
    public abstract void createContact(Address email);

    /**
     * Check whether the provided email address belongs to one of the contacts.
     *
     * @param emailAddress The email address to look for.
     * @return <tt>true</tt>, if the email address belongs to a contact.
     *         <tt>false</tt>, otherwise.
     */
    public abstract boolean isInContacts(String emailAddress);

    /**
     * Filter the contacts matching the given search term.
     *
     * @param filter The search term to filter the contacts.
     * @return A {@link Cursor} instance that can be used to get the
     *         matching contacts.
     */
    public abstract Cursor searchContacts(CharSequence filter);

    /**
     * Get the name of the contact an email address belongs to.
     *
     * @param address The email address to search for.
     * @return The name of the contact the email address belongs to. Or
     *      <tt>null</tt> if there's no matching contact.
     */
    public abstract String getNameForAddress(String address);

    /**
     * Extract the name from a {@link Cursor} instance returned by
     * {@link #searchContacts(CharSequence)}.
     *
     * @param cursor The {@link Cursor} instance.
     * @return The name of the contact in the {@link Cursor}'s current row.
     */
    public abstract String getName(Cursor cursor);

    /**
     * Extract the email address from a {@link Cursor} instance returned by
     * {@link #searchContacts(CharSequence)}.
     *
     * @param cursor The {@link Cursor} instance.
     * @return The email address of the contact in the {@link Cursor}'s current
     *         row.
     */
    public abstract String getEmail(Cursor cursor);

    /**
     * Mark contacts with the provided email addresses as contacted.
     *
     * @param addresses Array of {@link Address} objects describing the
     *        contacts to be marked as contacted.
     */
    public abstract void markAsContacted(final Address[] addresses);

    /**
     * Creates the intent necessary to open a contact picker.
     *
     * @return The intent necessary to open a contact picker.
     */
    public abstract Intent contactPickerIntent();

    /**
     * Given a contact picker intent, returns the primary email address of that
     * contact.
     *
     * @param intent The {@link Intent} returned by this contact picker.
     * @return The primary email address of the picked contact.
     */
    public abstract String getEmailFromContactPicker(final Intent intent);

    /**
     * Does the device actually have a Contacts application suitable for
     * picking a contact. As hard as it is to believe, some vendors ship
     * without it.
     *
     * @return True, if the device supports picking contacts. False, otherwise.
     */
    public boolean hasContactPicker() {
        if (mHasContactPicker == null) {
            mHasContactPicker = !(mContext.getPackageManager().
                                  queryIntentActivities(contactPickerIntent(), 0).isEmpty());
        }
        return mHasContactPicker;
    }
}
