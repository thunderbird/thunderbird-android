package com.fsck.k9.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.CommonDataKinds.Email;
import com.fsck.k9.mail.Address;

/**
 * Access the contacts on the device using the API introduced with SDK 5.
 *
 * @see android.provider.ContactsContract
 */
public class ContactsSdk5 extends com.fsck.k9.helper.Contacts
{
    /**
     * The order in which the search results are returned by
     * {@link #searchContacts(CharSequence)}.
     */
    private static final String SORT_ORDER =
        Contacts.TIMES_CONTACTED + " DESC, " +
        Contacts.DISPLAY_NAME;

    /**
     * Array of columns to load from the database.
     *
     * Important: The _ID field is needed by
     * {@link com.fsck.k9.EmailAddressAdapter} or more specificly by
     * {@link android.widget.ResourceCursorAdapter}.
     */
    private static final String PROJECTION[] =
    {
        Contacts._ID,
        Contacts.DISPLAY_NAME,
        Email.DATA
    };

    /**
     * Index of the name field in the projection. This must match the order in
     * {@link #PROJECTION}.
     */
    private static final int NAME_INDEX = 1;

    /**
     * Index of the email address field in the projection. This must match the
     * order in {@link #PROJECTION}.
     */
    private static final int EMAIL_INDEX = 2;


    public ContactsSdk5(final Context context)
    {
        super(context);
    }

    @Override
    public void createContact(final Activity activity, final Address email)
    {
        final Uri contactUri = Uri.fromParts("mailto", email.getAddress(), null);

        final Intent contactIntent = new Intent(Intents.SHOW_OR_CREATE_CONTACT);
        contactIntent.setData(contactUri);

        // Pass along full E-mail string for possible create dialog
        contactIntent.putExtra(Intents.EXTRA_CREATE_DESCRIPTION,
                               email.toString());

        // Only provide personal name hint if we have one
        final String senderPersonal = email.getPersonal();
        if (senderPersonal != null)
        {
            contactIntent.putExtra(Intents.Insert.NAME, senderPersonal);
        }

        activity.startActivity(contactIntent);
    }

    @Override
    public String getOwnerName()
    {
        String name = null;

        // Get the name of the first account that has one.
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        for (final Account account : accounts)
        {
            if (account.name != null)
            {
                name = account.name;
                break;
            }
        }

        return name;
    }

    @Override
    public boolean isInContacts(final String emailAddress)
    {
        boolean result = false;


        final Uri uri = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress));
        final Cursor c = mContentResolver.query(uri, PROJECTION, null, null, null);

        if (c != null)
        {
            if (c.getCount() > 0)
            {
                result = true;
            }
            c.close();
        }

        return result;
    }

    @Override
    public Cursor searchContacts(final CharSequence constraint)
    {
        final String filter = (constraint == null) ? "" : constraint.toString();
        final Uri uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter));
        final Cursor c = mContentResolver.query(
                             uri,
                             PROJECTION,
                             null,
                             null,
                             SORT_ORDER);

        if (c != null)
        {
            /*
             * To prevent expensive execution in the UI thread:
             * Cursors get lazily executed, so if you don't call anything on
             * the cursor before returning it from the background thread you'll
             * have a complied program for the cursor, but it won't have been
             * executed to generate the data yet. Often the execution is more
             * expensive than the compilation...
             */
            c.getCount();
        }

        return c;
    }

    @Override
    public Cursor searchByAddress(String address)
    {
        final String filter = (address == null) ? "" : address;
        final Uri uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter));
        final Cursor c = mContentResolver.query(
                             uri,
                             PROJECTION,
                             null,
                             null,
                             SORT_ORDER);

        if (c != null)
        {
            /*
             * To prevent expensive execution in the UI thread:
             * Cursors get lazily executed, so if you don't call anything on
             * the cursor before returning it from the background thread you'll
             * have a complied program for the cursor, but it won't have been
             * executed to generate the data yet. Often the execution is more
             * expensive than the compilation...
             */
            c.getCount();
        }

        return c;
    }

    @Override
    public String getName(Cursor c)
    {
        return c.getString(NAME_INDEX);
    }

    @Override
    public String getEmail(Cursor c)
    {
        return c.getString(EMAIL_INDEX);
    }
}
