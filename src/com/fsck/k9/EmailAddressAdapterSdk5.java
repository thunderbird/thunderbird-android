/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts.Data;
import android.view.View;
import android.widget.TextView;
import com.fsck.k9.mail.Address;

public class EmailAddressAdapterSdk5 extends EmailAddressAdapter
{
    public static final int NAME_INDEX = 1;
    public static final int DATA_INDEX = 2;

    private static final String SORT_ORDER = Contacts.TIMES_CONTACTED
            + " DESC, " + Contacts.DISPLAY_NAME;

    private static final String[] PROJECTION = {
        Data._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Email.DATA // 2
    };

    @Override
    public final String convertToString(Cursor cursor)
    {
        String name = cursor.getString(NAME_INDEX);
        String address = cursor.getString(DATA_INDEX);

        return new Address(address, name).toString();
    }

    @Override
    public final void bindView(View view, Context context, Cursor cursor)
    {
        TextView text1 = (TextView) view.findViewById(R.id.text1);
        TextView text2 = (TextView) view.findViewById(R.id.text2);
        text1.setText(cursor.getString(NAME_INDEX));
        text2.setText(cursor.getString(DATA_INDEX));
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint)
    {
        String filter = constraint == null ? "" : constraint.toString();
        Uri uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter));
        Cursor c = mContentResolver.query(uri, PROJECTION, null, null, SORT_ORDER);
        // To prevent expensive execution in the UI thread
        // Cursors get lazily executed, so if you don't call anything on the cursor before
        // returning it from the background thread you'll have a complied program for the cursor,
        // but it won't have been executed to generate the data yet. Often the execution is more
        // expensive than the compilation...
        if (c != null)
        {
            c.getCount();
        }
        return c;
    }
}
