package com.fsck.k9.preferences;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.AccountPreferenceSerializer;
import com.fsck.k9.Core;
import com.fsck.k9.DI;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.ServerSettingsSerializer;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import kotlinx.datetime.Clock;
import timber.log.Timber;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;


public class SettingsImporter {
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
            SettingsFileParser settingsFileParser = new SettingsFileParser();
            Imported imported = settingsFileParser.parseSettings(inputStream, false, null, true);

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

            SettingsFileParser settingsFileParser = new SettingsFileParser();
            Imported imported = settingsFileParser.parseSettings(inputStream, globalSettings, accountUuids, false);

            Preferences preferences = Preferences.getPreferences();
            Storage storage = preferences.getStorage();

            if (globalSettings) {
                try {
                    StorageEditor editor = preferences.createStorageEditor();
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
                                StorageEditor editor = preferences.createStorageEditor();

                                AccountDescriptionPair importResult = importAccount(context, editor,
                                        imported.contentVersion, account, overwrite);

                                if (editor.commit()) {
                                    Timber.v("Committed settings for account \"%s\" to the settings database.",
                                            importResult.imported.name);

                                    // Add UUID of the account we just imported to the list of
                                    // account UUIDs
                                    if (!importResult.overwritten) {
                                        editor = preferences.createStorageEditor();

                                        String newUuid = importResult.imported.uuid;
                                        String oldAccountUuids = preferences.getStorage().getString("accountUuids", "");
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
                                String reason = e.getMessage();
                                if (TextUtils.isEmpty(reason)) {
                                    reason = "Unknown";
                                }
                                Timber.e(e, "Encountered invalid setting while importing account \"%s\", reason: \"%s\"",
                                        account.name, reason);

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

                    StorageEditor editor = preferences.createStorageEditor();

                    if (!editor.commit()) {
                        throw new SettingsImportExportException("Failed to set default account");
                    }
                } else {
                    Timber.w("Was asked to import at least one account but none found.");
                }
            }

            preferences.loadAccounts();

            SpecialLocalFoldersCreator localFoldersCreator = DI.get(SpecialLocalFoldersCreator.class);

            // Create special local folders
            for (AccountDescriptionPair importedAccount : importedAccounts) {
                String accountUuid = importedAccount.imported.uuid;
                Account account = preferences.getAccount(accountUuid);

                localFoldersCreator.createSpecialLocalFolders(account);
            }

            DI.get(RealGeneralSettingsManager.class).loadSettings();
            Core.setServicesEnabled(context);

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
        Map<String, Object> validatedSettings = GeneralSettingsDescriptions.validate(contentVersion, settings.settings);

        // Upgrade global settings to current content version
        if (contentVersion != Settings.VERSION) {
            GeneralSettingsDescriptions.upgrade(contentVersion, validatedSettings);
        }

        // Convert global settings to the string representation used in preference storage
        Map<String, String> stringSettings = GeneralSettingsDescriptions.convert(validatedSettings);

        // Use current global settings as base and overwrite with validated settings read from the import file.
        Map<String, String> mergedSettings = new HashMap<>(GeneralSettingsDescriptions.getGlobalSettings(storage));
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

        Preferences prefs = Preferences.getPreferences();
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
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.ACCOUNT_DESCRIPTION_KEY, accountName);

        if (account.incoming == null) {
            // We don't import accounts without incoming server settings
            throw new InvalidSettingValueException("Missing incoming server settings");
        }

        // Write incoming server settings
        ServerSettings incoming = createServerSettings(account.incoming);
        ServerSettingsSerializer serverSettingsSerializer = DI.get(ServerSettingsSerializer.class);
        String incomingServer = serverSettingsSerializer.serialize(incoming);
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.INCOMING_SERVER_SETTINGS_KEY, incomingServer);

        String incomingServerName = incoming.host;
        boolean incomingPasswordNeeded = AuthType.EXTERNAL != incoming.authenticationType &&
                AuthType.XOAUTH2 != incoming.authenticationType &&
                (incoming.password == null || incoming.password.isEmpty());

        boolean authorizationNeeded = incoming.authenticationType == AuthType.XOAUTH2;

        if (account.outgoing == null) {
            throw new InvalidSettingValueException("Missing outgoing server settings");
        }

        String outgoingServerName = null;
        boolean outgoingPasswordNeeded = false;
        // Write outgoing server settings
        ServerSettings outgoing = createServerSettings(account.outgoing);
        String outgoingServer = serverSettingsSerializer.serialize(outgoing);
        putString(editor, accountKeyPrefix + AccountPreferenceSerializer.OUTGOING_SERVER_SETTINGS_KEY, outgoingServer);

        /*
         * Mark account as disabled if the settings file contained a username but no password, except when the
         * AuthType is EXTERNAL.
         */
        outgoingPasswordNeeded = AuthType.EXTERNAL != outgoing.authenticationType &&
                AuthType.XOAUTH2 != outgoing.authenticationType &&
                outgoing.username != null &&
                !outgoing.username.isEmpty() &&
                (outgoing.password == null || outgoing.password.isEmpty());

        authorizationNeeded |= outgoing.authenticationType == AuthType.XOAUTH2;

        outgoingServerName = outgoing.host;

        boolean createAccountDisabled = incomingPasswordNeeded || outgoingPasswordNeeded || authorizationNeeded;
        if (createAccountDisabled) {
            editor.putBoolean(accountKeyPrefix + "enabled", false);
        }

        // Validate account settings
        Map<String, Object> validatedSettings =
                AccountSettingsDescriptions.validate(contentVersion, account.settings.settings, !mergeImportedAccount);

        // Upgrade account settings to current content version
        if (contentVersion != Settings.VERSION) {
            AccountSettingsDescriptions.upgrade(contentVersion, validatedSettings);
        }

