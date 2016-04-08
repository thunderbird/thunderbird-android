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
import java.util.Map.Entry;

import com.fsck.k9.helper.FileHelper;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import com.fsck.k9.preferences.Settings.SettingsDescription;


public class SettingsExporter {
    private static final String EXPORT_FILENAME = "settings.k9s";

    /**
     * File format version number.
     *
     * <p>
     * Increment this if you need to change the structure of the settings file. When you do this
     * remember that we also have to be able to handle old file formats. So have fun adding support
     * for that to {@link SettingsImporter} :)
     * </p>
     */
    public static final int FILE_FORMAT_VERSION = 1;

    public static final String ROOT_ELEMENT = "k9settings";
    public static final String VERSION_ATTRIBUTE = "version";
    public static final String FILE_FORMAT_ATTRIBUTE = "format";
    public static final String GLOBAL_ELEMENT = "global";
    public static final String SETTINGS_ELEMENT = "settings";
    public static final String ACCOUNTS_ELEMENT = "accounts";
    public static final String ACCOUNT_ELEMENT = "account";
    public static final String UUID_ATTRIBUTE = "uuid";
    public static final String INCOMING_SERVER_ELEMENT = "incoming-server";
    public static final String OUTGOING_SERVER_ELEMENT = "outgoing-server";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String HOST_ELEMENT = "host";
    public static final String PORT_ELEMENT = "port";
    public static final String CONNECTION_SECURITY_ELEMENT = "connection-security";
    public static final String AUTHENTICATION_TYPE_ELEMENT = "authentication-type";
    public static final String USERNAME_ELEMENT = "username";
    public static final String CLIENT_CERTIFICATE_ALIAS_ELEMENT = "client-cert-alias";
    public static final String PASSWORD_ELEMENT = "password";
    public static final String EXTRA_ELEMENT = "extra";
    public static final String IDENTITIES_ELEMENT = "identities";
    public static final String IDENTITY_ELEMENT = "identity";
    public static final String FOLDERS_ELEMENT = "folders";
    public static final String FOLDER_ELEMENT = "folder";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String VALUE_ELEMENT = "value";
    public static final String KEY_ATTRIBUTE = "key";
    public static final String NAME_ELEMENT = "name";
    public static final String EMAIL_ELEMENT = "email";
    public static final String DESCRIPTION_ELEMENT = "description";


