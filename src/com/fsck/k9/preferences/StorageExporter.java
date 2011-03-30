package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.crypto.CipherOutputStream;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.store.LocalStore;


public class StorageExporter {
    private static final String EXPORT_FILENAME = "settings.k9s";

    private static final String ROOT_ELEMENT = "k9settings";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String GLOBAL_ELEMENT = "global";
    private static final String SETTINGS_ELEMENT = "settings";
    private static final String ACCOUNTS_ELEMENT = "accounts";
    private static final String ACCOUNT_ELEMENT = "account";
    private static final String UUID_ATTRIBUTE = "uuid";
    private static final String IDENTITIES_ELEMENT = "identities";
    private static final String IDENTITY_ELEMENT = "identity";
    private static final String FOLDERS_ELEMENT = "folders";
    private static final String FOLDER_ELEMENT = "folder";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ELEMENT = "value";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String NAME_ELEMENT = "name";
    private static final String EMAIL_ELEMENT = "email";
    private static final String DESCRIPTION_ELEMENT = "description";


    public static String exportToFile(Context context, boolean includeGlobals,
            Set<String> accountUuids, String encryptionKey)
            throws StorageImportExportException {

        OutputStream os = null;
        try
        {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator
                                + context.getPackageName());
            dir.mkdirs();
            File file = Utility.createUniqueFile(dir, EXPORT_FILENAME);
            String fileName = file.getAbsolutePath();
            os = new FileOutputStream(fileName);

            if (encryptionKey == null) {
                exportPreferences(context, os, includeGlobals, accountUuids);
            } else {
                exportPreferencesEncrypted(context, os, includeGlobals, accountUuids,
                        encryptionKey);
            }

            // If all went well, we return the name of the file just written.
            return fileName;
        } catch (Exception e) {
            throw new StorageImportExportException();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {}
            }
        }
    }

    public static void exportPreferencesEncrypted(Context context, OutputStream os, boolean includeGlobals,
            Set<String> accountUuids, String encryptionKey) throws StorageImportExportException  {

        try {
            K9Krypto k = new K9Krypto(encryptionKey, K9Krypto.MODE.ENCRYPT);
            CipherOutputStream cos = new CipherOutputStream(os, k.mCipher);

            exportPreferences(context, cos, includeGlobals, accountUuids);
        } catch (Exception e) {
            throw new StorageImportExportException();
        }
    }

    public static void exportPreferences(Context context, OutputStream os, boolean includeGlobals,
            Set<String> accountUuids) throws StorageImportExportException  {

        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(os, "UTF-8");

            serializer.startDocument(null, Boolean.valueOf(true));

            // Output with indentation
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(null, ROOT_ELEMENT);
            serializer.attribute(null, VERSION_ATTRIBUTE, "x");

            Log.i(K9.LOG_TAG, "Exporting preferences");

            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();

            if (accountUuids == null) {
                Account[] accounts = preferences.getAccounts();
                accountUuids = new HashSet<String>();
                for (Account account : accounts) {
                    accountUuids.add(account.getUuid());
                }
            }

            Map<String, Object> prefs = new TreeMap<String, Object>(storage.getAll());

            if (includeGlobals) {
                serializer.startTag(null, GLOBAL_ELEMENT);
                writeSettings(serializer, prefs);
                serializer.endTag(null, GLOBAL_ELEMENT);
            }

            serializer.startTag(null, ACCOUNTS_ELEMENT);
            for (String accountUuid : accountUuids) {
                writeAccount(serializer, accountUuid, prefs);
            }
            serializer.endTag(null, ACCOUNTS_ELEMENT);

            serializer.endTag(null, ROOT_ELEMENT);
            serializer.endDocument();
            serializer.flush();

        } catch (Exception e) {
            throw new StorageImportExportException(e.getLocalizedMessage(), e);
        }
    }

    private static void writeSettings(XmlSerializer serializer,
            Map<String, Object> prefs) throws IOException {

        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            if (key.indexOf('.') != -1) {
                // Skip account entries
                continue;
            }
            serializer.startTag(null, VALUE_ELEMENT);
            serializer.attribute(null, KEY_ATTRIBUTE, key);
            serializer.text(value);
            serializer.endTag(null, VALUE_ELEMENT);
        }
    }

    private static void writeAccount(XmlSerializer serializer, String accountUuid,
            Map<String, Object> prefs) throws IOException {

        Set<Integer> identities = new HashSet<Integer>();
        Set<String> folders = new HashSet<String>();

        serializer.startTag(null, ACCOUNT_ELEMENT);
        serializer.attribute(null, UUID_ATTRIBUTE, accountUuid);

        String name = (String) prefs.get(accountUuid + "." + Account.ACCOUNT_DESCRIPTION_KEY);
        if (name != null) {
            serializer.startTag(null, NAME_ELEMENT);
            serializer.text(name);
            serializer.endTag(null, NAME_ELEMENT);
        }

        serializer.startTag(null, SETTINGS_ELEMENT);
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            String[] comps = key.split("\\.");
            if (comps.length >= 2) {
                String keyUuid = comps[0];
                String secondPart = comps[1];

                if (!keyUuid.equals(accountUuid)
                        || Account.ACCOUNT_DESCRIPTION_KEY.equals(secondPart)) {
                    continue;
                }
                if (comps.length == 3) {
                    String thirdPart = comps[2];

                    if (Account.IDENTITY_KEYS.contains(secondPart)) {
                        // This is an identity key. Save identity index for later...
                        try {
                            identities.add(Integer.parseInt(thirdPart));
                        } catch (NumberFormatException e) { /* ignore */ }
                        // ... but don't write it now.
                        continue;
                    }

                    if (LocalStore.FOLDER_SETTINGS_KEYS.contains(thirdPart)) {
                        // This is a folder key. Save folder name for later...
                        folders.add(secondPart);
                        // ... but don't write it now.
                        continue;
                    }
                }
            } else {
                // Skip global config entries and identity entries
                continue;
            }

            // Strip account UUID from key
            String keyPart = key.substring(comps[0].length() + 1);

            serializer.startTag(null, VALUE_ELEMENT);
            serializer.attribute(null, KEY_ATTRIBUTE, keyPart);
            serializer.text(value);
            serializer.endTag(null, VALUE_ELEMENT);
        }
        serializer.endTag(null, SETTINGS_ELEMENT);

        if (identities.size() > 0) {
            serializer.startTag(null, IDENTITIES_ELEMENT);

            // Sort identity indices (that's why we store them as Integers)
            List<Integer> sortedIdentities = new ArrayList<Integer>(identities);
            Collections.sort(sortedIdentities);

            for (Integer identityIndex : sortedIdentities) {
                writeIdentity(serializer, accountUuid, identityIndex.toString(), prefs);
            }
            serializer.endTag(null, IDENTITIES_ELEMENT);
        }

        if (folders.size() > 0) {
            serializer.startTag(null, FOLDERS_ELEMENT);
            for (String folder : folders) {
                writeFolder(serializer, accountUuid, folder, prefs);
            }
            serializer.endTag(null, FOLDERS_ELEMENT);
        }

        serializer.endTag(null, ACCOUNT_ELEMENT);
    }

    private static void writeIdentity(XmlSerializer serializer, String accountUuid,
            String identity, Map<String, Object> prefs) throws IOException {

        serializer.startTag(null, IDENTITY_ELEMENT);

        String name = (String) prefs.get(accountUuid + "." + Account.IDENTITY_NAME_KEY +
                "." + identity);
        serializer.startTag(null, NAME_ELEMENT);
        serializer.text(name);
        serializer.endTag(null, NAME_ELEMENT);

        String email = (String) prefs.get(accountUuid + "." + Account.IDENTITY_EMAIL_KEY +
                "." + identity);
        serializer.startTag(null, EMAIL_ELEMENT);
        serializer.text(email);
        serializer.endTag(null, EMAIL_ELEMENT);

        String description = (String) prefs.get(accountUuid + "." +
                Account.IDENTITY_DESCRIPTION_KEY + "." + identity);
        if (description != null) {
            serializer.startTag(null, DESCRIPTION_ELEMENT);
            serializer.text(description);
            serializer.endTag(null, DESCRIPTION_ELEMENT);
        }

        serializer.startTag(null, SETTINGS_ELEMENT);
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            String[] comps = key.split("\\.");
            if (comps.length >= 3) {
                String keyUuid = comps[0];
                String identityKey = comps[1];
                String identityIndex = comps[2];
                if (!keyUuid.equals(accountUuid) || !identityIndex.equals(identity)
                        || !Account.IDENTITY_KEYS.contains(identityKey)
                        || Account.IDENTITY_NAME_KEY.equals(identityKey)
                        || Account.IDENTITY_EMAIL_KEY.equals(identityKey)
                        || Account.IDENTITY_DESCRIPTION_KEY.equals(identityKey)) {
                    continue;
                }
            } else {
                // Skip non-identity config entries
                continue;
            }

            serializer.startTag(null, VALUE_ELEMENT);
            serializer.attribute(null, KEY_ATTRIBUTE, comps[1]);
            serializer.text(value);
            serializer.endTag(null, VALUE_ELEMENT);
        }
        serializer.endTag(null, SETTINGS_ELEMENT);

        serializer.endTag(null, IDENTITY_ELEMENT);
    }

    private static void writeFolder(XmlSerializer serializer, String accountUuid,
            String folder, Map<String, Object> prefs) throws IOException {

        serializer.startTag(null, FOLDER_ELEMENT);
        serializer.attribute(null, NAME_ATTRIBUTE, folder);
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            String[] comps = key.split("\\.");
            if (comps.length >= 3) {
                String keyUuid = comps[0];
                String folderName = comps[1];
                String folderKey = comps[2];
                if (!keyUuid.equals(accountUuid) || !folderName.equals(folder)
                        || !LocalStore.FOLDER_SETTINGS_KEYS.contains(folderKey)) {
                    continue;
                }
            } else {
                // Skip non-folder config entries
                continue;
            }

            serializer.startTag(null, VALUE_ELEMENT);
            serializer.attribute(null, KEY_ATTRIBUTE, comps[2]);
            serializer.text(value);
            serializer.endTag(null, VALUE_ELEMENT);
        }
        serializer.endTag(null, FOLDER_ELEMENT);
    }
}
