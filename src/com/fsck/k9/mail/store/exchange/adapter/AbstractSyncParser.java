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

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.EasStore;
import com.fsck.k9.mail.store.EasStore.EasFolder;

/**
 * Base class for the Email and PIM sync parsers
 * Handles the basic flow of syncKeys, looping to get more data, handling errors, etc.
 * Each subclass must implement a handful of methods that relate specifically to the data type
 *
 */
public abstract class AbstractSyncParser extends Parser {
    protected EasFolder mFolder;
    protected Account mAccount;
    protected String mNewSyncKey = null;

    public AbstractSyncParser(InputStream in, EasFolder folder, Account account) throws IOException {
        super(in);
        mFolder = folder;
        mAccount = account;
    }
    
    public String getNewSyncKey() {
        return mNewSyncKey;
    }

    /**
     * Read, parse, and act on incoming commands from the Exchange server
     * @throws IOException if the connection is broken
     * @throws MessagingException
     */
    public abstract void commandsParser() throws IOException, MessagingException;

    /**
     * Read, parse, and act on server responses
     * @throws IOException
     */
    public abstract void responsesParser() throws IOException, MessagingException;

    /**
     * Commit any changes found during parsing
     * @throws IOException
     */
    public abstract void commit() throws IOException;

    /**
     * Delete all records of this class in this account
     */
    public abstract void wipe();

    /**
     * Loop through the top-level structure coming from the Exchange server
     * Sync keys and the more available flag are handled here, whereas specific data parsing
     * is handled by abstract methods implemented for each data class (e.g. Email, Contacts, etc.)
     */
    @Override
    public boolean parse() throws IOException, MessagingException {
        int status;
        boolean moreAvailable = false;
        
        // If we're not at the top of the XML tree, throw an exception.
        if (nextTag(START_DOCUMENT) != Tags.SYNC_SYNC) {
            throw new EasParserException();
        }

        // Loop here through the remaining XML
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.SYNC_COLLECTION || tag == Tags.SYNC_COLLECTIONS) {
                // Ignore these tags, since we've only got one collection sync'ing in this loop
            } else if (tag == Tags.SYNC_STATUS) {
                // Status = 1 is success; everything else is a failure
                status = getValueInt();
                if (status != 1) {
                    Log.e(K9.LOG_TAG, "Sync failed: " + status);
                    // Status = 3 means invalid sync key
                    if (status == 3) {
                        // Must delete all of the data and start over with syncKey of "0".
                        mNewSyncKey = EasStore.INITIAL_SYNC_KEY;
                        Log.e(K9.LOG_TAG, "Bad sync key; RESET and delete data");
                        wipe();
                        
                        // Indicate there's more so that we'll start sync'ing again.
                        moreAvailable = true;
                    } else if (status == 8) {
                        // This is Bad; it means the server doesn't recognize the serverId it
                        // sent us.  What's needed is a refresh of the folder list.
                        // EASTODO: SyncManager.reloadFolderList(mContext, mAccount.mId, true);
                    }
                    // TODO Look at other error codes and consider what's to be done
                }
            } else if (tag == Tags.SYNC_COMMANDS) {
                commandsParser();
            } else if (tag == Tags.SYNC_RESPONSES) {
                responsesParser();
            } else if (tag == Tags.SYNC_MORE_AVAILABLE) {
                moreAvailable = true;
            } else if (tag == Tags.SYNC_SYNC_KEY) {
                if (mFolder.getSyncKey().equals("0")) {
                    moreAvailable = true;
                }
                String newKey = getValue();
                userLog("Parsed key for ", mFolder.toString(), ": ", newKey);
                if (!newKey.equals(mFolder.getSyncKey())) {
                    mNewSyncKey = newKey;
                }
            } else {
                skipTag();
            }
        }

        // Commit any changes
        commit();

        // Let the caller know that there's more to do.
        userLog("Returning moreAvailable = " + moreAvailable);
        return moreAvailable;
    }

    void userLog(String ...strings) {
        Log.i(K9.LOG_TAG, Arrays.toString(strings));
    }

    void userLog(String string, int num, String string2) {
        Log.i(K9.LOG_TAG, string + num + string2);
    }
}
