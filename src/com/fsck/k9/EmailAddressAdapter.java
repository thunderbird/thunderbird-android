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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.widget.ResourceCursorAdapter;

public abstract class EmailAddressAdapter extends ResourceCursorAdapter
{
    private static EmailAddressAdapter sInstance;
    private static Context sContext;

    public static EmailAddressAdapter getInstance(Context context)
    {
        if (sInstance == null)
        {
            String className;

            sContext = context;

            /*
             * Check the version of the SDK we are running on. Choose an
             * implementation class designed for that version of the SDK.
             */
            int sdkVersion = Integer.parseInt(Build.VERSION.SDK);       // Cupcake style
            if (sdkVersion < Build.VERSION_CODES.ECLAIR)
            {
                className = "com.fsck.k9.EmailAddressAdapterSdk3_4";
            }
            else
            {
                className = "com.fsck.k9.EmailAddressAdapterSdk5";
            }

            /*
             * Find the required class by name and instantiate it.
             */
            try
            {
                Class<? extends EmailAddressAdapter> clazz =
                    Class.forName(className).asSubclass(EmailAddressAdapter.class);
                sInstance = clazz.newInstance();
            }
            catch (Exception e)
            {
                throw new IllegalStateException(e);
            }
        }

        return sInstance;
    }

    public static Context getContext()
    {
        return sContext;
    }

    protected ContentResolver mContentResolver;

    public EmailAddressAdapter()
    {
        super(getContext(), R.layout.recipient_dropdown_item, null);
        mContentResolver = getContext().getContentResolver();
    }
}
