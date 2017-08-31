package com.fsck.k9.mailstore;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSQLiteConnection;

@RunWith(K9RobolectricTestRunner.class)
public class MigrationTest {

    Account account;
    File databaseFile;
    File attachmentDir;

    @Before
    public void setUp() throws Exception {
        K9.setDebug(true);
        ShadowLog.stream = System.out;
        ShadowSQLiteConnection.reset();

        account = getNewAccount();

        StorageManager storageManager = StorageManager.getInstance(RuntimeEnvironment.application);
        databaseFile = storageManager.getDatabase(account.getUuid(), account.getLocalStorageProviderId());
        Assert.assertTrue(databaseFile.getParentFile().isDirectory() || databaseFile.getParentFile().mkdir());

        attachmentDir = StorageManager.getInstance(RuntimeEnvironment.application).getAttachmentDirectory(
                account.getUuid(), account.getLocalStorageProviderId());
        Assert.assertTrue(attachmentDir.isDirectory() || attachmentDir.mkdir());
    }

    private SQLiteDatabase createV50Database() {

        SQLiteDatabase db = RuntimeEnvironment.application.openOrCreateDatabase(databaseFile.getName(),
                Context.MODE_PRIVATE, null);

        String[] v50SchemaSql = new String[] {
                "CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, last_updated INTEGER, unread_count INTEGER," +
                        "visible_limit INTEGER, status TEXT, push_state TEXT, last_pushed INTEGER," +
                        "flagged_count INTEGER default 0, integrate INTEGER, top_group INTEGER, poll_class TEXT," +
                        "push_class TEXT, display_class TEXT, notify_class TEXT);",
                "CREATE TABLE messages (id INTEGER PRIMARY KEY, deleted INTEGER default 0, folder_id INTEGER, uid TEXT," +
                        "subject TEXT, date INTEGER, flags TEXT, sender_list TEXT, to_list TEXT, cc_list TEXT," +
                        "bcc_list TEXT, reply_to_list TEXT, html_content TEXT, text_content TEXT," +
                        "attachment_count INTEGER, internal_date INTEGER, message_id TEXT, preview TEXT," +
                        "mime_type TEXT, normalized_subject_hash INTEGER, empty INTEGER, read INTEGER default 0," +
                        "flagged INTEGER default 0, answered INTEGER default 0, forwarded INTEGER default 0);",
                "CREATE TABLE headers (id INTEGER PRIMARY KEY, message_id INTEGER, name TEXT, value TEXT);",
                "CREATE TABLE threads (id INTEGER PRIMARY KEY, message_id INTEGER, root INTEGER, parent INTEGER);",
                "CREATE TABLE attachments (id INTEGER PRIMARY KEY, message_id INTEGER,store_data TEXT," +
                        "content_uri TEXT, size INTEGER, name TEXT, mime_type TEXT," +
                        "content_id TEXT, content_disposition TEXT);",
                "CREATE TABLE pending_commands (id INTEGER PRIMARY KEY, command TEXT, arguments TEXT);",
                "CREATE INDEX folder_name ON folders (name);",
                "CREATE INDEX header_folder ON headers (message_id);",
                "CREATE INDEX msg_uid ON messages (uid, folder_id);",
                "CREATE INDEX msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date);",
                "CREATE INDEX msg_empty ON messages (empty);",
                "CREATE INDEX msg_read ON messages (read);",
                "CREATE INDEX msg_flagged ON messages (flagged);",
                "CREATE INDEX msg_composite ON messages (deleted, empty,folder_id,flagged,read);",
                "CREATE INDEX threads_message_id ON threads (message_id);",
                "CREATE INDEX threads_root ON threads (root);",
                "CREATE INDEX threads_parent ON threads (parent);",
                "CREATE TRIGGER set_thread_root AFTER INSERT ON threads BEGIN " +
                        "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; END;",
                "CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN " +
                        "DELETE FROM messages WHERE old.id = folder_id; END;",
                "CREATE TRIGGER delete_message BEFORE DELETE ON messages BEGIN " +
                        "DELETE FROM attachments WHERE old.id = message_id;" +
                        "DELETE FROM headers where old.id = message_id; END;"
        };
        for (String statement : v50SchemaSql) {
            db.execSQL(statement);
        }
        db.setVersion(50);

        String[] folderSql = new String[] {
                "INSERT INTO folders VALUES (1,'Trash',0,NULL,25,NULL,NULL,0,0,0,1,'NO_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (2,'Sent',1448975758597,NULL,25,NULL,'uidNext=552',NULL,0,0,1,'NO_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (8,'Drafts',0,NULL,25,NULL,NULL,0,0,0,1,'FIRST_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (13,'Spam',NULL,NULL,25,NULL,NULL,NULL,0,0,1,'NO_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (14,'K9MAIL_INTERNAL_OUTBOX',NULL,NULL,25,NULL,NULL,NULL,0,0,1,'NO_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (15,'K9mail-errors',NULL,NULL,25,NULL,NULL,NULL,0,0,1,'NO_CLASS','INHERITED','FIRST_CLASS','INHERITED')",
                "INSERT INTO folders VALUES (16,'dev',1453812012958,NULL,25,NULL,'uidNext=9',0,0,0,0,'INHERITED','SECOND_CLASS','NO_CLASS','INHERITED')",
        };
        for (String statement : folderSql) {
            db.execSQL(statement);
        }

        return db;
    }

