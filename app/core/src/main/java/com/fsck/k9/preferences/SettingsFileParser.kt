package com.fsck.k9.preferences;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fsck.k9.mail.AuthType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import timber.log.Timber;


class SettingsFileParser {
    Imported parseSettings(InputStream inputStream, boolean globalSettings, List<String> accountUuids,
        boolean overview) throws SettingsImportExportException {

        if (!overview && accountUuids == null) {
            throw new IllegalArgumentException("Argument 'accountUuids' must not be null.");
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
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

    private Imported parseRoot(XmlPullParser xpp, boolean globalSettings, List<String> accountUuids,
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

    private int validateFileFormatVersion(String versionString) throws SettingsImportExportException {
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

    private int validateContentVersion(String versionString) throws SettingsImportExportException {
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

    private ImportedSettings parseSettings(XmlPullParser xpp, String endTag)
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

    private Map<String, ImportedAccount> parseAccounts(XmlPullParser xpp, List<String> accountUuids,
        boolean overview) throws XmlPullParserException, IOException {

        Map<String, ImportedAccount> accounts = null;

        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && SettingsExporter.ACCOUNTS_ELEMENT.equals(xpp.getName()))) {
            if (eventType == XmlPullParser.START_TAG) {
                String element = xpp.getName();
                if (SettingsExporter.ACCOUNT_ELEMENT.equals(element)) {
                    if (accounts == null) {
                        accounts = new LinkedHashMap<>();
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

    private ImportedAccount parseAccount(XmlPullParser xpp, List<String> accountUuids, boolean overview)
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

    private ImportedServer parseServerSettings(XmlPullParser xpp, String endTag)
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

    private List<ImportedIdentity> parseIdentities(XmlPullParser xpp)
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

    private ImportedIdentity parseIdentity(XmlPullParser xpp) throws XmlPullParserException, IOException {
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

    private List<ImportedFolder> parseFolders(XmlPullParser xpp) throws XmlPullParserException, IOException {
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

    private ImportedFolder parseFolder(XmlPullParser xpp) throws XmlPullParserException, IOException {
        ImportedFolder folder = new ImportedFolder();

        folder.name = xpp.getAttributeValue(null, SettingsExporter.NAME_ATTRIBUTE);

        folder.settings = parseSettings(xpp, SettingsExporter.FOLDER_ELEMENT);

        return folder;
    }

    private String getText(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int eventType = xpp.next();
        if (eventType != XmlPullParser.TEXT) {
            return "";
        }
        return xpp.getText();
    }

    private void skipToEndTag(XmlPullParser xpp, String endTag) throws XmlPullParserException, IOException {
        int eventType = xpp.next();
        while (!(eventType == XmlPullParser.END_TAG && endTag.equals(xpp.getName()))) {
            eventType = xpp.next();
        }
    }
}
