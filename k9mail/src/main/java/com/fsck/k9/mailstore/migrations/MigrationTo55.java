package com.fsck.k9.mailstore.migrations;


import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.message.extractors.MessageFulltextCreator;


public class MigrationTo55 {
    public static void createFtsSearchTable(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)");

        LocalStore localStore = migrationsHelper.getLocalStore();

        try {
            List<LocalFolder> folders = localStore.getPersonalNamespaces(true);
            ContentValues cv = new ContentValues();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            for (LocalFolder folder : folders) {
                List<LocalMessage> localMessages = folder.getMessages(null, false);
                for (LocalMessage localMessage : localMessages) {
                    folder.fetch(Collections.singletonList(localMessage), fp, null);
                    MessageFulltextCreator fulltextCreator = localStore.getMessageFulltextCreator();
                    String fulltext = fulltextCreator.createFulltext(localMessage);
                    if (fulltext != null) {
                        Log.d(K9.LOG_TAG, "fulltext for msg id " + localMessage.getId() + " is " + fulltext.length() + " chars long");
                        cv.clear();
                        cv.put("docid", localMessage.getId());
                        cv.put("fulltext", fulltext);
                        db.insert("messages_fulltext", null, cv);
                    } else {
                        Log.d(K9.LOG_TAG, "no fulltext for msg id " + localMessage.getId() + " :(");
                    }
                }
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "error indexing fulltext - skipping rest, fts index is incomplete!", e);
        }
    }
}