    public static String exportToFile(Context context, boolean includeGlobals,
            Set<String> accountUuids)
            throws SettingsImportExportException {

        OutputStream os = null;
        String filename = null;
        try
        {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator + context.getPackageName());
            if (!dir.mkdirs()) {
                Log.d(K9.LOG_TAG, "Unable to create directory: " + dir.getAbsolutePath());
            }

            File file = FileHelper.createUniqueFile(dir, EXPORT_FILENAME);
            filename = file.getAbsolutePath();
            os = new FileOutputStream(filename);

            exportPreferences(context, os, includeGlobals, accountUuids);

            // If all went well, we return the name of the file just written.
            return filename;
        } catch (Exception e) {
            throw new SettingsImportExportException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    Log.w(K9.LOG_TAG, "Couldn't close exported settings file: " + filename);
                }
            }
        }
    }

    public static void exportPreferences(Context context, OutputStream os, boolean includeGlobals,
            Set<String> accountUuids) throws SettingsImportExportException  {

        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(os, "UTF-8");

            serializer.startDocument(null, Boolean.TRUE);

            // Output with indentation
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(null, ROOT_ELEMENT);
            serializer.attribute(null, VERSION_ATTRIBUTE, Integer.toString(Settings.VERSION));
            serializer.attribute(null, FILE_FORMAT_ATTRIBUTE,
                    Integer.toString(FILE_FORMAT_VERSION));

            Log.i(K9.LOG_TAG, "Exporting preferences");

            Preferences preferences = Preferences.getPreferences(context);
            Storage storage = preferences.getStorage();

            Set<String> exportAccounts;
            if (accountUuids == null) {
                List<Account> accounts = preferences.getAccounts();
                exportAccounts = new HashSet<String>();
                for (Account account : accounts) {
                    exportAccounts.add(account.getUuid());
                }
            } else {
                exportAccounts = accountUuids;
            }

            Map<String, Object> prefs = new TreeMap<String, Object>(storage.getAll());

            if (includeGlobals) {
                serializer.startTag(null, GLOBAL_ELEMENT);
                writeSettings(serializer, prefs);
                serializer.endTag(null, GLOBAL_ELEMENT);
            }

            serializer.startTag(null, ACCOUNTS_ELEMENT);
            for (String accountUuid : exportAccounts) {
                Account account = preferences.getAccount(accountUuid);
                writeAccount(serializer, account, prefs);
            }
            serializer.endTag(null, ACCOUNTS_ELEMENT);

            serializer.endTag(null, ROOT_ELEMENT);
            serializer.endDocument();
            serializer.flush();

        } catch (Exception e) {
            throw new SettingsImportExportException(e.getLocalizedMessage(), e);
        }
    }

    private static void writeSettings(XmlSerializer serializer,
            Map<String, Object> prefs) throws IOException {

        for (Entry<String, TreeMap<Integer, SettingsDescription>> versionedSetting :
                GlobalSettings.SETTINGS.entrySet()) {

            String key = versionedSetting.getKey();
            String valueString = (String) prefs.get(key);
            TreeMap<Integer, SettingsDescription> versions = versionedSetting.getValue();
            Integer highestVersion = versions.lastKey();
            SettingsDescription setting = versions.get(highestVersion);
            if (setting == null) {
                // Setting was removed.
                continue;
            }

            if (valueString != null) {
                try {
                    Object value = setting.fromString(valueString);
                    String outputValue = setting.toPrettyString(value);
                    writeKeyValue(serializer, key, outputValue);
                } catch (InvalidSettingValueException e) {
                    Log.w(K9.LOG_TAG, "Global setting \"" + key  + "\" has invalid value \"" +
                            valueString + "\" in preference storage. This shouldn't happen!");
                }
            } else {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Couldn't find key \"" + key + "\" in preference storage." +
                            "Using default value.");
                }

                Object value = setting.getDefaultValue();
                String outputValue = setting.toPrettyString(value);
                writeKeyValue(serializer, key, outputValue);
            }
        }
    }

    private static void writeAccount(XmlSerializer serializer, Account account,
            Map<String, Object> prefs) throws IOException {

        Set<Integer> identities = new HashSet<Integer>();
        Set<String> folders = new HashSet<String>();
        String accountUuid = account.getUuid();

        serializer.startTag(null, ACCOUNT_ELEMENT);
        serializer.attribute(null, UUID_ATTRIBUTE, accountUuid);

        String name = (String) prefs.get(accountUuid + "." + Account.ACCOUNT_DESCRIPTION_KEY);
        if (name != null) {
            serializer.startTag(null, NAME_ELEMENT);
            serializer.text(name);
            serializer.endTag(null, NAME_ELEMENT);
        }


        // Write incoming server settings
        ServerSettings incoming = RemoteStore.decodeStoreUri(account.getStoreUri());
        serializer.startTag(null, INCOMING_SERVER_ELEMENT);
        serializer.attribute(null, TYPE_ATTRIBUTE, incoming.type.name());

        writeElement(serializer, HOST_ELEMENT, incoming.host);
        if (incoming.port != -1) {
            writeElement(serializer, PORT_ELEMENT, Integer.toString(incoming.port));
        }
        if (incoming.connectionSecurity != null) {
            writeElement(serializer, CONNECTION_SECURITY_ELEMENT, incoming.connectionSecurity.name());
        }
        if (incoming.authenticationType != null) {
            writeElement(serializer, AUTHENTICATION_TYPE_ELEMENT, incoming.authenticationType.name());
        }
        writeElement(serializer, USERNAME_ELEMENT, incoming.username);
        writeElement(serializer, CLIENT_CERTIFICATE_ALIAS_ELEMENT, incoming.clientCertificateAlias);
        // XXX For now we don't export the password
        //writeElement(serializer, PASSWORD_ELEMENT, incoming.password);

        Map<String, String> extras = incoming.getExtra();
        if (extras != null && extras.size() > 0) {
            serializer.startTag(null, EXTRA_ELEMENT);
            for (Entry<String, String> extra : extras.entrySet()) {
                writeKeyValue(serializer, extra.getKey(), extra.getValue());
            }
            serializer.endTag(null, EXTRA_ELEMENT);
        }

        serializer.endTag(null, INCOMING_SERVER_ELEMENT);


        // Write outgoing server settings
        ServerSettings outgoing = Transport.decodeTransportUri(account.getTransportUri());
        serializer.startTag(null, OUTGOING_SERVER_ELEMENT);
        serializer.attribute(null, TYPE_ATTRIBUTE, outgoing.type.name());

        writeElement(serializer, HOST_ELEMENT, outgoing.host);
        if (outgoing.port != -1) {
            writeElement(serializer, PORT_ELEMENT, Integer.toString(outgoing.port));
        }
        if (outgoing.connectionSecurity != null) {
            writeElement(serializer, CONNECTION_SECURITY_ELEMENT, outgoing.connectionSecurity.name());
        }
        if (outgoing.authenticationType != null) {
            writeElement(serializer, AUTHENTICATION_TYPE_ELEMENT, outgoing.authenticationType.name());
        }
        writeElement(serializer, USERNAME_ELEMENT, outgoing.username);
        writeElement(serializer, CLIENT_CERTIFICATE_ALIAS_ELEMENT, outgoing.clientCertificateAlias);
        // XXX For now we don't export the password
        //writeElement(serializer, PASSWORD_ELEMENT, outgoing.password);

        extras = outgoing.getExtra();
        if (extras != null && extras.size() > 0) {
            serializer.startTag(null, EXTRA_ELEMENT);
            for (Entry<String, String> extra : extras.entrySet()) {
                writeKeyValue(serializer, extra.getKey(), extra.getValue());
            }
            serializer.endTag(null, EXTRA_ELEMENT);
        }

        serializer.endTag(null, OUTGOING_SERVER_ELEMENT);


        // Write account settings
        serializer.startTag(null, SETTINGS_ELEMENT);
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String valueString = entry.getValue().toString();
            String[] comps = key.split("\\.", 2);

            if (comps.length < 2) {
                // Skip global settings
                continue;
            }

            String keyUuid = comps[0];
            String keyPart = comps[1];

            if (!keyUuid.equals(accountUuid)) {
                // Setting doesn't belong to the account we're currently writing.
                continue;
            }

            int indexOfLastDot = keyPart.lastIndexOf(".");
            boolean hasThirdPart = indexOfLastDot != -1 && indexOfLastDot < keyPart.length() - 1;
            if (hasThirdPart) {
                String secondPart = keyPart.substring(0, indexOfLastDot);
                String thirdPart = keyPart.substring(indexOfLastDot + 1);

                if (Account.IDENTITY_DESCRIPTION_KEY.equals(secondPart)) {
                    // This is an identity key. Save identity index for later...
                    try {
                        identities.add(Integer.parseInt(thirdPart));
                    } catch (NumberFormatException e) { /* ignore */ }
                    // ... but don't write it now.
                    continue;
                }

                if (FolderSettings.SETTINGS.containsKey(thirdPart)) {
                    // This is a folder key. Save folder name for later...
                    folders.add(secondPart);
                    // ... but don't write it now.
                    continue;
                }
            }

            TreeMap<Integer, SettingsDescription> versionedSetting =
                AccountSettings.SETTINGS.get(keyPart);

            if (versionedSetting != null) {
                Integer highestVersion = versionedSetting.lastKey();
                SettingsDescription setting = versionedSetting.get(highestVersion);

                if (setting != null) {
                    // Only export account settings that can be found in AccountSettings.SETTINGS
                    try {
                        Object value = setting.fromString(valueString);
                        String pretty = setting.toPrettyString(value);
                        writeKeyValue(serializer, keyPart, pretty);
                    } catch (InvalidSettingValueException e) {
                        Log.w(K9.LOG_TAG, "Account setting \"" + keyPart + "\" (" +
                                account.getDescription() + ") has invalid value \"" + valueString +
                                "\" in preference storage. This shouldn't happen!");
                    }
                }
            }
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

        String prefix = accountUuid + ".";
        String suffix = "." + identity;

        // Write name belonging to the identity
        String name = (String) prefs.get(prefix + Account.IDENTITY_NAME_KEY + suffix);
        serializer.startTag(null, NAME_ELEMENT);
        serializer.text(name);
        serializer.endTag(null, NAME_ELEMENT);

        // Write email address belonging to the identity
        String email = (String) prefs.get(prefix + Account.IDENTITY_EMAIL_KEY + suffix);
        serializer.startTag(null, EMAIL_ELEMENT);
        serializer.text(email);
        serializer.endTag(null, EMAIL_ELEMENT);

        // Write identity description
        String description = (String) prefs.get(prefix + Account.IDENTITY_DESCRIPTION_KEY + suffix);
        if (description != null) {
            serializer.startTag(null, DESCRIPTION_ELEMENT);
            serializer.text(description);
            serializer.endTag(null, DESCRIPTION_ELEMENT);
        }

        // Write identity settings
        serializer.startTag(null, SETTINGS_ELEMENT);
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String valueString = entry.getValue().toString();
            String[] comps = key.split("\\.");

            if (comps.length < 3) {
                // Skip non-identity config entries
                continue;
            }

            String keyUuid = comps[0];
            String identityKey = comps[1];
            String identityIndex = comps[2];
            if (!keyUuid.equals(accountUuid) || !identityIndex.equals(identity)) {
                // Skip entries that belong to another identity
                continue;
            }

            TreeMap<Integer, SettingsDescription> versionedSetting =
                IdentitySettings.SETTINGS.get(identityKey);

            if (versionedSetting != null) {
                Integer highestVersion = versionedSetting.lastKey();
                SettingsDescription setting = versionedSetting.get(highestVersion);

                if (setting != null) {
                    // Only write settings that have an entry in IdentitySettings.SETTINGS
                    try {
                        Object value = setting.fromString(valueString);
                        String outputValue = setting.toPrettyString(value);
                        writeKeyValue(serializer, identityKey, outputValue);
                    } catch (InvalidSettingValueException e) {
                        Log.w(K9.LOG_TAG, "Identity setting \"" + identityKey +
                                "\" has invalid value \"" + valueString +
                                "\" in preference storage. This shouldn't happen!");
                    }
                }
            }
        }
        serializer.endTag(null, SETTINGS_ELEMENT);

        serializer.endTag(null, IDENTITY_ELEMENT);
    }

    private static void writeFolder(XmlSerializer serializer, String accountUuid,
            String folder, Map<String, Object> prefs) throws IOException {

        serializer.startTag(null, FOLDER_ELEMENT);
        serializer.attribute(null, NAME_ATTRIBUTE, folder);

        // Write folder settings
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            String key = entry.getKey();
            String valueString = entry.getValue().toString();
            int indexOfFirstDot = key.indexOf('.');
            int indexOfLastDot = key.lastIndexOf('.');

            if (indexOfFirstDot == -1 || indexOfLastDot == -1 || indexOfFirstDot == indexOfLastDot) {
                // Skip non-folder config entries
                continue;
            }

            String keyUuid = key.substring(0, indexOfFirstDot);
            String folderName = key.substring(indexOfFirstDot + 1, indexOfLastDot);
            String folderKey = key.substring(indexOfLastDot + 1);

            if (!keyUuid.equals(accountUuid) || !folderName.equals(folder)) {
                // Skip entries that belong to another folder
                continue;
            }

            TreeMap<Integer, SettingsDescription> versionedSetting =
                FolderSettings.SETTINGS.get(folderKey);

            if (versionedSetting != null) {
                Integer highestVersion = versionedSetting.lastKey();
                SettingsDescription setting = versionedSetting.get(highestVersion);

                if (setting != null) {
                    // Only write settings that have an entry in FolderSettings.SETTINGS
                    try {
                        Object value = setting.fromString(valueString);
                        String outputValue = setting.toPrettyString(value);
                        writeKeyValue(serializer, folderKey, outputValue);
                    } catch (InvalidSettingValueException e) {
                        Log.w(K9.LOG_TAG, "Folder setting \"" + folderKey +
                                "\" has invalid value \"" + valueString +
                                "\" in preference storage. This shouldn't happen!");
                    }
                }
            }
        }

        serializer.endTag(null, FOLDER_ELEMENT);
    }

    private static void writeElement(XmlSerializer serializer, String elementName, String value)
            throws IllegalArgumentException, IllegalStateException, IOException {
        if (value != null) {
            serializer.startTag(null, elementName);
            serializer.text(value);
            serializer.endTag(null, elementName);
        }
    }

    private static void writeKeyValue(XmlSerializer serializer, String key, String value)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, VALUE_ELEMENT);
        serializer.attribute(null, KEY_ATTRIBUTE, key);
        if (value != null) {
            serializer.text(value);
        }
        serializer.endTag(null, VALUE_ELEMENT);
    }
}
