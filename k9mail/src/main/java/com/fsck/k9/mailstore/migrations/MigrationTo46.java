package com.fsck.k9.mailstore.migrations;


import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.mail.Flag;


class MigrationTo46 {
    public static void addMessagesFlagColumns(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        db.execSQL("ALTER TABLE messages ADD read INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD flagged INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD answered INTEGER default 0");
        db.execSQL("ALTER TABLE messages ADD forwarded INTEGER default 0");

        String[] projection = { "id", "flags" };

        ContentValues cv = new ContentValues();
        List<Flag> extraFlags = new ArrayList<>();

        Cursor cursor = db.query("messages", projection, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String flagList = cursor.getString(1);

                boolean read = false;
                boolean flagged = false;
                boolean answered = false;
                boolean forwarded = false;

                if (flagList != null && flagList.length() > 0) {
                    String[] flags = flagList.split(",");

                    for (String flagStr : flags) {
                        try {
                            Flag flag = Flag.valueOf(flagStr);

                            switch (flag) {
                                case ANSWERED: {
                                    answered = true;
                                    break;
                                }
                                case DELETED: {
                                    // Don't store this in column 'flags'
                                    break;
                                }
                                case FLAGGED: {
                                    flagged = true;
                                    break;
                                }
                                case FORWARDED: {
                                    forwarded = true;
                                    break;
                                }
                                case SEEN: {
                                    read = true;
                                    break;
                                }
                                case DRAFT:
                                case RECENT:
                                case X_DESTROYED:
                                case X_DOWNLOADED_FULL:
                                case X_DOWNLOADED_PARTIAL:
                                case X_REMOTE_COPY_STARTED:
                                case X_SEND_FAILED:
                                case X_SEND_IN_PROGRESS: {
                                    extraFlags.add(flag);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // Ignore bad flags
                        }
                    }
                }


                cv.put("flags", migrationsHelper.serializeFlags(extraFlags));
                cv.put("read", read);
                cv.put("flagged", flagged);
                cv.put("answered", answered);
                cv.put("forwarded", forwarded);

                db.update("messages", cv, "id = ?", new String[] { Long.toString(id) });

                cv.clear();
                extraFlags.clear();
            }
        } finally {
            cursor.close();
        }

        db.execSQL("CREATE INDEX IF NOT EXISTS msg_read ON messages (read)");
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_flagged ON messages (flagged)");
    }
}
