package com.fsck.k9.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.DateFormatter;
import com.fsck.k9.helper.Utility;

public class StorageImporter {

    /**
     * Class to list the contents of an import file/stream.
     *
     * @see StorageImporter#getImportStreamContents(Context,InputStream,String)
     */
    public static class ImportContents {
        /**
         * True, if the import file contains global settings.
         */
        public final boolean globalSettings;

        /**
         * The list of accounts found in the import file. Never {@code null}.
         */
        public final List<AccountDescription> accounts;

        private ImportContents(boolean globalSettings, List<AccountDescription> accounts) {
            this.globalSettings = globalSettings;
            this.accounts = accounts;
        }
    }

    /**
     * Class to describe an account (name, UUID).
     *
     * @see ImportContents
     */
    public static class AccountDescription {
        /**
         * The name of the account.
         */
        public final String name;

        /**
         * The UUID of the account.
         */
        public final String uuid;

        private AccountDescription(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    public static class AccountDescriptionPair {
        public final AccountDescription original;
        public final AccountDescription imported;

        private AccountDescriptionPair(AccountDescription original, AccountDescription imported) {
            this.original = original;
            this.imported = imported;
        }
    }

    public static class ImportResults {
        public final boolean globalSettings;
        public final List<AccountDescriptionPair> importedAccounts;
        public final List<AccountDescription> errorneousAccounts;

        private ImportResults(boolean globalSettings,
                List<AccountDescriptionPair> importedAccounts,
                List<AccountDescription> errorneousAccounts) {
           this.globalSettings = globalSettings;
           this.importedAccounts = importedAccounts;
           this.errorneousAccounts = errorneousAccounts;
        }
    }

    public static boolean isImportStreamEncrypted(Context context, InputStream inputStream) {
        return false;
    }

    /**
     * Parses an import {@link InputStream} and returns information on whether it contains global
     * settings and/or account settings. For all account configurations found, the name of the
     * account along with the account UUID is returned.
     *
     * @param context
     * @param inputStream
     * @param encryptionKey
     * @return
     * @throws StorageImportExportException
     */
    public static ImportContents getImportStreamContents(Context context, InputStream inputStream,
            String encryptionKey) throws StorageImportExportException {

        try {
            // Parse the import stream but don't save individual settings (overview=true)
            Imported imported = parseSettings(inputStream, false, null, false, true);

            // If the stream contains global settings the "globalSettings" member will not be null
            boolean globalSettings = (imported.globalSettings != null);

            final List<AccountDescription> accounts = new ArrayList<AccountDescription>();
            // If the stream contains at least one account configuration the "accounts" member
            // will not be null.
            if (imported.accounts != null) {
                for (ImportedAccount account : imported.accounts.values()) {
                    accounts.add(new AccountDescription(account.name, account.uuid));
                }
            }

            //TODO: throw exception if neither global settings nor account settings could be found

            return new ImportContents(globalSettings, accounts);

        } catch (StorageImportExportException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageImportExportException(e);
        }
    }

    /**
     * Reads an import {@link InputStream} and imports the global settings and/or account
     * configurations specified by the arguments.
     *
     * @param context
     * @param inputStream
     * @param encryptionKey
     * @param globalSettings
     * @param accountUuids
     * @param overwrite
     * @throws StorageImportExportException
     */
    public static ImportResults importSettings(Context context, InputStream inputStream, String encryptionKey,
            boolean globalSettings, List<String> accountUuids, boolean overwrite)
    throws StorageImportExportException {

        try
        {
            boolean globalSettingsImported = false;
            List<AccountDescriptionPair> importedAccounts = new ArrayList<AccountDescriptionPair>();
            List<AccountDescription> errorneousAccounts = new ArrayList<AccountDescription>();

            Imported imported = parseSettings(inputStream, globalSettings, accountUuids, overwrite, false);

            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();

            if (globalSettings) {
                try {
                    SharedPreferences.Editor editor = storage.edit();
                    if (imported.globalSettings != null) {
                        importGlobalSettings(editor, imported.globalSettings);
                    } else {
                        Log.w(K9.LOG_TAG, "Was asked to import global settings but none found.");
                    }
                    if (editor.commit()) {
                        globalSettingsImported = true;
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Exception while importing global settings", e);
                }
            }

            if (accountUuids != null && accountUuids.size() > 0) {
                if (imported.accounts != null) {
                    List<String> newUuids = new ArrayList<String>();
                    for (String accountUuid : accountUuids) {
                        if (imported.accounts.containsKey(accountUuid)) {
                            ImportedAccount account = imported.accounts.get(accountUuid);
                            try {
                                SharedPreferences.Editor editor = storage.edit();

                                AccountDescriptionPair importResult = importAccount(context,
                                        editor, account, overwrite);

                                String newUuid = importResult.imported.uuid;
                                if (!newUuid.equals(importResult.original.uuid)) {
                                    newUuids.add(newUuid);
                                }
                                if (editor.commit()) {
                                    importedAccounts.add(importResult);
                                } else {
                                    errorneousAccounts.add(importResult.original);
                                }
                            } catch (Exception e) {
                                errorneousAccounts.add(new AccountDescription(account.name, account.uuid));
                            }
                        } else {
                            Log.w(K9.LOG_TAG, "Was asked to import account with UUID " +
                                    accountUuid + ". But this account wasn't found.");
                        }
                    }

                    SharedPreferences.Editor editor = storage.edit();

                    if (newUuids.size() > 0) {
                        String oldAccountUuids = storage.getString("accountUuids", "");
                        String appendUuids = Utility.combine(newUuids.toArray(new String[0]), ',');
                        String prefix = "";
                        if (oldAccountUuids.length() > 0) {
                            prefix = oldAccountUuids + ",";
                        }
                        editor.putString("accountUuids", prefix + appendUuids);
                    }

                    String defaultAccountUuid = storage.getString("defaultAccountUuid", null);
                    if (defaultAccountUuid == null) {
                        editor.putString("defaultAccountUuid", accountUuids.get(0));
                    }

                    if (!editor.commit()) {
                        throw new StorageImportExportException("Failed to set default account");
                    }
                } else {
                    Log.w(K9.LOG_TAG, "Was asked to import at least one account but none found.");
                }
            }

            preferences.loadAccounts();
            DateFormatter.clearChosenFormat();
            K9.loadPrefs(preferences);
            K9.setServicesEnabled(context);

            return new ImportResults(globalSettingsImported, importedAccounts, errorneousAccounts);

        } catch (StorageImportExportException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageImportExportException(e);
        }
    }

    private static void importGlobalSettings(SharedPreferences.Editor editor,
            ImportedSettings settings) {

        Map<String, String> writeSettings = GlobalSettings.validate(settings.settings);

        for (Map.Entry<String, String> setting : writeSettings.entrySet()) {
            String key = setting.getKey();
            String value = setting.getValue();
            Log.v(K9.LOG_TAG, "Write " + key + "=" + value);
            editor.putString(key, value);
        }
    }

    private static AccountDescriptionPair importAccount(Context context,
            SharedPreferences.Editor editor, ImportedAccount account, boolean overwrite) {

        AccountDescription original = new AccountDescription(account.name, account.uuid);

        // Validate input and ignore malformed values when possible
        Map<String, String> validatedSettings =
            AccountSettings.validate(account.settings.settings);

        //TODO: validate account name
        //TODO: validate identity settings
        //TODO: validate folder settings


        Preferences prefs = Preferences.getPreferences(context);
        Account[] accounts = prefs.getAccounts();

        String uuid = account.uuid;
        Account existingAccount = prefs.getAccount(uuid);
        if (!overwrite && existingAccount != null) {
            // An account with this UUID already exists, but we're not allowed to overwrite it.
            // So generate a new UUID.
            uuid = UUID.randomUUID().toString();
        }

        String accountName = account.name;
        if (isAccountNameUsed(accountName, accounts)) {
            // Account name is already in use. So generate a new one by appending " (x)", where x
            // is the first number >= 1 that results in an unused account name.
            for (int i = 1; i <= accounts.length; i++) {
                accountName = account.name + " (" + i + ")";
                if (!isAccountNameUsed(accountName, accounts)) {
                    break;
                }
            }
        }

        String accountKeyPrefix = uuid + ".";
        editor.putString(accountKeyPrefix + Account.ACCOUNT_DESCRIPTION_KEY, accountName);

        // Write account settings
        for (Map.Entry<String, String> setting : validatedSettings.entrySet()) {
            String key = accountKeyPrefix + setting.getKey();
            String value = setting.getValue();
            editor.putString(key, value);
        }

        // If it's a new account generate and write a new "accountNumber"
        if (existingAccount == null || !uuid.equals(account.uuid)) {
            int newAccountNumber = Account.generateAccountNumber(prefs);
            editor.putString(accountKeyPrefix + "accountNumber", Integer.toString(newAccountNumber));
        }

        if (account.identities != null) {
            importIdentities(editor, uuid, account, overwrite, existingAccount);
        }

        // Write folder settings
        if (account.folders != null) {
            for (ImportedFolder folder : account.folders) {
                String folderKeyPrefix = uuid + "." + folder.name + ".";
                for (Map.Entry<String, String> setting : folder.settings.settings.entrySet()) {
                    String key = folderKeyPrefix + setting.getKey();
                    String value = setting.getValue();
                    editor.putString(key, value);
                }
            }
        }

        //TODO: sync folder settings with localstore?

        AccountDescription imported = new AccountDescription(accountName, uuid);
        return new AccountDescriptionPair(original, imported);
    }

    private static void importIdentities(SharedPreferences.Editor editor, String uuid,
            ImportedAccount account, boolean overwrite, Account existingAccount) {

        String accountKeyPrefix = uuid + ".";

        // Gather information about existing identities for this account (if any)
        int nextIdentityIndex = 0;
        final List<Identity> existingIdentities;
        if (overwrite && existingAccount != null) {
            existingIdentities = existingAccount.getIdentities();
            nextIdentityIndex = existingIdentities.size();
        } else {
            existingIdentities = new ArrayList<Identity>();
        }

        // Write identities
        for (ImportedIdentity identity : account.identities) {
            int writeIdentityIndex = nextIdentityIndex;
            if (existingIdentities.size() > 0) {
                int identityIndex = findIdentity(identity, existingIdentities);
                if (overwrite && identityIndex != -1) {
                    writeIdentityIndex = identityIndex;
                }
            }
            if (writeIdentityIndex == nextIdentityIndex) {
                nextIdentityIndex++;
            }

            String identityDescription = identity.description;
            if (isIdentityDescriptionUsed(identityDescription, existingIdentities)) {
                // Identity description is already in use. So generate a new one by appending
                // " (x)", where x is the first number >= 1 that results in an unused identity
                // description.
                for (int i = 1; i <= existingIdentities.size(); i++) {
                    identityDescription = identity.description + " (" + i + ")";
                    if (!isIdentityDescriptionUsed(identityDescription, existingIdentities)) {
                        break;
                    }
                }
            }

            editor.putString(accountKeyPrefix + Account.IDENTITY_NAME_KEY + "." +
                    writeIdentityIndex, identity.name);
            editor.putString(accountKeyPrefix + Account.IDENTITY_EMAIL_KEY + "." +
                    writeIdentityIndex, identity.email);
            editor.putString(accountKeyPrefix + Account.IDENTITY_DESCRIPTION_KEY + "." +
                    writeIdentityIndex, identityDescription);

            // Write identity settings
            for (Map.Entry<String, String> setting : identity.settings.settings.entrySet()) {
                String key = setting.getKey();
                String value = setting.getValue();
                editor.putString(accountKeyPrefix + key + "." + writeIdentityIndex, value);
            }
        }
    }

    private static boolean isAccountNameUsed(String name, Account[] accounts) {
        for (Account account : accounts) {
            if (account.getDescription().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdentityDescriptionUsed(String description, List<Identity> identities) {
        for (Identity identitiy : identities) {
            if (identitiy.getDescription().equals(description)) {
                return true;
            }
        }
        return false;
    }

    private static int findIdentity(ImportedIdentity identity,
            List<Identity> identities) {
        for (int i = 0; i < identities.size(); i++) {
            Identity existingIdentity = identities.get(i);
            if (existingIdentity.getName().equals(identity.name) &&
                    existingIdentity.getEmail().equals(identity.email)) {
                return i;
            }
        }
        return -1;
    }

    private static Imported parseSettings(InputStream inputStream, boolean globalSettings,
            List<String> accountUuids, boolean overwrite, boolean overview)
    throws StorageImportExportException {

        if (!overview && accountUuids == null) {
            throw new IllegalArgumentException("Argument 'accountUuids' must not be null.");
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            //factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            InputStreamReader reader = new InputStreamReader(inputStream);
            xpp.setInput(reader);

            Imported imported = null;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    if (StorageExporter.ROOT_ELEMENT.equals(xpp.getName())) {
                        imported = parseRoot(xpp, globalSettings, accountUuids, overview);
                    } else {
                        Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }

            if (imported == null || (overview && imported.globalSettings == null &&
                    imported.accounts == null)) {
                throw new StorageImportExportException("Invalid import data");
            }

            return imported;
        } catch (Exception e) {
            throw new StorageImportExportException(e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) { /* Ignore */ }
        }
    }

    private static void skipToEndTag(XmlPullParser xpp, String endTag)
    throws XmlPullParserException, IOException {

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {
            eventType = xpp.next();
        }
    }

    private static String getText(XmlPullParser xpp)
    throws XmlPullParserException, IOException {

        int eventType = xpp.next();
        if (eventType != XmlPullParser.TEXT) {
            return "";
        }
        return xpp.getText();
    }

    private static Imported parseRoot(XmlPullParser xpp, boolean globalSettings,
            List<String> accountUuids, boolean overview)
    throws XmlPullParserException, IOException {

        Imported result = new Imported();

        //TODO: check version attribute

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG &&
                 StorageExporter.ROOT_ELEMENT.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.GLOBAL_ELEMENT.equals(element)) {
                    if (overview || globalSettings) {
                        if (result.globalSettings == null) {
                            if (overview) {
                                result.globalSettings = new ImportedSettings();
                                skipToEndTag(xpp, StorageExporter.GLOBAL_ELEMENT);
                            } else {
                                result.globalSettings = parseSettings(xpp, StorageExporter.GLOBAL_ELEMENT);
                            }
                        } else {
                            skipToEndTag(xpp, StorageExporter.GLOBAL_ELEMENT);
                            Log.w(K9.LOG_TAG, "More than one global settings element. Only using the first one!");
                        }
                    } else {
                        skipToEndTag(xpp, StorageExporter.GLOBAL_ELEMENT);
                        Log.i(K9.LOG_TAG, "Skipping global settings");
                    }
                } else if (StorageExporter.ACCOUNTS_ELEMENT.equals(element)) {
                    if (result.accounts == null) {
                        result.accounts = parseAccounts(xpp, accountUuids, overview);
                    } else {
                        Log.w(K9.LOG_TAG, "More than one accounts element. Only using the first one!");
                    }
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return result;
    }

    private static ImportedSettings parseSettings(XmlPullParser xpp, String endTag)
    throws XmlPullParserException, IOException {

        ImportedSettings result = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.VALUE_ELEMENT.equals(element)) {
                    String key = xpp.getAttributeValue(null, StorageExporter.KEY_ATTRIBUTE);
                    String value = getText(xpp);

                    if (result == null) {
                        result = new ImportedSettings();
                    }

                    if (result.settings.containsKey(key)) {
                        Log.w(K9.LOG_TAG, "Already read key \"" + key + "\". Ignoring value \"" + value + "\"");
                    } else {
                        result.settings.put(key, value);
                    }
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return result;
    }

    private static Map<String, ImportedAccount> parseAccounts(XmlPullParser xpp,
            List<String> accountUuids, boolean overview)
    throws XmlPullParserException, IOException {

        Map<String, ImportedAccount> accounts = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG &&
                 StorageExporter.ACCOUNTS_ELEMENT.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.ACCOUNT_ELEMENT.equals(element)) {
                    if (accounts == null) {
                        accounts = new HashMap<String, ImportedAccount>();
                    }

                    ImportedAccount account = parseAccount(xpp, accountUuids, overview);

                    if (!accounts.containsKey(account.uuid)) {
                        accounts.put(account.uuid, account);
                    } else {
                        Log.w(K9.LOG_TAG, "Duplicate account entries with UUID " + account.uuid +
                                ". Ignoring!");
                    }
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return accounts;
    }

    private static ImportedAccount parseAccount(XmlPullParser xpp, List<String> accountUuids,
            boolean overview)
    throws XmlPullParserException, IOException {

        ImportedAccount account = new ImportedAccount();

        String uuid = xpp.getAttributeValue(null, StorageExporter.UUID_ATTRIBUTE);
        account.uuid = uuid;

        if (overview || accountUuids.contains(uuid)) {
            int eventType = xpp.next();
            while (!(eventType == XmlPullParser.END_TAG &&
                     StorageExporter.ACCOUNT_ELEMENT.equals(xpp.getName()))) {

                if(eventType == XmlPullParser.START_TAG) {
                    String element = xpp.getName();
                    if (StorageExporter.NAME_ELEMENT.equals(element)) {
                        account.name = getText(xpp);
                    } else if (StorageExporter.SETTINGS_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, StorageExporter.SETTINGS_ELEMENT);
                        } else {
                            account.settings = parseSettings(xpp, StorageExporter.SETTINGS_ELEMENT);
                        }
                    } else if (StorageExporter.IDENTITIES_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, StorageExporter.IDENTITIES_ELEMENT);
                        } else {
                            account.identities = parseIdentities(xpp);
                        }
                    } else if (StorageExporter.FOLDERS_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, StorageExporter.FOLDERS_ELEMENT);
                        } else {
                            account.folders = parseFolders(xpp);
                        }
                    } else {
                        Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } else {
            skipToEndTag(xpp, StorageExporter.ACCOUNT_ELEMENT);
            Log.i(K9.LOG_TAG, "Skipping account with UUID " + uuid);
        }

        return account;
    }

    private static List<ImportedIdentity> parseIdentities(XmlPullParser xpp)
    throws XmlPullParserException, IOException {
        List<ImportedIdentity> identities = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG &&
                 StorageExporter.IDENTITIES_ELEMENT.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.IDENTITY_ELEMENT.equals(element)) {
                    if (identities == null) {
                        identities = new ArrayList<ImportedIdentity>();
                    }

                    ImportedIdentity identity = parseIdentity(xpp);
                    identities.add(identity);
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return identities;
    }

    private static ImportedIdentity parseIdentity(XmlPullParser xpp)
    throws XmlPullParserException, IOException {
        ImportedIdentity identity = new ImportedIdentity();

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG &&
                 StorageExporter.IDENTITY_ELEMENT.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.NAME_ELEMENT.equals(element)) {
                    identity.name = getText(xpp);
                } else if (StorageExporter.EMAIL_ELEMENT.equals(element)) {
                    identity.email = getText(xpp);
                } else if (StorageExporter.DESCRIPTION_ELEMENT.equals(element)) {
                    identity.description = getText(xpp);
                } else if (StorageExporter.SETTINGS_ELEMENT.equals(element)) {
                    identity.settings = parseSettings(xpp, StorageExporter.SETTINGS_ELEMENT);
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return identity;
    }

    private static List<ImportedFolder> parseFolders(XmlPullParser xpp)
    throws XmlPullParserException, IOException {
        List<ImportedFolder> folders = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG &&
                 StorageExporter.FOLDERS_ELEMENT.equals(xpp.getName()))) {

            if(eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (StorageExporter.FOLDER_ELEMENT.equals(element)) {
                    if (folders == null) {
                        folders = new ArrayList<ImportedFolder>();
                    }

                    ImportedFolder folder = parseFolder(xpp);
                    folders.add(folder);
                } else {
                    Log.w(K9.LOG_TAG, "Unexpected start tag: " + xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return folders;
    }

    private static ImportedFolder parseFolder(XmlPullParser xpp)
    throws XmlPullParserException, IOException {
        ImportedFolder folder = new ImportedFolder();

        String name = xpp.getAttributeValue(null, StorageExporter.NAME_ATTRIBUTE);
        folder.name = name;

        folder.settings = parseSettings(xpp, StorageExporter.FOLDER_ELEMENT);

        return folder;
    }

    private static class Imported {
        public ImportedSettings globalSettings;
        public Map<String, ImportedAccount> accounts;
    }

    private static class ImportedSettings {
        public Map<String, String> settings = new HashMap<String, String>();
    }

    private static class ImportedAccount {
        public String uuid;
        public String name;
        public ImportedSettings settings;
        public List<ImportedIdentity> identities;
        public List<ImportedFolder> folders;
    }

    private static class ImportedIdentity {
        public String name;
        public String email;
        public String description;
        public ImportedSettings settings;
    }

    private static class ImportedFolder {
        public String name;
        public ImportedSettings settings;
    }
}
