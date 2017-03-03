package com.fsck.k9.mailstore.migrations;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.message.extractors.MessageFulltextCreator;


class MigrationTo55 {
    private static final int BATCH_SIZE = 20;

    static void createFtsSearchTable(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)");

        LocalStore localStore = migrationsHelper.getLocalStore();
        MessageFulltextCreator fulltextCreator = localStore.getMessageFulltextCreator();

        try {
            List<LocalFolder> folders = localStore.getPersonalNamespaces(true);
            ContentValues cv = new ContentValues();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            for (LocalFolder folder : folders) {
                Iterator<String> messageUidIterator = folder.getAllMessageUids().iterator();
                ArrayList<LocalMessage> batch = new ArrayList<>(BATCH_SIZE);
                while (messageUidIterator.hasNext()) {
                    readBatchFromIterator(messageUidIterator, folder, batch);

                    folder.fetch(batch, fp, null);
                    for (LocalMessage localMessage : batch) {
                        writeMessageFulltextIntoDatabase(db, fulltextCreator, cv, localMessage);
                    }

                    batch.clear();
                }
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "error indexing fulltext - skipping rest, fts index is incomplete!", e);
        }
    }

    private static void writeMessageFulltextIntoDatabase(SQLiteDatabase db, MessageFulltextCreator fulltextCreator,
            ContentValues cv, LocalMessage localMessage) {
        String fulltext = fulltextCreator.createFulltext(localMessage);
        if (!TextUtils.isEmpty(fulltext)) {
            Log.d(K9.LOG_TAG,
                    "fulltext for msg id " + localMessage.getId() + " is " + fulltext.length() +
                            " chars long");
            cv.clear();
            cv.put("docid", localMessage.getId());
            cv.put("fulltext", fulltext);
            db.insert("messages_fulltext", null, cv);
        } else {
            Log.d(K9.LOG_TAG, "no fulltext for msg id " + localMessage.getId() + " :(");
        }
    }

    private static void readBatchFromIterator(Iterator<String> messageUidIterator, LocalFolder folder,
            ArrayList<LocalMessage> batch) throws MessagingException {
        for (int i = 0; i < BATCH_SIZE; i++) {
            if (!messageUidIterator.hasNext()) {
                break;
            }

            String messageUid = messageUidIterator.next();
            LocalMessage localMessage = folder.getMessage(messageUid);
            batch.add(localMessage);
        }
    }
}
