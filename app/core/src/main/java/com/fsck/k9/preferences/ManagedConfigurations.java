package com.fsck.k9.preferences;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.RestrictionsManager;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;


//
//
//
//@Author ItsTimetoforget <Zusel7.Zusel7@gmail.com
//
//
//


public class ManagedConfigurations {

    private String accountType;
    //    IMAP Configs
    private String imapServer;
    private String imapSecurity;
    private Integer imapPort;
    private String imapUsername;
    private String imapAuthentication;
    private boolean autoDetectImapNamespace = false;
    private String imapPrefix;
    private boolean compressionOnMobile = false;
    private boolean compressionOnWiFi = false;
    private boolean compressionOnOther = false;
    //    SMTP Configs
    private String smtpServer;
    private String smtpSecurity;
    private Integer smtpPort;
    private boolean requireSignIn;
    private String smtpUsername;
    private String smtpAuthentication;
    // POP3 Config
    private String pop3Server;
    private String pop3Security;
    private Integer pop3Port;
    private String pop3Username;
    private String pop3Authentication;
    //    General Config
    private Integer folderPollFrequency;
    private Integer numberOfMessagesToDisplay;
    private boolean notifyMeWhenMailArrives;
    private String accountName;
    private String senderName;
    private String email;
    //  Folder Config
    private String archiveFolder;
    private String draftFolder;
    private String sentFolder;
    private String trashFolder;

    public static String getRestrictionasString(Context context, String restrictionName) {
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle appRestrictions = restrictionsManager.getApplicationRestrictions();
        try {
            return appRestrictions.getString(restrictionName);
        } catch (NullPointerException e) {
            return "";
        }
    }

    //checks if all required settings are available
    public static Boolean autoConfigAvailable(Context context) {
        Boolean available = false;
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = restrictionsManager.getApplicationRestrictions();
        //accountType
        String accountType = restrictions.getString("accountType");
        //General
        Bundle general = (Bundle) restrictions.get("General");
        String email = general.getString("email");
        // IMAP
        Bundle imap = (Bundle) restrictions.get("IMAP");
        String imapServer = imap.getString("imapServer");
        Integer imapPort = imap.getInt("imapPort");
        //POP3
        Bundle pop3 = (Bundle) restrictions.get("POP3");
        String pop3Server = pop3.getString("pop3Server");
        Integer pop3Port = pop3.getInt("pop3Port");
        //SMTP
        Bundle smtp = (Bundle) restrictions.get("SMTP");
        String smtpServer = smtp.getString("smtpServer");
        Integer smtpPort = smtp.getInt("smtpPort");
        if (accountType.equalsIgnoreCase("imap")) {
            if (imapServer != null && imapPort != 0 && smtpPort != 0 && smtpServer != null && email != null) {
                available = true;
            }
        } else if (accountType.equalsIgnoreCase("pop3")) {
            if (pop3Port != 0 && pop3Server != null && smtpPort != 0 && smtpServer != null && email != null) {
                available = true;
            }
        }

        return available;
    }

    public static String getEmail(Context context) {
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = restrictionsManager.getApplicationRestrictions();
        Bundle general = (Bundle) restrictions.get("General");
        return general.getString("email");
    }

