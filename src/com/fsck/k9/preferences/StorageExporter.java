package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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


public class StorageExporter {
    private static final String EXPORT_FILENAME = "settings.k9s";

    private static final String ROOT_ELEMENT = "k9settings";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String SETTINGS_ELEMENT = "settings";
    private static final String ACCOUNTS_ELEMENT = "accounts";
    private static final String ACCOUNT_ELEMENT = "account";
    private static final String IDENTITIES_ELEMENT = "identities";
    private static final String IDENTITY_ELEMENT = "identity";
    private static final String UUID_ATTRIBUTE = "uuid";
    private static final String VALUE_ELEMENT = "value";
    private static final String KEY_ATTRIBUTE = "key";


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
            exportPreferences(context, os, includeGlobals, accountUuids);

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

            Map<String, ? extends Object> prefs = storage.getAll();

            if (includeGlobals) {
                serializer.startTag(null, SETTINGS_ELEMENT);
                writeSettings(serializer, prefs);
                serializer.endTag(null, SETTINGS_ELEMENT);
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
            Map<String, ? extends Object> prefs) throws IOException {

        for (Map.Entry<String, ? extends Object> entry : prefs.entrySet()) {
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
            Map<String, ? extends Object> prefs) throws IOException {

        Set<String> identities = new HashSet<String>();

        serializer.startTag(null, ACCOUNT_ELEMENT);
        serializer.attribute(null, UUID_ATTRIBUTE, accountUuid);

        serializer.startTag(null, SETTINGS_ELEMENT);
        for (Map.Entry<String, ? extends Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            String[] comps = key.split("\\.");
            if (comps.length >= 2) {
                String keyUuid = comps[0];
                if (!keyUuid.equals(accountUuid)) {
                    continue;
                }
                if (comps.length == 3) {
                    String identityKey = comps[1];
                    String identityIndex = comps[2];

                    if (Account.IDENTITY_KEYS.contains(identityKey)) {
                        // This is an identity key. Save identity index for later...
                        identities.add(identityIndex);
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
            for (String identityIndex : identities) {
                writeIdentity(serializer, accountUuid, identityIndex, prefs);
            }
            serializer.endTag(null, IDENTITIES_ELEMENT);
        }

        serializer.endTag(null, ACCOUNT_ELEMENT);
    }

    private static void writeIdentity(XmlSerializer serializer, String accountUuid,
            String identity, Map<String, ? extends Object> prefs) throws IOException {

        serializer.startTag(null, IDENTITY_ELEMENT);
        for (Map.Entry<String, ? extends Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            String[] comps = key.split("\\.");
            if (comps.length >= 3) {
                String keyUuid = comps[0];
                String identityKey = comps[1];
                String identityIndex = comps[2];
                if (!keyUuid.equals(accountUuid) || !identityIndex.equals(identity)
                        || !Account.IDENTITY_KEYS.contains(identityKey)) {
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
        serializer.endTag(null, IDENTITY_ELEMENT);
    }
}
