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
import java.util.List;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.store.EasStore;
import com.fsck.k9.mail.store.exchange.Eas;

/**
 * Parse the result of a FolderSync command
 *
 * Handles the addition, deletion, and changes to folders in the user's Exchange account.
 **/

public class FolderSyncParser extends Parser {

    public static final String TAG = "FolderSyncParser";

    // These are defined by the EAS protocol
    public static final int USER_FOLDER_TYPE = 1;
    public static final int INBOX_TYPE = 2;
    public static final int DRAFTS_TYPE = 3;
    public static final int DELETED_TYPE = 4;
    public static final int SENT_TYPE = 5;
    public static final int OUTBOX_TYPE = 6;
    public static final int TASKS_TYPE = 7;
    public static final int CALENDAR_TYPE = 8;
    public static final int CONTACTS_TYPE = 9;
    public static final int NOTES_TYPE = 10;
    public static final int JOURNAL_TYPE = 11;
    public static final int USER_MAILBOX_TYPE = 12;

    public static final List<Integer> mValidFolderTypes = Arrays.asList(INBOX_TYPE, DRAFTS_TYPE,
            DELETED_TYPE, SENT_TYPE, USER_MAILBOX_TYPE);
    // OUTBOX_TYPE is not included because K-9 uses its own special outbox. Adding the remote folder
    // causes duplicate folder names.
    
    private List<Folder> folderList;

    private EasStore easStore;

    public FolderSyncParser(InputStream in, EasStore easStore, List<Folder> folderList) throws IOException {
        super(in);

        this.easStore = easStore;
        this.folderList = folderList;
    }

    @Override
    public boolean parse() throws IOException {
        int status;
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.FOLDER_FOLDER_SYNC)
            throw new EasParserException();
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.FOLDER_STATUS) {
                status = getValueInt();
                if (status != Eas.FOLDER_STATUS_OK) {
                    Log.e(K9.LOG_TAG, "FolderSync failed: " + status);
                    if (status == Eas.FOLDER_STATUS_INVALID_KEY) {
                        easStore.setStoreSyncKey("0");
                        Log.e(K9.LOG_TAG, "Bad sync key; RESET and delete all folders");
                        // EASTODO
                        res = true;
                    } else {
                        // Other errors are at the server, so let's throw an error that will
                        // cause this sync to be retried at a later time
                        Log.e(K9.LOG_TAG, "Throwing IOException; will retry later");
                        throw new EasParserException("Folder status error");
                    }
                }
            } else if (tag == Tags.FOLDER_SYNC_KEY) {
                easStore.setStoreSyncKey(getValue());
                userLog("New Account SyncKey: ", easStore.getStoreSyncKey());
            } else if (tag == Tags.FOLDER_CHANGES) {
                changesParser();
            } else {
                skipTag();
            }
        }
        return res;
    }

    public void deleteParser() throws IOException {
        /* Right now we only do full refreshes on the folder list.
        while (nextTag(Tags.FOLDER_DELETE) != END) {
            switch (tag) {
            case Tags.FOLDER_SERVER_ID:
                String serverId = getValue();
                break;
            default:
                skipTag();
            }
        }*/
    }

    public void addParser() throws IOException {
        String name = null;
        String serverId = null;
        String parentId = null;
        int type = 0;

        while (nextTag(Tags.FOLDER_ADD) != END) {
            switch (tag) {
            case Tags.FOLDER_DISPLAY_NAME: {
                name = getValue();
                break;
            }
            case Tags.FOLDER_TYPE: {
                type = getValueInt();
                break;
            }
            case Tags.FOLDER_PARENT_ID: {
                parentId = getValue();
                break;
            }
            case Tags.FOLDER_SERVER_ID: {
                serverId = getValue();
                break;
            }
            default:
                skipTag();
            }
        }
        if (mValidFolderTypes.contains(type)) {
            EasStore.EasFolder folder = easStore.new EasFolder(name, serverId, type);
            folderList.add(folder);
            
            if (parentId != null) {
                folder.setParentId(parentId);
            }
        }

        return;
    }

    public void updateParser() throws IOException {
        /* Right now we only do full refreshes on the folder list.
        String serverId = null;
        String displayName = null;
        String parentId = null;
        while (nextTag(Tags.FOLDER_UPDATE) != END) {
            switch (tag) {
            case Tags.FOLDER_SERVER_ID:
                serverId = getValue();
                break;
            case Tags.FOLDER_DISPLAY_NAME:
                displayName = getValue();
                break;
            case Tags.FOLDER_PARENT_ID:
                parentId = getValue();
                break;
            default:
                skipTag();
                break;
            }
        }
        
        // We'll make a change if one of parentId or displayName are specified
        // serverId is required, but let's be careful just the same
        if (serverId != null && (displayName != null || parentId != null)) {
        }*/
    }

    public void changesParser() throws IOException {
        while (nextTag(Tags.FOLDER_CHANGES) != END) {
            if (tag == Tags.FOLDER_ADD) {
                addParser();
            } else if (tag == Tags.FOLDER_DELETE) {
                deleteParser();
            } else if (tag == Tags.FOLDER_UPDATE) {
                updateParser();
            } else if (tag == Tags.FOLDER_COUNT) {
                getValueInt();
            } else {
                skipTag();
            }
        }
    }

    void userLog(String ...strings) {
        Log.i(K9.LOG_TAG, Arrays.toString(strings));
    }

    void userLog(String string, int num, String string2) {
        Log.i(K9.LOG_TAG, string + num + string2);
    }
}