    public void updateRestrictions(Context context) {
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle appRestrictions = restrictionsManager.getApplicationRestrictions();
        this.accountType = appRestrictions.getString("accountType");
        //    IMAP Configs
        Bundle imap = (Bundle) appRestrictions.get("IMAP");
        this.imapServer = imap.getString("imapServer");
        this.imapSecurity = imap.getString("imapSecurity");
        this.imapPort = imap.getInt("imapPort");
        this.imapUsername = imap.getString("imapUsername");
        this.imapAuthentication = imap.getString("imapAuthentication");
        this.autoDetectImapNamespace = imap.getBoolean("autoDetectImapNamespace");
        this.imapPrefix = imap.getString("imapPrefix");
        this.compressionOnMobile = imap.getBoolean("compressionOnMobile");
        this.compressionOnWiFi = imap.getBoolean("compressionOnWiFi");
        this.compressionOnOther = imap.getBoolean("compressionOnOther");
        //    SMTP Configs
        Bundle smtp = (Bundle) appRestrictions.get("SMTP");
        this.smtpServer = smtp.getString("smtpServer");
        this.smtpSecurity = smtp.getString("smtpSecurity");
        this.smtpPort = smtp.getInt("smtpPort");
        this.requireSignIn = smtp.getBoolean("requireSignIn");
        this.smtpUsername = smtp.getString("smtpUsername");
        this.smtpAuthentication = smtp.getString("smtpAuthentication");
        // POP3 Config
        Bundle pop3 = (Bundle) appRestrictions.get("POP3");
        this.pop3Server = pop3.getString("pop3Server");
        this.pop3Security = pop3.getString("pop3Security");
        this.pop3Port = pop3.getInt("pop3Port");
        this.pop3Username = pop3.getString("pop3Username");
        this.pop3Authentication = pop3.getString("pop3Authentication");
        //    General Config
        Bundle general = (Bundle) appRestrictions.get("General");
        this.folderPollFrequency = general.getInt("folderPollFrequency");
        this.numberOfMessagesToDisplay = general.getInt("numberOfMessagesToDisplay");
        this.notifyMeWhenMailArrives = general.getBoolean("notifyMeWhenMailArrives");
        this.accountName = general.getString("imapServer");
        this.senderName = general.getString("imapServer");
        this.email = general.getString("email");
        //  Folder Config
        Bundle folder = (Bundle) appRestrictions.get("Folder");
        this.archiveFolder = folder.getString("archiveFolder");
        this.draftFolder = folder.getString("draftFolder");
        this.sentFolder = folder.getString("draftFolder");
        this.trashFolder = folder.getString("draftFolder");
    }

    public String getEmail() {
        return this.email;
    }

    public String getAccountName() {
        if (this.accountName == "") {
            return null;
        }
        return null;
    }

    public ServerSettings getIncomingServerSettings(String password) {
        ServerSettings incoming = null;
        if (accountType.equalsIgnoreCase("imap")) {
            ConnectionSecurity imapSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            AuthType authType = AuthType.PLAIN;
            String imapUsername = email;
            Map<String, String> extra = new HashMap<>();
            String autoDetectNamespace = "true";
            String pathPrefix = "";
            if (this.imapSecurity == null || this.imapSecurity.equals("")) {
                if (imapPort.equals(143)) {
                    imapSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
                }
                if (imapPort.equals(993)) {
                    imapSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
                }
            } else {
                if (this.imapSecurity.equalsIgnoreCase("none")) {
                    imapSecurity = ConnectionSecurity.NONE;
                } else if (this.imapSecurity.equalsIgnoreCase("starttls")) {
                    imapSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
                } else if (this.imapSecurity.equalsIgnoreCase("ssl/tls")) {
                    imapSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
                }
            }
            if (imapAuthentication != null) {
                if (imapAuthentication.equalsIgnoreCase("normal password")) {
                    authType = AuthType.PLAIN;
                } else if (imapAuthentication.equalsIgnoreCase("encrypted password")) {
                    authType = AuthType.CRAM_MD5;
                }
            }
            if (this.imapUsername != null) {
                imapUsername = this.imapUsername;
            }
            if (!this.autoDetectImapNamespace) {
                autoDetectNamespace = "false";
            }
            if (this.imapPrefix != null) {
                pathPrefix = this.imapPrefix;
            }
            extra.put("autoDetectNamespace", autoDetectNamespace);
            extra.put("pathPrefix", pathPrefix);
            incoming = new ServerSettings("imap", imapServer, imapPort, imapSecurity, authType, imapUsername, password,
                    null, extra);
            return incoming;
        } else if (accountType.equalsIgnoreCase("pop3")) {
            ConnectionSecurity pop3Security = ConnectionSecurity.SSL_TLS_REQUIRED;
            AuthType authType = AuthType.PLAIN;
            String pop3Username = email;
            if (this.pop3Security == null || this.pop3Security.equals("")) {
                if (pop3Port.equals(110)) {
                    pop3Security = ConnectionSecurity.STARTTLS_REQUIRED;
                } else if (pop3Port.equals(995)) {
                    pop3Security = ConnectionSecurity.SSL_TLS_REQUIRED;
                }
            } else {
                if (this.pop3Security.equalsIgnoreCase("none")) {
                    pop3Security = ConnectionSecurity.NONE;
                } else if (this.pop3Security.equalsIgnoreCase("starttls")) {
                    pop3Security = ConnectionSecurity.STARTTLS_REQUIRED;
                } else if (this.pop3Security.equalsIgnoreCase("ssl/tls")) {
                    pop3Security = ConnectionSecurity.SSL_TLS_REQUIRED;
                }
            }
            if (pop3Authentication != null) {
                if (pop3Authentication.equalsIgnoreCase("normal password")) {
                    authType = AuthType.PLAIN;
                } else if (pop3Authentication.equalsIgnoreCase("encrypted password")) {
                    authType = AuthType.CRAM_MD5;
                }
            }
            if (this.pop3Username != null) {
                pop3Username = this.pop3Username;
            }
            incoming = new ServerSettings("pop3", pop3Server, pop3Port, pop3Security, authType, pop3Username, password,
                    null);
        }
        return incoming;
    }

