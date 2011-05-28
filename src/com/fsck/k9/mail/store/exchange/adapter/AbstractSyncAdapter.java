/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.fsck.k9.mail.store.exchange.adapter;

import com.android.email.Email;
import com.android.email.provider.EmailContent.Account;
import com.android.email.provider.EmailContent.Mailbox;
import com.android.exchange.EasSyncService;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parent class of all sync adapters (EasMailbox, EasCalendar, and EasContacts)
 *
 */
public abstract class AbstractSyncAdapter {

    public static final int SECONDS = 1000;
    public static final int MINUTES = SECONDS*60;
    public static final int HOURS = MINUTES*60;
    public static final int DAYS = HOURS*24;
    public static final int WEEKS = DAYS*7;

    public Mailbox mMailbox;
    public EasSyncService mService;
    public Context mContext;
    public Account mAccount;
    public final android.accounts.Account mAccountManagerAccount;

    // Create the data for local changes that need to be sent up to the server
    public abstract boolean sendLocalChanges(Serializer s)
        throws IOException;
    // Parse incoming data from the EAS server, creating, modifying, and deleting objects as
    // required through the EmailProvider
    public abstract boolean parse(InputStream is)
        throws IOException;
    // The name used to specify the collection type of the target (Email, Calendar, or Contacts)
    public abstract String getCollectionName();
    public abstract void cleanup();
    public abstract boolean isSyncable();

    public boolean isLooping() {
        return false;
    }

    public AbstractSyncAdapter(Mailbox mailbox, EasSyncService service) {
        mMailbox = mailbox;
        mService = service;
        mContext = service.mContext;
        mAccount = service.mAccount;
        mAccountManagerAccount = new android.accounts.Account(mAccount.mEmailAddress,
                Email.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    public void userLog(String ...strings) {
        mService.userLog(strings);
    }

    public void incrementChangeCount() {
        mService.mChangeCount++;
    }

    /**
     * Returns the current SyncKey; override if the SyncKey is stored elsewhere (as for Contacts)
     * @return the current SyncKey for the Mailbox
     * @throws IOException
     */
    public String getSyncKey() throws IOException {
        if (mMailbox.mSyncKey == null) {
            userLog("Reset SyncKey to 0");
            mMailbox.mSyncKey = "0";
        }
        return mMailbox.mSyncKey;
    }

    public void setSyncKey(String syncKey, boolean inCommands) throws IOException {
        mMailbox.mSyncKey = syncKey;
    }
}

