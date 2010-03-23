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
import android.database.DatabaseUtils;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.view.View;
import android.widget.TextView;
import com.fsck.k9.mail.Address;

import static android.provider.Contacts.ContactMethods.CONTENT_EMAIL_URI;

public class EmailAddressAdapterSdk3_4 extends EmailAddressAdapter
{
    public static final int NAME_INDEX = 1;

    public static final int DATA_INDEX = 2;

    private static final String SORT_ORDER = People.TIMES_CONTACTED + " DESC, " + People.NAME;

    private static final String[] PROJECTION =
    {
        ContactMethods._ID, // 0
        ContactMethods.NAME, // 1
        ContactMethods.DATA // 2
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
        TextView text1 = (TextView)view.findViewById(R.id.text1);
        TextView text2 = (TextView)view.findViewById(R.id.text2);
        text1.setText(cursor.getString(NAME_INDEX));
        text2.setText(cursor.getString(DATA_INDEX));
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint)
    {
        String where = null;

        if (constraint != null)
        {
            String filter = DatabaseUtils.sqlEscapeString(constraint.toString() + '%');

            StringBuilder s = new StringBuilder();
            s.append("("+People.NAME+" LIKE ");
            s.append(filter);
            s.append(") OR ("+ContactMethods.DATA+" LIKE ");
            s.append(filter);
            s.append(")");

            where = s.toString();
        }

        return mContentResolver.query(CONTENT_EMAIL_URI, PROJECTION, where, null, SORT_ORDER);
    }
}