    private void insertSimplePlaintextMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(2,0,16,'3','regular mail',1453380493000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','','<pre class=\"k9mail\">nothing special here.<br /></pre>','nothing special here.\n'," +
                        "0,1453380499000,'<20160121124813.GA31046@littlepip>','nothing special here.'," +
                        "'text/plain',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (2,'Return-Path','<look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'X-Original-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Delivered-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Date','Thu, 21 Jan 2016 13:48:13 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Subject','regular mail')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Message-ID','<20160121124813.GA31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Content-Disposition','inline')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'User-Agent','Mutt/1.5.24 (2015-08-30)')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Content-Type','text/plain\n charset=utf-8')",
                "INSERT INTO headers (message_id, name, value) VALUES (2,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(3,2,3,NULL)",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migrateTextPlain() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertSimplePlaintextMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("3");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals("text/plain", msg.getMimeType());
        Assert.assertEquals(2, msg.getDatabaseId());
        Assert.assertEquals(13, msg.getHeaderNames().size());
        Assert.assertEquals(0, msg.getAttachmentCount());

        Assert.assertEquals(1, msg.getHeader("User-Agent").length);
        Assert.assertEquals("Mutt/1.5.24 (2015-08-30)", msg.getHeader("User-Agent")[0]);
        Assert.assertEquals(1, msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE).length);
        Assert.assertEquals("text/plain",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], null));
        Assert.assertEquals("utf-8",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], "charset"));

        Assert.assertTrue(msg.getBody() instanceof BinaryMemoryBody);

        String msgTextContent = MessageExtractor.getTextFromPart(msg);
        Assert.assertEquals("nothing special here.\r\n", msgTextContent);
    }

    private void insertMixedWithAttachments(SQLiteDatabase db) throws Exception {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(3,0,16,'4','mail with attach',1453380649000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_PARTIAL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','','<pre class=\"k9mail\">ooohh, an attachment!<br /></pre>','ooohh, an attachment!\n'," +
                        "2,1453380654000,'<20160121125049.GB31046@littlepip>','ooohh, an attachment!'," +
                        "'multipart/mixed',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (3,'Date','Thu, 21 Jan 2016 13:50:49 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'Subject','mail with attach')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'Message-ID','<20160121125049.GB31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'Content-Type','multipart/mixed; boundary=\"----5D6OUTIYLNN2X63O0R2M0V53TOUAQP\"')",
                "INSERT INTO headers (message_id, name, value) VALUES (3,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(4,3,4,NULL)",

                "INSERT INTO attachments VALUES(3,3,'2'," +
                        "'content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/3/RAW',2250," +
                        "'k9small.png','image/png',NULL,'attachment')",
                "INSERT INTO attachments VALUES(4,3,'2'," +
                        "'content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/5/RAW',2250," +
                        "'baduri.png','application/whatevs',NULL,'attachment')",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }

        copyAttachmentFromFile("k9small.png", 3, 2250);
        copyAttachmentFromFile("k9small.png", 5, 2250);
    }

    @Test
    public void migrateMixedWithAttachments() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertMixedWithAttachments(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("4");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(3, msg.getDatabaseId());
        Assert.assertEquals(8, msg.getHeaderNames().size());
        Assert.assertEquals("multipart/mixed", msg.getMimeType());
        Assert.assertEquals(1, msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE).length);
        Assert.assertEquals("multipart/mixed",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], null));
        Assert.assertEquals("----5D6OUTIYLNN2X63O0R2M0V53TOUAQP",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], "boundary"));
        Assert.assertEquals(2, msg.getAttachmentCount());

        Multipart body = (Multipart) msg.getBody();
        Assert.assertEquals(3, body.getCount());

        Assert.assertEquals("multipart/alternative", body.getBodyPart(0).getMimeType());
        LocalBodyPart attachmentPart = (LocalBodyPart) body.getBodyPart(1);
        Assert.assertEquals("image/png", attachmentPart.getMimeType());
        Assert.assertEquals("2", attachmentPart.getServerExtra());
        Assert.assertEquals("attachment", MimeUtility.getHeaderParameter(attachmentPart.getDisposition(), null));
        Assert.assertEquals("k9small.png", MimeUtility.getHeaderParameter(attachmentPart.getDisposition(), "filename"));
        Assert.assertEquals("2250", MimeUtility.getHeaderParameter(attachmentPart.getDisposition(), "size"));

        FileBackedBody attachmentBody = (FileBackedBody) attachmentPart.getBody();
        Assert.assertEquals(2250, attachmentBody.getSize());
        Assert.assertEquals(MimeUtil.ENC_BINARY, attachmentBody.getEncoding());

        Assert.assertEquals("application/whatevs", body.getBodyPart(2).getMimeType());
        Assert.assertNull(body.getBodyPart(2).getBody());
    }

    private void insertPgpMimeSignedMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(4,0,16,'5','signed mail with attach',1453380687000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_PARTIAL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','','<pre class=\"k9mail\">attached AND signed!<br /><br /> - V<br /></pre>','attached AND signed!\n" +
                        "\n" +
                        " - V\n" +
                        "',2,1453380691000,'<20160121125127.GC31046@littlepip>','attached AND signed! - V'," +
                        "'multipart/signed',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (4,'Date','Thu, 21 Jan 2016 13:51:27 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'Subject','signed mail with attach')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'Message-ID','<20160121125127.GC31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'Content-Type','multipart/signed; boundary=\"----03N4L9HQP6BY776BVZW4ZIPOWZLBJH\"')",
                "INSERT INTO headers (message_id, name, value) VALUES (4,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(5,4,5,NULL)",

                "INSERT INTO attachments VALUES(6,4,'2',NULL,836,'signature.asc','application/pgp-signature',NULL,'')",
                "INSERT INTO attachments VALUES(5,4,'1.2',NULL,39456,'smirk.png','image/png',NULL,'attachment')",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migratePgpMimeSignedMessage() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertPgpMimeSignedMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("5");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(4, msg.getDatabaseId());
        Assert.assertEquals(8, msg.getHeaderNames().size());
        Assert.assertEquals("multipart/mixed", msg.getMimeType());
        Assert.assertEquals(2, msg.getAttachmentCount());

        Multipart body = (Multipart) msg.getBody();
        Assert.assertEquals(3, body.getCount());

        Assert.assertEquals("multipart/alternative", body.getBodyPart(0).getMimeType());
        Assert.assertEquals("image/png", body.getBodyPart(1).getMimeType());
        Assert.assertEquals("application/pgp-signature", body.getBodyPart(2).getMimeType());
    }


    private void insertPgpMimeEncryptedMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(5,0,16,'6','pgp/mime encrypted text',1453380734000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','',NULL,NULL,2,1453380737000,'<20160121125214.GD31046@littlepip>',NULL," +
                        "'multipart/encrypted',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (5,'Return-Path','<look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'X-Original-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Delivered-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Date','Thu, 21 Jan 2016 13:52:14 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Subject','pgp/mime encrypted text')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Message-ID','<20160121125214.GD31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Content-Disposition','inline')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'User-Agent','Mutt/1.5.24 (2015-08-30)')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Content-Type','multipart/encrypted; protocol=\"application/pgp-encrypted\";\tboundary=\"UoPmpPX/dBe4BELn\"')",
                "INSERT INTO headers (message_id, name, value) VALUES (5,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(6,5,6,NULL)",

                "INSERT INTO attachments VALUES(1,5,NULL,'content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/1/RAW',12,NULL,'application/pgp-encrypted',NULL,'attachment')",
                "INSERT INTO attachments VALUES(2,5,NULL,'content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/2/RAW',1946,'msg.asc','application/octet-stream',NULL,'attachment')",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migratePgpMimeEncryptedMessage() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertPgpMimeEncryptedMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("6");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(5, msg.getDatabaseId());
        Assert.assertEquals(13, msg.getHeaderNames().size());
        Assert.assertEquals("multipart/encrypted", msg.getMimeType());
        Assert.assertEquals(2, msg.getAttachmentCount());

        Multipart body = (Multipart) msg.getBody();
        Assert.assertEquals(1, msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE).length);
        Assert.assertEquals("application/pgp-encrypted",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], "protocol"));
        Assert.assertEquals("UoPmpPX/dBe4BELn",
                MimeUtility.getHeaderParameter(msg.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0], "boundary"));
        Assert.assertEquals("UoPmpPX/dBe4BELn", body.getBoundary());
        Assert.assertEquals(2, body.getCount());

        Assert.assertEquals("application/pgp-encrypted", body.getBodyPart(0).getMimeType());
        Assert.assertEquals("application/octet-stream", body.getBodyPart(1).getMimeType());
    }

    private void insertPgpInlineEncryptedMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(6,0,16,'7','pgp/inline encrypted',1453380759000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','','<pre class=\"k9mail\">-----BEGIN PGP MESSAGE-----<br />Version: GnuPG v1<br /><br />" +
                        "hQIMA65sUXMb7rTOAQ//TemIsM3AK2uYT8P5R4vJSqRkdyr8T0sg0R/xtr7oHY19<br />" +
                        "fv1t9yu9Z0zub5v4+AhcJ7ZbURUG+ETGsrBS7xJhHxlCu0KQYEme6tnBOrkXN0Tn<br />" +
                        "9h52EiK7ENbZ53IuBael3XoEWrpC/1nZGSpjvUt+DUC7+OdVGHWMfoxjNMKNcJiT<br />" +
                        "quVDpaiI2yqDCvHOn9yLlxlZa+j82sSwOb285txTOQWhCY6H1bllAByOiGpQGp6F<br />" +
                        "FMWsts7y04VoFTwxadzWywi1Bdscd8HFDm0TO/75OKcUVoGRcNuHszxGxSgT+R77<br />" +
                        "eB0wgXLZQ1NnqWTOGekGJ0x9Ddx8FcMIcokDGUh2D98jJZwVa3I9ssKFUORMCCaN<br />" +
                        "sAe2xHk3q+tr7mm5qPD7DU3Ld7qotjBVoXlo/jNLbK2drP9wARZV0u6395zp38QR<br />" +
                        "zp4yattwqpNri4GRG/hD2bM2rD+pQKtCnwqW5VlW2oh2NQ5ztf9eZ5oJgw8clljH<br />" +
                        "ciJi5B0tuVygJnI/hHy+N4TE8mDAN6IXSBnR570zX6Idb8tiAVSlkdh89wZJ1m7G<br />" +
                        "nvU7HlNyW5cf6C5RWz+gGsg1aeNQqZGCJkfdDXXd4rpzNsp5LDmzPMwj3f8QIHhi<br />" +
                        "vsCBaMvS1wiYGjTlAXSbcLVDZ1kyTNd92H1ktC1/A5bUDLZe7EhzHBMDh6YI9k3S<br />" +
                        "6QFx2E8otLIICwu8lEVbVqdvFkApfpUt8DEBbXxxiw1c8Mbe6EmgTD4H8I6NxwXd<br />" +
                        "7QGerWOrPzyKjvdBQkmKvfao2fdvPWqsu+tgejmMlQQ4t75zV6Tb75gQOacDUr0w<br />" +
                        "66D0t/EMp/KL2roBpw3purEubaYsQpQImBiyxpJbLKNL0dxT16V7xa4XUlon8EI+<br />" +
                        "N/gTZdCzmVUmX5mPRheZaNn0y7/TiMTxMxO+oKDGt0ks4Hqvz9+lphcfqTl2Nhop<br />" +
                        "AW79xOh8hD2+XIxiNiyRYgugCDm/iSixKStjyV822/6DBltWZ9r1OgeNFBgpIZiO<br />" +
                        "r5SA4oM0krMljpE+9wCHZt3R4PxJ6Pv+9cxb1MC5tWRO7SKIrp53TZtiqDVTARGh<br />" +
                        "FgoJGrdL1jo89efPiZJY8mijtPuYk0gwSbTnE7mOaIoczzQr99ojwso1T2CeXIP0<br />" +
                        "Eg3sCv/0b+fTQlFCRQxvuUaQ75NfhUnA7dBFYCdBtrje/eREO4I/Jg05pb+pp81n<br />" +
                        "T/QGVl6uA5+zm3YdRSvZ5BIpZleu/ddkvH1a7/113XUmPun397NBC1X0RTa2h6X6<br />" +
                        "HGPTgqaQ89FJfU3oYvfvEmo8hrKPmaPR+3AXgSCkAGWM+xRddzFAxf72S+LrFaZX<br />" +
                        "mRf25pDoZf8i2PgsMd2cFcJdO01J6sdtIsm8k9mfk2uVwAFaUBBAgBHZCFzGp3yt<br />" +
                        "0OIiPTFKywtLMIfqla6hDEoPb+yosiRI9lQmGyW8bOCwO5sMUvFZfTAJnhQvRazS<br />" +
                        "HWeTlYCKZadM4p2p/ucFAm94edi+DPz2bzaFg7O/+B9N2g/s7PvD0djJEHGGDT+S<br />" +
                        "ucdGTWliAnOaFCyGUWXmAE7C1O4m+4bJwVmz7ts0ReLwDCGhPmA2/+F/K9WgaU1f<br />" +
                        "j8JjG3kNUmcrXP0PEctwdi9phnJscL5abfOrI9mT3eYfXIVy<br />" +
                        "=tD4Z<br />" +
                        "-----END PGP MESSAGE-----<br />" +
                        "</pre>','-----BEGIN PGP MESSAGE-----\n" +
                        "Version: GnuPG v1\n" +
                        "\n" +
                        "hQIMA65sUXMb7rTOAQ//TemIsM3AK2uYT8P5R4vJSqRkdyr8T0sg0R/xtr7oHY19\n" +
                        "fv1t9yu9Z0zub5v4+AhcJ7ZbURUG+ETGsrBS7xJhHxlCu0KQYEme6tnBOrkXN0Tn\n" +
                        "9h52EiK7ENbZ53IuBael3XoEWrpC/1nZGSpjvUt+DUC7+OdVGHWMfoxjNMKNcJiT\n" +
                        "quVDpaiI2yqDCvHOn9yLlxlZa+j82sSwOb285txTOQWhCY6H1bllAByOiGpQGp6F\n" +
                        "FMWsts7y04VoFTwxadzWywi1Bdscd8HFDm0TO/75OKcUVoGRcNuHszxGxSgT+R77\n" +
                        "eB0wgXLZQ1NnqWTOGekGJ0x9Ddx8FcMIcokDGUh2D98jJZwVa3I9ssKFUORMCCaN\n" +
                        "sAe2xHk3q+tr7mm5qPD7DU3Ld7qotjBVoXlo/jNLbK2drP9wARZV0u6395zp38QR\n" +
                        "zp4yattwqpNri4GRG/hD2bM2rD+pQKtCnwqW5VlW2oh2NQ5ztf9eZ5oJgw8clljH\n" +
                        "ciJi5B0tuVygJnI/hHy+N4TE8mDAN6IXSBnR570zX6Idb8tiAVSlkdh89wZJ1m7G\n" +
                        "nvU7HlNyW5cf6C5RWz+gGsg1aeNQqZGCJkfdDXXd4rpzNsp5LDmzPMwj3f8QIHhi\n" +
                        "vsCBaMvS1wiYGjTlAXSbcLVDZ1kyTNd92H1ktC1/A5bUDLZe7EhzHBMDh6YI9k3S\n" +
                        "6QFx2E8otLIICwu8lEVbVqdvFkApfpUt8DEBbXxxiw1c8Mbe6EmgTD4H8I6NxwXd\n" +
                        "7QGerWOrPzyKjvdBQkmKvfao2fdvPWqsu+tgejmMlQQ4t75zV6Tb75gQOacDUr0w\n" +
                        "66D0t/EMp/KL2roBpw3purEubaYsQpQImBiyxpJbLKNL0dxT16V7xa4XUlon8EI+\n" +
                        "N/gTZdCzmVUmX5mPRheZaNn0y7/TiMTxMxO+oKDGt0ks4Hqvz9+lphcfqTl2Nhop\n" +
                        "AW79xOh8hD2+XIxiNiyRYgugCDm/iSixKStjyV822/6DBltWZ9r1OgeNFBgpIZiO\n" +
                        "r5SA4oM0krMljpE+9wCHZt3R4PxJ6Pv+9cxb1MC5tWRO7SKIrp53TZtiqDVTARGh\n" +
                        "FgoJGrdL1jo89efPiZJY8mijtPuYk0gwSbTnE7mOaIoczzQr99ojwso1T2CeXIP0\n" +
                        "Eg3sCv/0b+fTQlFCRQxvuUaQ75NfhUnA7dBFYCdBtrje/eREO4I/Jg05pb+pp81n\n" +
                        "T/QGVl6uA5+zm3YdRSvZ5BIpZleu/ddkvH1a7/113XUmPun397NBC1X0RTa2h6X6\n" +
                        "HGPTgqaQ89FJfU3oYvfvEmo8hrKPmaPR+3AXgSCkAGWM+xRddzFAxf72S+LrFaZX\n" +
                        "mRf25pDoZf8i2PgsMd2cFcJdO01J6sdtIsm8k9mfk2uVwAFaUBBAgBHZCFzGp3yt\n" +
                        "0OIiPTFKywtLMIfqla6hDEoPb+yosiRI9lQmGyW8bOCwO5sMUvFZfTAJnhQvRazS\n" +
                        "HWeTlYCKZadM4p2p/ucFAm94edi+DPz2bzaFg7O/+B9N2g/s7PvD0djJEHGGDT+S\n" +
                        "ucdGTWliAnOaFCyGUWXmAE7C1O4m+4bJwVmz7ts0ReLwDCGhPmA2/+F/K9WgaU1f\n" +
                        "j8JjG3kNUmcrXP0PEctwdi9phnJscL5abfOrI9mT3eYfXIVy\n" +
                        "=tD4Z\n" +
                        "-----END PGP MESSAGE-----\n" +
                        "',0,1453380763000,'<20160121125239.GE31046@littlepip>','Version: GnuPG v1 hQIMA65sUXMb7rTOAQ//TemIsM3AK2uYT8P5R4vJSqRkdyr8T0sg0R/xtr7oHY19 fv1t9yu9Z0zub5v4+AhcJ7ZbURUG+ETGsrBS7xJhHxlCu0KQYEme6tnBOrkXN0Tn 9h52EiK7ENbZ53IuBael3XoEWrpC/1nZGSpjvUt+DUC7+OdVGHWMfoxjNMKNcJiT quVDpaiI2yqDCvHOn9yLlxlZa+j82sSwOb285txTOQWhCY6H1bllAByOiGpQGp6F FMWsts7y04VoFTwxadzWywi1Bdscd8HFDm0TO/75OKcUVoGRcNuHszxGxSgT+R77 eB0wgXLZQ1NnqWTOGekGJ0x9Ddx8FcMIcokDGUh2D98jJZwVa3I9ssKFUORMCCaN sAe2xHk3q+tr7mm5qPD7DU3Ld7qotjBVoXlo/jNLbK2drP9wARZV0u6395zp38QR zp4yattwqpNri4GRG/hD2bM2rD+pQKtCnwqW5Vl','text/plain',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (6,'Return-Path','<look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'X-Original-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Delivered-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Date','Thu, 21 Jan 2016 13:52:39 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Subject','pgp/inline encrypted')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Message-ID','<20160121125239.GE31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'User-Agent','Mutt/1.5.24 (2015-08-30)')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Content-Type','text/plain\n charset=utf-8')",
                "INSERT INTO headers (message_id, name, value) VALUES (6,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(7,6,7,NULL)",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migratePgpInlineEncryptedMessage() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertPgpInlineEncryptedMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("7");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(6, msg.getDatabaseId());
        Assert.assertEquals(12, msg.getHeaderNames().size());
        Assert.assertEquals("text/plain", msg.getMimeType());
        Assert.assertEquals(0, msg.getAttachmentCount());
        Assert.assertTrue(msg.getBody() instanceof BinaryMemoryBody);

        String msgTextContent = MessageExtractor.getTextFromPart(msg);
        Assert.assertEquals(OpenPgpUtils.PARSE_RESULT_MESSAGE, OpenPgpUtils.parseMessage(msgTextContent));
    }

    private void insertPgpInlineClearsignedMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(7,0,16,'8','pgp/inline clearsigned',1453380782000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','look@my.amazin.horse;','valodim@mugenguild.com'," +
                        "'','','','<pre class=\"k9mail\">-----BEGIN PGP SIGNED MESSAGE-----<br />Hash: SHA1<br /><br />" +
                        "this msg is only signed~<br /><br />-----BEGIN PGP SIGNATURE-----<br />Version: GnuPG v1<br />" +
                        "<br />iQIcBAEBAgAGBQJWoNSuAAoJEHvRgyDerfoROjkQAK2Md7CE4GDcHaWppXUttUeh<br />" +
                        "wrjbnW2McJSysjWmb6FYt1CYsjl+3vImIgqg59rZxjdaffs+lNxO3B0blfAOMjSs<br />" +
                        "LOCJytbB02/f79e44kWWt5ZG0d+3NTl8sN4OkXb47fot28CG7JLJgkGpMbmwm6sM<br />" +
                        "C5pUA4o/OwWbkg2xj2FUDmgx4clyA9BxEBxO1ZU+VFLawtZ6OdRLF8iKzJKyKTi4<br />" +
                        "GEQEiSET5UcRMFgUeI6U3fLPKnmSer4qZP8/G9IcvpVgCOzW6foMZ8mbO+n/Jqs4<br />" +
                        "644slRlBNYor/5tl5f6sYy5Hyzrj4c6Tq2Duzu0VECQnaTOCl7QyW8Vc1R2qferO<br />" +
                        "4Rs94InVWfNn5ltV7OPHLBSNAZ8YRILpafrWw+EZbrE5+hwlKernpdn6dRAG668s<br />" +
                        "KyASsXjtGfPUlcYtFvJQS2U/gAsGcQPPL9g4x8FL2jRqDI92EU8Cw+G2HKlqNegP<br />" +
                        "6vNkfGv4/LRCtQ+KrajFcSyqrjmZV8lohCI3qJowtK5nFN3Z+5Kk2jqVgRHYuXhR<br />" +
                        "uCcQrwHOvVts+POHWqbPOR3VDaGWS40rqAwaJrko92IOxhEpUBmNnpH2dARKs1AB<br />" +
                        "itiWpWNkbalgbEDBx4mmdcj4KsVF5Q86xfg3n8zUQAqhoOKwll5wRQ11lQOXca6O<br />" +
                        "GsPI+A/j12owLSxOez//<br />=Umj+<br />-----END PGP SIGNATURE-----<br />" +
                        "</pre>','-----BEGIN PGP SIGNED MESSAGE-----\n" +
                        "Hash: SHA1\n" +
                        "\n" +
                        "this msg is only signed~\n" +
                        "\n" +
                        "-----BEGIN PGP SIGNATURE-----\n" +
                        "Version: GnuPG v1\n" +
                        "\n" +
                        "iQIcBAEBAgAGBQJWoNSuAAoJEHvRgyDerfoROjkQAK2Md7CE4GDcHaWppXUttUeh\n" +
                        "wrjbnW2McJSysjWmb6FYt1CYsjl+3vImIgqg59rZxjdaffs+lNxO3B0blfAOMjSs\n" +
                        "LOCJytbB02/f79e44kWWt5ZG0d+3NTl8sN4OkXb47fot28CG7JLJgkGpMbmwm6sM\n" +
                        "C5pUA4o/OwWbkg2xj2FUDmgx4clyA9BxEBxO1ZU+VFLawtZ6OdRLF8iKzJKyKTi4\n" +
                        "GEQEiSET5UcRMFgUeI6U3fLPKnmSer4qZP8/G9IcvpVgCOzW6foMZ8mbO+n/Jqs4\n" +
                        "644slRlBNYor/5tl5f6sYy5Hyzrj4c6Tq2Duzu0VECQnaTOCl7QyW8Vc1R2qferO\n" +
                        "4Rs94InVWfNn5ltV7OPHLBSNAZ8YRILpafrWw+EZbrE5+hwlKernpdn6dRAG668s\n" +
                        "KyASsXjtGfPUlcYtFvJQS2U/gAsGcQPPL9g4x8FL2jRqDI92EU8Cw+G2HKlqNegP\n" +
                        "6vNkfGv4/LRCtQ+KrajFcSyqrjmZV8lohCI3qJowtK5nFN3Z+5Kk2jqVgRHYuXhR\n" +
                        "uCcQrwHOvVts+POHWqbPOR3VDaGWS40rqAwaJrko92IOxhEpUBmNnpH2dARKs1AB\n" +
                        "itiWpWNkbalgbEDBx4mmdcj4KsVF5Q86xfg3n8zUQAqhoOKwll5wRQ11lQOXca6O\n" +
                        "GsPI+A/j12owLSxOez//\n" +
                        "=Umj+\n" +
                        "-----END PGP SIGNATURE-----\n" +
                        "',0,1453380785000,'<20160121125302.GF31046@littlepip>','Hash: SHA1 this msg is only signed~ Version: GnuPG v1 iQIcBAEBAgAGBQJWoNSuAAoJEHvRgyDerfoROjkQAK2Md7CE4GDcHaWppXUttUeh wrjbnW2McJSysjWmb6FYt1CYsjl+3vImIgqg59rZxjdaffs+lNxO3B0blfAOMjSs LOCJytbB02/f79e44kWWt5ZG0d+3NTl8sN4OkXb47fot28CG7JLJgkGpMbmwm6sM C5pUA4o/OwWbkg2xj2FUDmgx4clyA9BxEBxO1ZU+VFLawtZ6OdRLF8iKzJKyKTi4 GEQEiSET5UcRMFgUeI6U3fLPKnmSer4qZP8/G9IcvpVgCOzW6foMZ8mbO+n/Jqs4 644slRlBNYor/5tl5f6sYy5Hyzrj4c6Tq2Duzu0VECQnaTOCl7QyW8Vc1R2qferO 4Rs94InVWfNn5ltV7OPHLBSNAZ8YRILpafrWw+EZbrE5+hwlKernpdn6dRAG668s KyA','text/plain',NULL,0,1,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (7,'Return-Path','<look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'X-Original-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Delivered-To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Date','Thu, 21 Jan 2016 13:53:02 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'From','Vincent Breitmoser <look@my.amazin.horse>')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'To','valodim@mugenguild.com')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Subject','pgp/inline clearsigned')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Message-ID','<20160121125302.GF31046@littlepip>')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'User-Agent','Mutt/1.5.24 (2015-08-30)')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Content-Type','text/plain\n charset=utf-8')",
                "INSERT INTO headers (message_id, name, value) VALUES (7,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(8,7,8,NULL)",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }


    @Test
    public void migratePgpInlineClearsignedMessage() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertPgpInlineClearsignedMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("8");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(7, msg.getDatabaseId());
        Assert.assertEquals(12, msg.getHeaderNames().size());
        Assert.assertEquals("text/plain", msg.getMimeType());
        Assert.assertEquals(0, msg.getAttachmentCount());
        Assert.assertTrue(msg.getBody() instanceof BinaryMemoryBody);

        String msgTextContent = MessageExtractor.getTextFromPart(msg);
        Assert.assertEquals(OpenPgpUtils.PARSE_RESULT_SIGNED_MESSAGE, OpenPgpUtils.parseMessage(msgTextContent));
    }

    private void insertMultipartAlternativeMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(8,0,16,'9','mail with html multipart',1453922449000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','jh@example.org;','look@my.amazin.horse'," +
                        "'','','','<html>\n" +
                        "  <head>\n" +
                        "\n" +
                        "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n" +
                        "  </head>\n" +
                        "  <body text=\"#000000\" bgcolor=\"#FFFFFF\">\n" +
                        "    <i>this</i> is an <b>HTML-<u>E-MAIL</u></b>.<br>\n" +
                        "  </body>\n" +
                        "</html>\n" +
                        "','this is an *HTML-_E-MAIL_*.\n" +
                        "',0,1453922455000,'<56A91891.7010509@example.org>'," +
                        "'this is an HTML-E-Mail.','multipart/alternative',NULL,0,0,0,0,0)",

                "INSERT INTO headers (message_id, name, value) VALUES (8,'To','look@my.amazin.horse')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'From','=?UTF-8?Q?Jan_H=c3=benbecker?= <jh@example.org>')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'Subject','mail with html multipart')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'Message-ID','<56A91891.7010509@example.org>')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'Date','Wed, 27 Jan 2016 20:20:49 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'User-Agent','Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Icedove/38.5.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'Content-Type','multipart/alternative; boundary=\"------------060200010509000000040004\"')",
                "INSERT INTO headers (message_id, name, value) VALUES (8,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(9,8,9,NULL)",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migrateTextHtml() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertMultipartAlternativeMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("9");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(8, msg.getDatabaseId());
        Assert.assertEquals(9, msg.getHeaderNames().size());
        Assert.assertEquals("multipart/alternative", msg.getMimeType());
        Assert.assertEquals(0, msg.getAttachmentCount());

        Multipart msgBody = (Multipart) msg.getBody();
        Assert.assertEquals("------------060200010509000000040004", msgBody.getBoundary());
    }

    private void insertHtmlWithRelatedMessage(SQLiteDatabase db) {
        String[] statements = new String[] {
                "INSERT INTO messages VALUES(9,0,16,'10','html with multipart/related content',1453922845000," +
                        "'X_GOT_ALL_HEADERS,X_DOWNLOADED_FULL','jh@example.org;','look@my.amazin.horse'," +
                        "'','','','<html>\n" +
                        "  <head>\n" +
                        "\n" +
                        "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">\n" +
                        "  </head>\n" +
                        "  <body text=\"#000000\" bgcolor=\"#FFFFFF\">\n" +
                        "    <blink>html text with inline attachment</blink><br>\n" +
                        "    <img alt=\"Alternative_Text\" src=\"content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/6/RAW\"\n" +
                        "      height=\"177\" width=\"220\">\n" +
                        "  </body>\n" +
                        "</html>\n" +
                        "','html text with inline attachment\n" +
                        "\n" +
                        "Alternative_Text\n" +
                        "',1,1453922852000,'<56A91A1D.7050908@example.org>','html text with inline attachment Alternative_Text','multipart/alternative',NULL,0,1,0,0,0);\n",

                "INSERT INTO headers (message_id, name, value) VALUES(9,'Return-Path','<jh@example.org>')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'X-Original-To','look@my.amazin.horse')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'To','look@my.amazin.horse')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'From','=?UTF-8?Q?Jan_H=c3=benbecker?= <jh@example.org>')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'Subject','html with multipart/related content')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'Message-ID','<56A91A1D.7050908@example.org>')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'Date','Wed, 27 Jan 2016 20:27:25 +0100')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'User-Agent','Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Icedove/38.5.0')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'MIME-Version','1.0')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'Content-Type','multipart/alternative; boundary=\"------------050707070308090509030605\"')",
                "INSERT INTO headers (message_id, name, value) VALUES(9,'Content-Transfer-Encoding','8bit')",

                "INSERT INTO threads VALUES(10,9,10,NULL)",

                "INSERT INTO attachments VALUES(6,9,NULL,'content://com.fsck.k9.attachmentprovider/" + account.getUuid() + "/6/RAW'," +
                        "8503,'attached.jpg','image/jpeg','part1.07090108.09020601@example.org','inline')",
        };

        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Test
    public void migrateHtmlWithRelatedMessage() throws Exception {
        SQLiteDatabase db = createV50Database();
        insertHtmlWithRelatedMessage(db);
        db.close();

        LocalStore localStore = LocalStore.getInstance(account, RuntimeEnvironment.application);

        LocalMessage msg = localStore.getFolder("dev").getMessage("10");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        localStore.getFolder("dev").fetch(Collections.singletonList(msg), fp, null);

        Assert.assertEquals(9, msg.getDatabaseId());
        Assert.assertEquals(11, msg.getHeaderNames().size());
        Assert.assertEquals("multipart/mixed", msg.getMimeType());
        Assert.assertEquals(1, msg.getAttachmentCount());

        Multipart msgBody = (Multipart) msg.getBody();
        Assert.assertEquals("------------050707070308090509030605", msgBody.getBoundary());

        Multipart multipartAlternativePart = (Multipart) msgBody.getBodyPart(0).getBody();
        BodyPart htmlPart = multipartAlternativePart.getBodyPart(1);
        String msgTextContent = MessageExtractor.getTextFromPart(htmlPart);
        Assert.assertNotNull(msgTextContent);
        Assert.assertTrue(msgTextContent.contains("cid:part1.07090108.09020601@example.org"));

        Assert.assertEquals("image/jpeg", msgBody.getBodyPart(1).getMimeType());
    }

    private void copyAttachmentFromFile(String resourceName, int attachmentId, int expectedFilesize) throws IOException {
        File resourceFile = new File(getClass().getResource("/attach/" + resourceName).getFile());
        File attachmentFile = new File(attachmentDir, Integer.toString(attachmentId));
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(resourceFile));
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(attachmentFile));
        int copied = IOUtils.copy(input, output);
        input.close();
        output.close();
        Assert.assertEquals(expectedFilesize, copied);
    }

    private Account getNewAccount() {
        Preferences preferences = Preferences.getPreferences(RuntimeEnvironment.application);

        //FIXME: This is a hack to get Preferences into a state where it's safe to call newAccount()
        preferences.loadAccounts();

        return preferences.newAccount();
    }
}
