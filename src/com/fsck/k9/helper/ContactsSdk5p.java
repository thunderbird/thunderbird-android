package com.fsck.k9.helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

/**
 * Access the contacts on the device using the API introduced with SDK 5.
 * Use some additional code to make search work with phonetic names.
 *
 * Android versions >= 2.2 (Froyo) support searching for phonetic names
 * out of the box (see {@link ContactsSdk5}).
 *
 * @see android.provider.ContactsContract
 */
public class ContactsSdk5p extends ContactsSdk5 {
    public ContactsSdk5p(final Context context) {
        super(context);
    }

    @Override
    public Cursor searchContacts(final CharSequence constraint) {
        if (constraint == null) {
            return null;
        }

        // Lookup using Email.CONTENT_FILTER_URI to get matching contact ids.
        // This does all sorts of magic we don't want to replicate.
        final String filter = constraint.toString();
        final Uri uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter));
        final Cursor cursor = mContentResolver.query(
                                  uri,
                                  new String[] {Email.CONTACT_ID},
                                  null,
                                  null,
                                  null);

        final StringBuilder matches = new StringBuilder();
        if ((cursor != null) && (cursor.getCount() > 0)) {
            boolean first = true;
            while (cursor.moveToNext()) {
                if (first) {
                    first = false;
                } else {
                    matches.append(",");
                }
                matches.append(cursor.getLong(0));
            }
            cursor.close();
        }

        // Find contacts with email addresses that have been found using
        // Email.CONTENT_FILTER_URI above or ones that have a matching phonetic name.
        final String where = Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" +
                             " AND " +
                             "(" +
                             // Match if found by Email.CONTENT_FILTER_URI
                             Email.CONTACT_ID + " IN (" + matches.toString() + ")" +
                             " OR " +
                             // Match if phonetic given name starts with "constraint"
                             StructuredName.PHONETIC_GIVEN_NAME + " LIKE ?" +
                             " OR " +
                             // Match if phonetic given name contains a word that starts with "constraint"
                             StructuredName.PHONETIC_GIVEN_NAME + " LIKE ?" +
                             " OR " +
                             // Match if phonetic middle name starts with "constraint"
                             StructuredName.PHONETIC_MIDDLE_NAME + " LIKE ?" +
                             " OR " +
                             // Match if phonetic middle name contains a word that starts with "constraint"
                             StructuredName.PHONETIC_MIDDLE_NAME + " LIKE ?" +
                             " OR " +
                             // Match if phonetic family name starts with "constraint"
                             StructuredName.PHONETIC_FAMILY_NAME + " LIKE ?" +
                             " OR " +
                             // Match if phonetic family name contains a word that starts with "constraint"
                             StructuredName.PHONETIC_FAMILY_NAME + " LIKE ?" +
                             ")";
        final String filter1 = constraint.toString() + "%";
        final String filter2 = "% " + filter1;
        final String[] args = new String[] {filter1, filter2, filter1, filter2, filter1, filter2};
        final Cursor c = mContentResolver.query(
                             Email.CONTENT_URI,
                             PROJECTION,
                             where,
                             args,
                             SORT_ORDER);

        if (c != null) {
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
}
