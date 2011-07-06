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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.EasStore.EasFolder;

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

    public EasFolder mFolder;
    public Context mContext;
    public Account mAccount;

    // Create the data for local changes that need to be sent up to the server
    public abstract boolean sendLocalChanges(Serializer s)
        throws IOException;
    // Parse incoming data from the EAS server, creating, modifying, and deleting objects as
    // required through the EmailProvider
    public abstract boolean parse(InputStream is)
        throws IOException, MessagingException;
    // The name used to specify the collection type of the target (Email, Calendar, or Contacts)
    public abstract String getCollectionName();
    public abstract void cleanup();
    public abstract boolean isSyncable();

    public boolean isLooping() {
        return false;
    }

    public AbstractSyncAdapter(EasFolder folder, Account account) {
    	mFolder = folder;
        mAccount = account;
    }

    public void userLog(String ...strings) {
        Log.i(K9.LOG_TAG, Arrays.toString(strings));
    }

    public void incrementChangeCount() {
//        mService.mChangeCount++;
    }

    /**
     * Returns the current SyncKey; override if the SyncKey is stored elsewhere (as for Contacts)
     * @return the current SyncKey for the Mailbox
     * @throws IOException
     */
    public String getSyncKey() throws IOException {
        if (mFolder.getSyncKey() == null) {
            userLog("Reset SyncKey to 0");
            mFolder.setSyncKey("0");
        }
        return mFolder.getSyncKey();
    }

    public void setSyncKey(String syncKey, boolean inCommands) throws IOException {
    	mFolder.setSyncKey(syncKey);
    }
}

