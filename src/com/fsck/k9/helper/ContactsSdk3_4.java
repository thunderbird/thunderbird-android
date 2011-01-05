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
        Contacts.ContactMethods.TIMES_CONTACTED + " DESC, " +
        Contacts.ContactMethods.DISPLAY_NAME + ", " +
        Contacts.ContactMethods._ID;

    /**
     * Array of columns to load from the database.
     *
     * Important: The _ID field is needed by
     * {@link com.fsck.k9.EmailAddressAdapter} or more specificly by
     * {@link android.widget.ResourceCursorAdapter}.
     */
    private static final String PROJECTION[] =
    {
        Contacts.ContactMethods._ID,
        Contacts.ContactMethods.DISPLAY_NAME,
        Contacts.ContactMethods.DATA,
        Contacts.ContactMethods.PERSON_ID
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

    /**
     * Index of the contact id field in the projection. This must match the order in
     * {@link #PROJECTION}.
     */
    private static final int CONTACT_ID_INDEX = 3;


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
                name = getName(c);
            }
            c.close();
        }

        return name;
    }

    @Override
    public boolean isInContacts(final String emailAddress)
    {
        boolean result = false;

        final Cursor c = getContactByAddress(emailAddress);

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
            where = Contacts.ContactMethods.KIND + " = " + Contacts.KIND_EMAIL +
                    " AND" + "(" +
                    Contacts.People.NAME + " LIKE ?" +
                    ") OR (" +
                    Contacts.People.NAME + " LIKE ?" +
                    ") OR (" +
                    Contacts.People.PHONETIC_NAME + " LIKE ?" +
                    ") OR (" +
                    Contacts.People.PHONETIC_NAME + " LIKE ?" +
                    ") OR (" +
                    Contacts.ContactMethods.DATA + " LIKE ?" +
                    ")";
            final String filter = constraint.toString() + "%";
            final String filter2 = "% " + filter;
            args = new String[] {filter, filter2, filter, filter2, filter};
        }

        final Cursor c = mContentResolver.query(
                             Contacts.ContactMethods.CONTENT_URI,
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
    public String getNameForAddress(String address)
    {
        if (address == null)
        {
            return null;
        }

        final Cursor c = getContactByAddress(address);

        String name = null;
        if (c != null)
        {
            if (c.getCount() > 0)
            {
                c.moveToFirst();
                name = getName(c);
            }
            c.close();
        }

        return name;
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

    @Override
    public void markAsContacted(final Address[] addresses)
    {
        //TODO: Optimize! Potentially a lot of database queries
        for (final Address address : addresses)
        {
            final Cursor c = getContactByAddress(address.getAddress());

            if (c != null)
            {
                if (c.getCount() > 0)
                {
                    c.moveToFirst();
                    final long personId = c.getLong(CONTACT_ID_INDEX);
                    Contacts.People.markAsContacted(mContentResolver, personId);
                }
                c.close();
            }
        }
    }

    /**
     * Return a {@link Cursor} instance that can be used to fetch information
     * about the contact with the given email address.
     *
     * @param address The email address to search for.
     * @return A {@link Cursor} instance that can be used to fetch information
     *         about the contact with the given email address
     */
    private Cursor getContactByAddress(String address)
    {
        final String where = Contacts.ContactMethods.KIND + " = " + Contacts.KIND_EMAIL +
                             " AND " +
                             Contacts.ContactMethods.DATA + " = ?";
        final String[] args = new String[] {address};

        final Cursor c = mContentResolver.query(
                             Contacts.ContactMethods.CONTENT_URI,
                             PROJECTION,
                             where,
                             args,
                             SORT_ORDER);
        return c;
    }
}