        // Convert account settings to the string representation used in preference storage
        Map<String, String> stringSettings = AccountSettingsDescriptions.convert(validatedSettings);

        // Merge account settings if necessary
        Map<String, String> writeSettings;
        if (mergeImportedAccount) {
            writeSettings = new HashMap<>(AccountSettingsDescriptions.getAccountSettings(prefs.getStorage(), uuid));
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
            int newAccountNumber = prefs.generateAccountNumber();
            putString(editor, accountKeyPrefix + "accountNumber", Integer.toString(newAccountNumber));
        }

        // Write identities
        if (account.identities != null) {
            importIdentities(editor, contentVersion, uuid, account, overwrite, existingAccount, prefs);
        } else if (!mergeImportedAccount) {
            // Require accounts to at least have one identity
            throw new InvalidSettingValueException("Missing identities, there should be at least one.");
        }

        // Write folder settings
        if (account.folders != null) {
            for (ImportedFolder folder : account.folders) {
                importFolder(editor, contentVersion, uuid, folder, mergeImportedAccount, prefs);
            }
        }

        // When deleting an account and then restoring it using settings import, the same account UUID will be used.
        // To avoid reusing a previously existing notification channel ID, we need to make sure to use a unique value
        // for `messagesNotificationChannelVersion`.
        Clock clock = DI.get(Clock.class);
        String messageNotificationChannelVersion = Long.toString(clock.now().getEpochSeconds());
        putString(editor, accountKeyPrefix + "messagesNotificationChannelVersion", messageNotificationChannelVersion);

        AccountDescription imported = new AccountDescription(accountName, uuid);
        return new AccountDescriptionPair(original, imported, mergeImportedAccount, authorizationNeeded,
                incomingPasswordNeeded, outgoingPasswordNeeded, incomingServerName, outgoingServerName);
    }

    private static void importFolder(StorageEditor editor, int contentVersion, String uuid, ImportedFolder folder,
            boolean overwrite, Preferences prefs) {

        // Validate folder settings
        Map<String, Object> validatedSettings =
                FolderSettingsDescriptions.validate(contentVersion, folder.settings.settings, !overwrite);

        // Upgrade folder settings to current content version
        if (contentVersion != Settings.VERSION) {
            FolderSettingsDescriptions.upgrade(contentVersion, validatedSettings);
        }

        // Convert folder settings to the string representation used in preference storage
        Map<String, String> stringSettings = FolderSettingsDescriptions.convert(validatedSettings);

        // Merge folder settings if necessary
        Map<String, String> writeSettings;
        if (overwrite) {
            writeSettings = FolderSettingsDescriptions.getFolderSettings(prefs.getStorage(), uuid, folder.name);
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

            String identitySuffix = "." + writeIdentityIndex;

            // Write name used in identity
            String identityName = (identity.name == null) ? "" : identity.name;
            putString(editor, accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_NAME_KEY + identitySuffix, identityName);

            // Validate email address
            if (!IdentitySettingsDescriptions.isEmailAddressValid(identity.email)) {
                throw new InvalidSettingValueException("Invalid email address: " + identity.email);
            }

            // Write email address
            putString(editor, accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_EMAIL_KEY + identitySuffix, identity.email);

            // Write identity description
            if (identity.description != null) {
                putString(editor, accountKeyPrefix + AccountPreferenceSerializer.IDENTITY_DESCRIPTION_KEY + identitySuffix,
                        identity.description);
            }

            if (identity.settings != null) {
                // Validate identity settings
                Map<String, Object> validatedSettings = IdentitySettingsDescriptions.validate(
                        contentVersion, identity.settings.settings, !mergeSettings);

                // Upgrade identity settings to current content version
                if (contentVersion != Settings.VERSION) {
                    IdentitySettingsDescriptions.upgrade(contentVersion, validatedSettings);
                }

                // Convert identity settings to the representation used in preference storage
                Map<String, String> stringSettings = IdentitySettingsDescriptions.convert(validatedSettings);

                // Merge identity settings if necessary
                Map<String, String> writeSettings;
                if (mergeSettings) {
                    writeSettings = new HashMap<>(IdentitySettingsDescriptions.getIdentitySettings(
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

            if (account.getDisplayName().equals(name)) {
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
        if (K9.isDebugLoggingEnabled()) {
            String outputValue = value;
            if (!K9.isSensitiveDebugLoggingEnabled() &&
                    (key.endsWith("." + AccountPreferenceSerializer.OUTGOING_SERVER_SETTINGS_KEY) ||
                            key.endsWith("." + AccountPreferenceSerializer.INCOMING_SERVER_SETTINGS_KEY))) {
                outputValue = "*sensitive*";
            }
            Timber.v("Setting %s=%s", key, outputValue);
        }
        editor.putString(key, value);
    }

    private static String getAccountDisplayName(ImportedAccount account) {
        String name = account.name;
        if (TextUtils.isEmpty(name) && account.identities != null && account.identities.size() > 0) {
            name = account.identities.get(0).email;
        }
        return name;
    }

    private static ServerSettings createServerSettings(ImportedServer importedServer) {
        String type = ServerTypeConverter.toServerSettingsType(importedServer.type);
        int port = convertPort(importedServer.port);
        ConnectionSecurity connectionSecurity = convertConnectionSecurity(importedServer.connectionSecurity);
        String password = importedServer.authenticationType == AuthType.XOAUTH2 ? "" : importedServer.password;
        Map<String, String> extra = importedServer.extras != null ?
                unmodifiableMap(importedServer.extras.settings) : emptyMap();

        return new ServerSettings(type, importedServer.host, port, connectionSecurity,
                importedServer.authenticationType, importedServer.username, password,
                importedServer.clientCertificateAlias, extra);
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
