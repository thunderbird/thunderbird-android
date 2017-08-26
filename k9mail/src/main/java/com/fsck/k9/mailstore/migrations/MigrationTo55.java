package com.fsck.k9.mailstore.migrations;


import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import timber.log.Timber;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.message.extractors.MessageFulltextCreator;


class MigrationTo55 {
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
                List<String> messageUids = folder.getAllMessageUids();
                for (String messageUid : messageUids) {
                    LocalMessage localMessage = folder.getMessage(messageUid);
                    folder.fetch(Collections.singletonList(localMessage), fp, null);

                    String fulltext = fulltextCreator.createFulltext(localMessage);
                    if (!TextUtils.isEmpty(fulltext)) {
                        Timber.d("fulltext for msg id %d is %d chars long", localMessage.getDatabaseId(), fulltext.length());
                        cv.clear();
                        cv.put("docid", localMessage.getDatabaseId());
                        cv.put("fulltext", fulltext);
                        db.insert("messages_fulltext", null, cv);
                    } else {
                        Timber.d("no fulltext for msg id %d :(", localMessage.getDatabaseId());
                    }
                }
            }
        } catch (MessagingException e) {
            Timber.e(e, "error indexing fulltext - skipping rest, fts index is incomplete!");
        }
    }
}
