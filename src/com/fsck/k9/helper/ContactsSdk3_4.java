package com.fsck.k9.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import com.fsck.k9.mail.Address;

/**
 * Access the contacts on the device using the old API (introduced in SDK 1).
 *
 * @see android.provider.Contacts
 */
@SuppressWarnings("deprecation")
public class ContactsSdk3_4 extends com.fsck.k9.helper.Contacts
{
    /**
     * The order in which the search results are returned by
     * {@link #searchContacts(CharSequence)}.
     */
    private static final String SORT_ORDER =
        Contacts.People.TIMES_CONTACTED + " DESC, " +
        Contacts.People.NAME;

    /**
     * Array of columns to load from the database.
     *
     * Important: The _ID field is needed by
     * {@link com.fsck.k9.EmailAddressAdapter} or more specificly by
     * {@link android.widget.ResourceCursorAdapter}.
     */
    private static final String PROJECTION[] =
    {
        Contacts.People.ContactMethods._ID,
        Contacts.People.ContactMethods.NAME,
        Contacts.People.ContactMethods.DATA
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


    public ContactsSdk3_4(final Context context)
    {
        super(context);
    }

    @Override
    public void createContact(final Activity activity, final Address email)
    {
        final Uri contactUri = Uri.fromParts("mailto", email.getAddress(), null);

        final Intent contactIntent = new Intent(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
        contactIntent.setData(contactUri);

        // Pass along full E-mail string for possible create dialog
        contactIntent.putExtra(Contacts.Intents.EXTRA_CREATE_DESCRIPTION,
                               email.toString());

        // Only provide personal name hint if we have one
        final String senderPersonal = email.getPersonal();
        if (senderPersonal != null)
        {
            contactIntent.putExtra(Contacts.Intents.Insert.NAME, senderPersonal);
        }

        activity.startActivity(contactIntent);
    }

    @Override
    public String getOwnerName()
    {
        String name = null;
        final Cursor c = mContentResolver.query(
                             Uri.withAppendedPath(Contacts.People.CONTENT_URI, "owner"),
                             PROJECTION,
                             null,
                             null,
                             null);

        if (c != null)
        {
            if (c.getCount() > 0)
            {
                c.moveToFirst();
                name = c.getString(NAME_INDEX);
            }
            c.close();
        }

        return name;
    }

    @Override
    public boolean isInContacts(final String emailAddress)
    {
        boolean result = false;

        final String where = Contacts.ContactMethods.DATA + "=?";
        final String[] args = new String[] {emailAddress};

        final Cursor c = mContentResolver.query(
                             Contacts.ContactMethods.CONTENT_EMAIL_URI,
                             PROJECTION,
                             where,
                             args,
                             null);

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
        final String where;
        final String[] args;
        if (constraint == null)
        {
            where = null;
            args = null;
        }
        else
        {
            where = "(" +
                    Contacts.People.NAME + " LIKE ?" +
                    ") OR (" +
                    Contacts.ContactMethods.DATA + " LIKE ?" +
                    ")";
            final String filter = constraint.toString() + "%";
            args = new String[] {filter, filter};
        }

        final Cursor c = mContentResolver.query(
                             Contacts.ContactMethods.CONTENT_EMAIL_URI,
                             PROJECTION,
                             where,
                             args,
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
        final String where;
        final String[] args;
        if (address == null)
        {
            where = null;
            args = null;
        }
        else
        {
            where = Contacts.ContactMethods.DATA + " = ?";
            args = new String[] {address};
        }

        final Cursor c = mContentResolver.query(
                Contacts.ContactMethods.CONTENT_EMAIL_URI,
                PROJECTION,
                where,
                args,
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
