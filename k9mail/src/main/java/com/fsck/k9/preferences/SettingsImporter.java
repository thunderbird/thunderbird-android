package com.fsck.k9.preferences;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.fsck.k9.mail.TransportUris;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class SettingsImporter {

    /**
     * Class to list the contents of an import file/stream.
     *
     * @see SettingsImporter#getImportStreamContents(InputStream)
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

    public static class AccountDescription {
        public final String name;
        public final String uuid;

        private AccountDescription(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    public static class AccountDescriptionPair {
        public final AccountDescription original;
        public final AccountDescription imported;
        public final boolean overwritten;

        private AccountDescriptionPair(AccountDescription original, AccountDescription imported, boolean overwritten) {
            this.original = original;
            this.imported = imported;
            this.overwritten = overwritten;
        }
    }

    public static class ImportResults {
        public final boolean globalSettings;
        public final List<AccountDescriptionPair> importedAccounts;
        public final List<AccountDescription> erroneousAccounts;

        private ImportResults(boolean globalSettings, List<AccountDescriptionPair> importedAccounts,
                List<AccountDescription> erroneousAccounts) {
            this.globalSettings = globalSettings;
            this.importedAccounts = importedAccounts;
            this.erroneousAccounts = erroneousAccounts;
        }
    }

    /**
     * Parses an import {@link InputStream} and returns information on whether it contains global
     * settings and/or account settings. For all account configurations found, the name of the
     * account along with the account UUID is returned.
     *
     * @param inputStream
     *         An {@code InputStream} to read the settings from.
     *
     * @return An {@link ImportContents} instance containing information about the contents of the
     *         settings file.
     *
     * @throws SettingsImportExportException
     *         In case of an error.
     */
    public static ImportContents getImportStreamContents(InputStream inputStream)
            throws SettingsImportExportException {

        try {
            // Parse the import stream but don't save individual settings (overview=true)
            Imported imported = parseSettings(inputStream, false, null, true);

            // If the stream contains global settings the "globalSettings" member will not be null
            boolean globalSettings = (imported.globalSettings != null);

            final List<AccountDescription> accounts = new ArrayList<>();
            // If the stream contains at least one account configuration the "accounts" member
            // will not be null.
            if (imported.accounts != null) {
                for (ImportedAccount account : imported.accounts.values()) {
                    String accountName = getAccountDisplayName(account);
                    accounts.add(new AccountDescription(accountName, account.uuid));
                }
            }

            //TODO: throw exception if neither global settings nor account settings could be found

            return new ImportContents(globalSettings, accounts);

        } catch (SettingsImportExportException e) {
            throw e;
        } catch (Exception e) {
            throw new SettingsImportExportException(e);
        }
    }

    /**
     * Reads an import {@link InputStream} and imports the global settings and/or account
     * configurations specified by the arguments.
     *
     * @param context
     *         A {@link Context} instance.
     * @param inputStream
     *         The {@code InputStream} to read the settings from.
     * @param globalSettings
     *         {@code true} if global settings should be imported from the file.
     * @param accountUuids
     *         A list of UUIDs of the accounts that should be imported.
     * @param overwrite
     *         {@code true} if existing accounts should be overwritten when an account with the
     *         same UUID is found in the settings file.<br>
     *         <strong>Note:</strong> This can have side-effects we currently don't handle, e.g.
     *         changing the account type from IMAP to POP3. So don't use this for now!
     *
     * @return An {@link ImportResults} instance containing information about errors and
     *         successfully imported accounts.
     *
     * @throws SettingsImportExportException
     *         In case of an error.
     */
    public static ImportResults importSettings(Context context, InputStream inputStream, boolean globalSettings,
            List<String> accountUuids, boolean overwrite) throws SettingsImportExportException {

        try {
            boolean globalSettingsImported = false;
            List<AccountDescriptionPair> importedAccounts = new ArrayList<>();
            List<AccountDescription> erroneousAccounts = new ArrayList<>();

            Imported imported = parseSettings(inputStream, globalSettings, accountUuids, false);

            Preferences preferences = Preferences.getPreferences(context);
            Storage storage = preferences.getStorage();

            if (globalSettings) {
                try {
                    StorageEditor editor = storage.edit();
                    if (imported.globalSettings != null) {
                        importGlobalSettings(storage, editor, imported.contentVersion, imported.globalSettings);
                    } else {
                        Timber.w("Was asked to import global settings but none found.");
                    }
                    if (editor.commit()) {
                        Timber.v("Committed global settings to the preference storage.");
                        globalSettingsImported = true;
                    } else {
                        Timber.v("Failed to commit global settings to the preference storage");
                    }
                } catch (Exception e) {
                    Timber.e(e, "Exception while importing global settings");
                }
            }

            if (accountUuids != null && accountUuids.size() > 0) {
                if (imported.accounts != null) {
                    for (String accountUuid : accountUuids) {
                        if (imported.accounts.containsKey(accountUuid)) {
                            ImportedAccount account = imported.accounts.get(accountUuid);
                            try {
                                StorageEditor editor = storage.edit();

                                AccountDescriptionPair importResult = importAccount(context, editor,
                                        imported.contentVersion, account, overwrite);

                                if (editor.commit()) {
                                    Timber.v("Committed settings for account \"%s\" to the settings database.",
                                            importResult.imported.name);

                                    // Add UUID of the account we just imported to the list of
                                    // account UUIDs
                                    if (!importResult.overwritten) {
                                        editor = storage.edit();

                                        String newUuid = importResult.imported.uuid;
                                        String oldAccountUuids = storage.getString("accountUuids", "");
                                        String newAccountUuids = (oldAccountUuids.length() > 0) ?
                                                oldAccountUuids + "," + newUuid : newUuid;

                                        putString(editor, "accountUuids", newAccountUuids);

                                        if (!editor.commit()) {
                                            throw new SettingsImportExportException("Failed to set account UUID list");
                                        }
                                    }

                                    // Reload accounts
                                    preferences.loadAccounts();

                                    importedAccounts.add(importResult);
                                } else {
                                    Timber.w("Error while committing settings for account \"%s\" to the settings " +
                                            "database.", importResult.original.name);

                                    erroneousAccounts.add(importResult.original);
                                }
                            } catch (InvalidSettingValueException e) {
                                Timber.e(e, "Encountered invalid setting while importing account \"%s\"",
                                        account.name);

                                erroneousAccounts.add(new AccountDescription(account.name, account.uuid));
                            } catch (Exception e) {
                                Timber.e(e, "Exception while importing account \"%s\"", account.name);
                                erroneousAccounts.add(new AccountDescription(account.name, account.uuid));
                            }
                        } else {
                            Timber.w("Was asked to import account with UUID %s. But this account wasn't found.",
                                    accountUuid);
                        }
                    }

                    StorageEditor editor = storage.edit();

                    String defaultAccountUuid = storage.getString("defaultAccountUuid", null);
                    if (defaultAccountUuid == null) {
                        putString(editor, "defaultAccountUuid", accountUuids.get(0));
                    }

                    if (!editor.commit()) {
                        throw new SettingsImportExportException("Failed to set default account");
                    }
                } else {
                    Timber.w("Was asked to import at least one account but none found.");
                }
            }

            preferences.loadAccounts();
            K9.loadPrefs(preferences);
            K9.setServicesEnabled(context);

            return new ImportResults(globalSettingsImported, importedAccounts, erroneousAccounts);

        } catch (SettingsImportExportException e) {
            throw e;
        } catch (Exception e) {
            throw new SettingsImportExportException(e);
        }
    }

    private static void importGlobalSettings(Storage storage, StorageEditor editor, int contentVersion,
            ImportedSettings settings) {

        // Validate global settings
        Map<String, Object> validatedSettings = GlobalSettings.validate(contentVersion, settings.settings);

        // Upgrade global settings to current content version
        if (contentVersion != Settings.VERSION) {
            GlobalSettings.upgrade(contentVersion, validatedSettings);
        }

        // Convert global settings to the string representation used in preference storage
        Map<String, String> stringSettings = GlobalSettings.convert(validatedSettings);

        // Use current global settings as base and overwrite with validated settings read from the import file.
        Map<String, String> mergedSettings = new HashMap<>(GlobalSettings.getGlobalSettings(storage));
        mergedSettings.putAll(stringSettings);

        for (Map.Entry<String, String> setting : mergedSettings.entrySet()) {
            String key = setting.getKey();
            String value = setting.getValue();
            putString(editor, key, value);
        }
    }

    private static AccountDescriptionPair importAccount(Context context, StorageEditor editor, int contentVersion,
            ImportedAccount account, boolean overwrite) throws InvalidSettingValueException {

        AccountDescription original = new AccountDescription(account.name, account.uuid);

        Preferences prefs = Preferences.getPreferences(context);
        List<Account> accounts = prefs.getAccounts();

        String uuid = account.uuid;
        Account existingAccount = prefs.getAccount(uuid);
        boolean mergeImportedAccount = (overwrite && existingAccount != null);

        if (!overwrite && existingAccount != null) {
            // An account with this UUID already exists, but we're not allowed to overwrite it.
            // So generate a new UUID.
            uuid = UUID.randomUUID().toString();
        }

        // Make sure the account name is unique
        String accountName = account.name;
        if (isAccountNameUsed(accountName, accounts)) {
            // Account name is already in use. So generate a new one by appending " (x)", where x is the first
            // number >= 1 that results in an unused account name.
            for (int i = 1; i <= accounts.size(); i++) {
                accountName = account.name + " (" + i + ")";
                if (!isAccountNameUsed(accountName, accounts)) {
                    break;
                }
            }
        }

        // Write account name
        String accountKeyPrefix = uuid + ".";
        putString(editor, accountKeyPrefix + Account.ACCOUNT_DESCRIPTION_KEY, accountName);

        if (account.incoming == null) {
            // We don't import accounts without incoming server settings
            throw new InvalidSettingValueException();
        }

        // Write incoming server settings (storeUri)
        ServerSettings incoming = new ImportedServerSettings(account.incoming);
        String storeUri = RemoteStore.createStoreUri(incoming);
        putString(editor, accountKeyPrefix + Account.STORE_URI_KEY, Base64.encode(storeUri));

        // Mark account as disabled if the AuthType isn't EXTERNAL and the
        // settings file didn't contain a password
        boolean createAccountDisabled = AuthType.EXTERNAL != incoming.authenticationType &&
                (incoming.password == null || incoming.password.isEmpty());

        if (account.outgoing == null && !ServerSettings.Type.WebDAV.name().equals(account.incoming.type)) {
            // All account types except WebDAV need to provide outgoing server settings
            throw new InvalidSettingValueException();
        }

        if (account.outgoing != null) {
            // Write outgoing server settings (transportUri)
            ServerSettings outgoing = new ImportedServerSettings(account.outgoing);
            String transportUri = TransportUris.createTransportUri(outgoing);
            putString(editor, accountKeyPrefix + Account.TRANSPORT_URI_KEY, Base64.encode(transportUri));

            /*
             * Mark account as disabled if the settings file contained a username but no password. However, no password
             * is required for the outgoing server for WebDAV accounts, because incoming and outgoing servers are 
             * identical for this account type. Nor is a password required if the AuthType is EXTERNAL.
             */
            boolean outgoingPasswordNeeded = AuthType.EXTERNAL != outgoing.authenticationType &&
                    !(ServerSettings.Type.WebDAV == outgoing.type) &&
                    outgoing.username != null &&
                    !outgoing.username.isEmpty() &&
                    (outgoing.password == null || outgoing.password.isEmpty());
            createAccountDisabled = outgoingPasswordNeeded || createAccountDisabled;
        }

        // Write key to mark account as disabled if necessary
        if (createAccountDisabled) {
            editor.putBoolean(accountKeyPrefix + "enabled", false);
        }

        // Validate account settings
        Map<String, Object> validatedSettings =
                AccountSettings.validate(contentVersion, account.settings.settings, !mergeImportedAccount);

        // Upgrade account settings to current content version
        if (contentVersion != Settings.VERSION) {
            AccountSettings.upgrade(contentVersion, validatedSettings);
        }

        // Convert account settings to the string representation used in preference storage
        Map<String, String> stringSettings = AccountSettings.convert(validatedSettings);

        // Merge account settings if necessary
        Map<String, String> writeSettings;
        if (mergeImportedAccount) {
            writeSettings = new HashMap<>(AccountSettings.getAccountSettings(prefs.getStorage(), uuid));
            writeSettings.putAll(stringSettings);
        } else {
            writeSettings = stringSettings;
        }

        // Write account settings
        for (Map.Entry<String, String> setting : writeSettings.entrySet()) {
            String key = accountKeyPrefix + setting.getKey();
            String value = setting.getValue();
            putString(editor, key, value);
        }

        // If it's a new account generate and write a new "accountNumber"
        if (!mergeImportedAccount) {
            int newAccountNumber = Account.generateAccountNumber(prefs);
            putString(editor, accountKeyPrefix + "accountNumber", Integer.toString(newAccountNumber));
        }

        // Write identities
        if (account.identities != null) {
            importIdentities(editor, contentVersion, uuid, account, overwrite, existingAccount, prefs);
        } else if (!mergeImportedAccount) {
            // Require accounts to at least have one identity
            throw new InvalidSettingValueException();
        }

        // Write folder settings
        if (account.folders != null) {
            for (ImportedFolder folder : account.folders) {
                importFolder(editor, contentVersion, uuid, folder, mergeImportedAccount, prefs);
            }
        }

        //TODO: sync folder settings with localstore?

        AccountDescription imported = new AccountDescription(accountName, uuid);
        return new AccountDescriptionPair(original, imported, mergeImportedAccount);
    }

    private static void importFolder(StorageEditor editor, int contentVersion, String uuid, ImportedFolder folder,
            boolean overwrite, Preferences prefs) {

        // Validate folder settings
        Map<String, Object> validatedSettings =
                FolderSettings.validate(contentVersion, folder.settings.settings, !overwrite);

        // Upgrade folder settings to current content version
        if (contentVersion != Settings.VERSION) {
            FolderSettings.upgrade(contentVersion, validatedSettings);
        }

        // Convert folder settings to the string representation used in preference storage
        Map<String, String> stringSettings = FolderSettings.convert(validatedSettings);

        // Merge folder settings if necessary
        Map<String, String> writeSettings;
        if (overwrite) {
            writeSettings = FolderSettings.getFolderSettings(prefs.getStorage(), uuid, folder.name);
            writeSettings.putAll(stringSettings);
        } else {
            writeSettings = stringSettings;
        }

        // Write folder settings
        String prefix = uuid + "." + folder.name + ".";
        for (Map.Entry<String, String> setting : writeSettings.entrySet()) {
            String key = prefix + setting.getKey();
            String value = setting.getValue();
            putString(editor, key, value);
        }
    }

    private static void importIdentities(StorageEditor editor, int contentVersion, String uuid, ImportedAccount account,
            boolean overwrite, Account existingAccount, Preferences prefs) throws InvalidSettingValueException {

        String accountKeyPrefix = uuid + ".";

        // Gather information about existing identities for this account (if any)
        int nextIdentityIndex = 0;
        final List<Identity> existingIdentities;
        if (overwrite && existingAccount != null) {
            existingIdentities = existingAccount.getIdentities();
            nextIdentityIndex = existingIdentities.size();
        } else {
            existingIdentities = new ArrayList<>();
        }

        // Write identities
        for (ImportedIdentity identity : account.identities) {
            int writeIdentityIndex = nextIdentityIndex;
            boolean mergeSettings = false;
            if (overwrite && existingIdentities.size() > 0) {
                int identityIndex = findIdentity(identity, existingIdentities);
                if (identityIndex != -1) {
                    writeIdentityIndex = identityIndex;
                    mergeSettings = true;
                }
            }
            if (!mergeSettings) {
                nextIdentityIndex++;
            }

            String identityDescription = (identity.description == null) ? "Imported" : identity.description;
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

            String identitySuffix = "." + writeIdentityIndex;

            // Write name used in identity
            String identityName = (identity.name == null) ? "" : identity.name;
            putString(editor, accountKeyPrefix + Account.IDENTITY_NAME_KEY + identitySuffix, identityName);

            // Validate email address
            if (!IdentitySettings.isEmailAddressValid(identity.email)) {
                throw new InvalidSettingValueException();
            }

            // Write email address
            putString(editor, accountKeyPrefix + Account.IDENTITY_EMAIL_KEY + identitySuffix, identity.email);

            // Write identity description
            putString(editor, accountKeyPrefix + Account.IDENTITY_DESCRIPTION_KEY + identitySuffix,
                    identityDescription);

            if (identity.settings != null) {
                // Validate identity settings
                Map<String, Object> validatedSettings = IdentitySettings.validate(
                        contentVersion, identity.settings.settings, !mergeSettings);

                // Upgrade identity settings to current content version
                if (contentVersion != Settings.VERSION) {
                    IdentitySettings.upgrade(contentVersion, validatedSettings);
                }

                // Convert identity settings to the representation used in preference storage
                Map<String, String> stringSettings = IdentitySettings.convert(validatedSettings);

                // Merge identity settings if necessary
                Map<String, String> writeSettings;
                if (mergeSettings) {
                    writeSettings = new HashMap<>(IdentitySettings.getIdentitySettings(
                            prefs.getStorage(), uuid, writeIdentityIndex));
                    writeSettings.putAll(stringSettings);
                } else {
                    writeSettings = stringSettings;
                }

                // Write identity settings
                for (Map.Entry<String, String> setting : writeSettings.entrySet()) {
                    String key = accountKeyPrefix + setting.getKey() + identitySuffix;
                    String value = setting.getValue();
                    putString(editor, key, value);
                }
            }
        }
    }

    private static boolean isAccountNameUsed(String name, List<Account> accounts) {
        for (Account account : accounts) {
            if (account == null) {
                continue;
            }

            if (account.getDescription().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdentityDescriptionUsed(String description, List<Identity> identities) {
        for (Identity identity : identities) {
            if (identity.getDescription().equals(description)) {
                return true;
            }
        }
        return false;
    }

    private static int findIdentity(ImportedIdentity identity, List<Identity> identities) {
        for (int i = 0; i < identities.size(); i++) {
            Identity existingIdentity = identities.get(i);
            if (existingIdentity.getName().equals(identity.name) &&
                    existingIdentity.getEmail().equals(identity.email)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Write to an {@link SharedPreferences.Editor} while logging what is written if debug logging
     * is enabled.
     *
     * @param editor
     *         The {@code Editor} to write to.
     * @param key
     *         The name of the preference to modify.
     * @param value
     *         The new value for the preference.
     */
    private static void putString(StorageEditor editor, String key, String value) {
        if (K9.isDebug()) {
            String outputValue = value;
            if (!K9.DEBUG_SENSITIVE && (key.endsWith(".transportUri") || key.endsWith(".storeUri"))) {
                outputValue = "*sensitive*";
            }
            Timber.v("Setting %s=%s", key, outputValue);
        }
        editor.putString(key, value);
    }

    @VisibleForTesting
    static Imported parseSettings(InputStream inputStream, boolean globalSettings, List<String> accountUuids,
            boolean overview) throws SettingsImportExportException {

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
                if (eventType == XmlPullParser.START_TAG) {
                    if (SettingsExporter.ROOT_ELEMENT.equals(xpp.getName())) {
                        imported = parseRoot(xpp, globalSettings, accountUuids, overview);
                    } else {
                        Timber.w("Unexpected start tag: %s", xpp.getName());
                    }
                }
                eventType = xpp.next();
            }

            if (imported == null || (overview && imported.globalSettings == null && imported.accounts == null)) {
                throw new SettingsImportExportException("Invalid import data");
            }

            return imported;
        } catch (Exception e) {
            throw new SettingsImportExportException(e);
        }
    }

    private static void skipToEndTag(XmlPullParser xpp, String endTag) throws XmlPullParserException, IOException {
        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {
            eventType = xpp.next();
        }
    }

    private static String getText(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int eventType = xpp.next();
        if (eventType != XmlPullParser.TEXT) {
            return "";
        }
        return xpp.getText();
    }

    private static Imported parseRoot(XmlPullParser xpp, boolean globalSettings, List<String> accountUuids,
            boolean overview) throws XmlPullParserException, IOException, SettingsImportExportException {

        Imported result = new Imported();

        String fileFormatVersionString = xpp.getAttributeValue(null, SettingsExporter.FILE_FORMAT_ATTRIBUTE);
        validateFileFormatVersion(fileFormatVersionString);

        String contentVersionString = xpp.getAttributeValue(null, SettingsExporter.VERSION_ATTRIBUTE);
        result.contentVersion = validateContentVersion(contentVersionString);

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ROOT_ELEMENT.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.GLOBAL_ELEMENT.equals(element)) {
                    if (overview || globalSettings) {
                        if (result.globalSettings == null) {
                            if (overview) {
                                result.globalSettings = new ImportedSettings();
                                skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT);
                            } else {
                                result.globalSettings = parseSettings(xpp, SettingsExporter.GLOBAL_ELEMENT);
                            }
                        } else {
                            skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT);
                            Timber.w("More than one global settings element. Only using the first one!");
                        }
                    } else {
                        skipToEndTag(xpp, SettingsExporter.GLOBAL_ELEMENT);
                        Timber.i("Skipping global settings");
                    }
                } else if (SettingsExporter.ACCOUNTS_ELEMENT.equals(element)) {
                    if (result.accounts == null) {
                        result.accounts = parseAccounts(xpp, accountUuids, overview);
                    } else {
                        Timber.w("More than one accounts element. Only using the first one!");
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return result;
    }

    private static int validateFileFormatVersion(String versionString) throws SettingsImportExportException {
        if (versionString == null) {
            throw new SettingsImportExportException("Missing file format version");
        }

        int version;
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            throw new SettingsImportExportException("Invalid file format version: " + versionString);
        }

        if (version != SettingsExporter.FILE_FORMAT_VERSION) {
            throw new SettingsImportExportException("Unsupported file format version: " + versionString);
        }

        return version;
    }

    private static int validateContentVersion(String versionString) throws SettingsImportExportException {
        if (versionString == null) {
            throw new SettingsImportExportException("Missing content version");
        }

        int version;
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            throw new SettingsImportExportException("Invalid content version: " + versionString);
        }

        if (version < 1) {
            throw new SettingsImportExportException("Unsupported content version: " + versionString);
        }

        return version;
    }

    private static ImportedSettings parseSettings(XmlPullParser xpp, String endTag)
            throws XmlPullParserException, IOException {

        ImportedSettings result = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {

            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.VALUE_ELEMENT.equals(element)) {
                    String key = xpp.getAttributeValue(null, SettingsExporter.KEY_ATTRIBUTE);
                    String value = getText(xpp);

                    if (result == null) {
                        result = new ImportedSettings();
                    }

                    if (result.settings.containsKey(key)) {
                        Timber.w("Already read key \"%s\". Ignoring value \"%s\"", key, value);
                    } else {
                        result.settings.put(key, value);
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return result;
    }

    private static Map<String, ImportedAccount> parseAccounts(XmlPullParser xpp, List<String> accountUuids,
            boolean overview) throws XmlPullParserException, IOException {

        Map<String, ImportedAccount> accounts = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ACCOUNTS_ELEMENT.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.ACCOUNT_ELEMENT.equals(element)) {
                    if (accounts == null) {
                        accounts = new HashMap<>();
                    }

                    ImportedAccount account = parseAccount(xpp, accountUuids, overview);

                    if (account == null) {
                        // Do nothing - parseAccount() already logged a message
                    } else if (!accounts.containsKey(account.uuid)) {
                        accounts.put(account.uuid, account);
                    } else {
                        Timber.w("Duplicate account entries with UUID %s. Ignoring!", account.uuid);
                    }
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return accounts;
    }

    private static ImportedAccount parseAccount(XmlPullParser xpp, List<String> accountUuids, boolean overview)
            throws XmlPullParserException, IOException {

        String uuid = xpp.getAttributeValue(null, SettingsExporter.UUID_ATTRIBUTE);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            skipToEndTag(xpp, SettingsExporter.ACCOUNT_ELEMENT);
            Timber.w("Skipping account with invalid UUID %s", uuid);
            return null;
        }

        ImportedAccount account = new ImportedAccount();
        account.uuid = uuid;

        if (overview || accountUuids.contains(uuid)) {
            int eventType = xpp.next();
            while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ACCOUNT_ELEMENT.equals(xpp.getName()))) {
                if (eventType == XmlPullParser.START_TAG) {
                    String element = xpp.getName();
                    if (SettingsExporter.NAME_ELEMENT.equals(element)) {
                        account.name = getText(xpp);
                    } else if (SettingsExporter.INCOMING_SERVER_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.INCOMING_SERVER_ELEMENT);
                        } else {
                            account.incoming = parseServerSettings(xpp, SettingsExporter.INCOMING_SERVER_ELEMENT);
                        }
                    } else if (SettingsExporter.OUTGOING_SERVER_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.OUTGOING_SERVER_ELEMENT);
                        } else {
                            account.outgoing = parseServerSettings(xpp, SettingsExporter.OUTGOING_SERVER_ELEMENT);
                        }
                    } else if (SettingsExporter.SETTINGS_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.SETTINGS_ELEMENT);
                        } else {
                            account.settings = parseSettings(xpp, SettingsExporter.SETTINGS_ELEMENT);
                        }
                    } else if (SettingsExporter.IDENTITIES_ELEMENT.equals(element)) {
                        account.identities = parseIdentities(xpp);
                    } else if (SettingsExporter.FOLDERS_ELEMENT.equals(element)) {
                        if (overview) {
                            skipToEndTag(xpp, SettingsExporter.FOLDERS_ELEMENT);
                        } else {
                            account.folders = parseFolders(xpp);
                        }
                    } else {
                        Timber.w("Unexpected start tag: %s", xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } else {
            skipToEndTag(xpp, SettingsExporter.ACCOUNT_ELEMENT);
            Timber.i("Skipping account with UUID %s", uuid);
        }

        // If we couldn't find an account name use the UUID
        if (account.name == null) {
            account.name = uuid;
        }

        return account;
    }

    private static ImportedServer parseServerSettings(XmlPullParser xpp, String endTag)
            throws XmlPullParserException, IOException {
        ImportedServer server = new ImportedServer();

        server.type = xpp.getAttributeValue(null, SettingsExporter.TYPE_ATTRIBUTE);

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.HOST_ELEMENT.equals(element)) {
                    server.host = getText(xpp);
                } else if (SettingsExporter.PORT_ELEMENT.equals(element)) {
                    server.port = getText(xpp);
                } else if (SettingsExporter.CONNECTION_SECURITY_ELEMENT.equals(element)) {
                    server.connectionSecurity = getText(xpp);
                } else if (SettingsExporter.AUTHENTICATION_TYPE_ELEMENT.equals(element)) {
                    String text = getText(xpp);
                    server.authenticationType = AuthType.valueOf(text);
                } else if (SettingsExporter.USERNAME_ELEMENT.equals(element)) {
                    server.username = getText(xpp);
                } else if (SettingsExporter.CLIENT_CERTIFICATE_ALIAS_ELEMENT.equals(element)) {
                    server.clientCertificateAlias = getText(xpp);
                } else if (SettingsExporter.PASSWORD_ELEMENT.equals(element)) {
                    server.password = getText(xpp);
                } else if (SettingsExporter.EXTRA_ELEMENT.equals(element)) {
                    server.extras = parseSettings(xpp, SettingsExporter.EXTRA_ELEMENT);
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return server;
    }

    private static List<ImportedIdentity> parseIdentities(XmlPullParser xpp)
            throws XmlPullParserException, IOException {
        List<ImportedIdentity> identities = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.IDENTITIES_ELEMENT.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.IDENTITY_ELEMENT.equals(element)) {
                    if (identities == null) {
                        identities = new ArrayList<>();
                    }

                    ImportedIdentity identity = parseIdentity(xpp);
                    identities.add(identity);
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return identities;
    }

    private static ImportedIdentity parseIdentity(XmlPullParser xpp) throws XmlPullParserException, IOException {
        ImportedIdentity identity = new ImportedIdentity();

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.IDENTITY_ELEMENT.equals(xpp.getName()))) {

            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.NAME_ELEMENT.equals(element)) {
                    identity.name = getText(xpp);
                } else if (SettingsExporter.EMAIL_ELEMENT.equals(element)) {
                    identity.email = getText(xpp);
                } else if (SettingsExporter.DESCRIPTION_ELEMENT.equals(element)) {
                    identity.description = getText(xpp);
                } else if (SettingsExporter.SETTINGS_ELEMENT.equals(element)) {
                    identity.settings = parseSettings(xpp, SettingsExporter.SETTINGS_ELEMENT);
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return identity;
    }

    private static List<ImportedFolder> parseFolders(XmlPullParser xpp) throws XmlPullParserException, IOException {
        List<ImportedFolder> folders = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.FOLDERS_ELEMENT.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.FOLDER_ELEMENT.equals(element)) {
                    if (folders == null) {
                        folders = new ArrayList<>();
                    }

                    ImportedFolder folder = parseFolder(xpp);
                    folders.add(folder);
                } else {
                    Timber.w("Unexpected start tag: %s", xpp.getName());
                }
            }
            eventType = xpp.next();
        }

        return folders;
    }

    private static ImportedFolder parseFolder(XmlPullParser xpp) throws XmlPullParserException, IOException {
        ImportedFolder folder = new ImportedFolder();

        String name = xpp.getAttributeValue(null, SettingsExporter.NAME_ATTRIBUTE);
        folder.name = name;

        folder.settings = parseSettings(xpp, SettingsExporter.FOLDER_ELEMENT);

        return folder;
    }

    private static String getAccountDisplayName(ImportedAccount account) {
        String name = account.name;
        if (TextUtils.isEmpty(name) && account.identities != null && account.identities.size() > 0) {
            name = account.identities.get(0).email;
        }
        return name;
    }

    private static class ImportedServerSettings extends ServerSettings {
        private final ImportedServer importedServer;

        public ImportedServerSettings(ImportedServer server) {
            super(ServerSettings.Type.valueOf(server.type), server.host, convertPort(server.port),
                    convertConnectionSecurity(server.connectionSecurity),
                    server.authenticationType, server.username, server.password,
                    server.clientCertificateAlias);
            importedServer = server;
        }

        @Override
        public Map<String, String> getExtra() {
            return (importedServer.extras != null) ?
                    Collections.unmodifiableMap(importedServer.extras.settings) : null;
        }

        private static int convertPort(String port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private static ConnectionSecurity convertConnectionSecurity(String connectionSecurity) {
            try {
                /*
                 * TODO:
                 * Add proper settings validation and upgrade capability for server settings.
                 * Once that exists, move this code into a SettingsUpgrader.
                 */
                if ("SSL_TLS_OPTIONAL".equals(connectionSecurity)) {
                    return ConnectionSecurity.SSL_TLS_REQUIRED;
                } else if ("STARTTLS_OPTIONAL".equals(connectionSecurity)) {
                    return ConnectionSecurity.STARTTLS_REQUIRED;
                }
                return ConnectionSecurity.valueOf(connectionSecurity);
            } catch (Exception e) {
                return ConnectionSecurity.SSL_TLS_REQUIRED;
            }
        }
    }

    @VisibleForTesting
    static class Imported {
        public int contentVersion;
        public ImportedSettings globalSettings;
        public Map<String, ImportedAccount> accounts;
    }

    private static class ImportedSettings {
        public Map<String, String> settings = new HashMap<>();
    }

    @VisibleForTesting
    static class ImportedAccount {
        public String uuid;
        public String name;
        public ImportedServer incoming;
        public ImportedServer outgoing;
        public ImportedSettings settings;
        public List<ImportedIdentity> identities;
        public List<ImportedFolder> folders;
    }

    @VisibleForTesting
    static class ImportedServer {
        public String type;
        public String host;
        public String port;
        public String connectionSecurity;
        public AuthType authenticationType;
        public String username;
        public String password;
        public String clientCertificateAlias;
        public ImportedSettings extras;
    }

    @VisibleForTesting
    static class ImportedIdentity {
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