    public ServerSettings getOutgoingServerSettings(String password) {
        ConnectionSecurity smtpSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
        AuthType authType = AuthType.PLAIN;
        String smtpUsername = email;
        if (this.smtpSecurity.equals("") || this.smtpSecurity == null) {
            if (smtpPort.equals(465)) {
                smtpSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            } else if (smtpPort.equals(587)) {
                smtpSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            } else if (smtpPort.equals(25)) {
                smtpSecurity = ConnectionSecurity.NONE;
            }
        } else {
            if (this.smtpSecurity.equalsIgnoreCase("none")) {
                smtpSecurity = ConnectionSecurity.NONE;
            } else if (this.smtpSecurity.equalsIgnoreCase("starttls")) {
                smtpSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            } else if (this.smtpSecurity.equalsIgnoreCase("ssl/tls")) {
                smtpSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            }
        }
        if (smtpAuthentication != null) {
            if (smtpAuthentication.equalsIgnoreCase("normal password")) {
                authType = AuthType.PLAIN;
            } else if (smtpAuthentication.equalsIgnoreCase("encrypted password")) {
                authType = AuthType.CRAM_MD5;
            }
        }
        if (smtpUsername != null) {
            smtpUsername = this.smtpUsername;
        }
        if (this.requireSignIn == true) {
            ServerSettings outgoing =
                    new ServerSettings("smtp", smtpServer, smtpPort, smtpSecurity, authType, smtpUsername, password,
                            null);
            return outgoing;
        } else {
            ServerSettings outgoing =
                    new ServerSettings("smtp", smtpServer, smtpPort, smtpSecurity, authType, "", "", null);
            return outgoing;
        }

    }

    public int getDisplayCount() {
        if (this.numberOfMessagesToDisplay == 0) {
            return 25;
        } else {
            return this.numberOfMessagesToDisplay;
        }
    }

    public int getCheckInterval() {
        if (this.folderPollFrequency == 0) {
            return 60;
        } else {
            return folderPollFrequency;
        }
    }

    public boolean getCompressionOnMobile() {
        return compressionOnMobile;
    }

    public boolean getCompressionOnWiFi() {
        return compressionOnWiFi;
    }

    public boolean getCompressionOnOther() {
        return compressionOnOther;
    }

    public void setFolders(Account mAccount) {
        if (this.sentFolder != null) {
            mAccount.setImportedSentFolder(this.sentFolder);
        }
        if (this.archiveFolder != null) {
            mAccount.setImportedArchiveFolder(this.archiveFolder);
        }
        if (this.trashFolder != null) {
            mAccount.setImportedTrashFolder(this.trashFolder);
        }
        if (this.draftFolder != null) {
            mAccount.setImportedDraftsFolder(this.draftFolder);
        }
    }

    public boolean getNotificationOnNewMail() {
        return this.notifyMeWhenMailArrives;
    }

    public String getSenderName() {
        if (this.senderName.equals("")) {
            return null;
        }
        return this.senderName;
    }
}
