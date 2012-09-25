package com.fsck.k9.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.content.Intent;
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
        Context appContext = context.getApplicationContext();
        if (sInstance == null) {
            /*
             * Check the version of the SDK we are running on. Choose an
             * implementation class designed for that version of the SDK.
             */
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
                /*
                 * The new API was introduced with SDK 5. But Android versions < 2.2
                 * need some additional code to be able to search for phonetic names.
                 */
                sInstance = new ContactsSdk5p(appContext);
            } else {
                sInstance = new ContactsSdk5(appContext);
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
     * Start the activity to add a phone number to an existing contact or add a new one.
     *
     * @param phoneNumber
     *         The phone number to add to a contact, or to use when creating a new contact.
     */
    public abstract void addPhoneContact(String phoneNumber);

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
     * Given a contact picker intent, returns a {@code ContactItem} instance for that contact.
     *
     * @param intent
     *         The {@link Intent} returned by the contact picker.
     *
     * @return A {@link ContactItem} instance describing the picked contact. Or {@code null} if the
     *         contact doesn't have any email addresses.
     */
    public abstract ContactItem extractInfoFromContactPickerIntent(final Intent intent);

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
