package com.fsck.k9.preferences;


import android.content.Context;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.widget.EditText;

import com.fsck.k9.core.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


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

    public void updateRestrictions(Context context){
        RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle appRestrictions = restrictionsManager.getApplicationRestrictions();
        this.accountType = appRestrictions.getString("accountType");
        //    IMAP Configs
        this.imapServer = appRestrictions.getString("imapServer");
        this.imapSecurity = appRestrictions.getString("imapSecurity");
        this.imapPort = appRestrictions.getInt("imapPort");
        this.imapUsername = appRestrictions.getString("imapUsername");
        this.imapAuthentication = appRestrictions.getString("imapAuthentication");
        this.autoDetectImapNamespace = appRestrictions.getBoolean("autoDetectImapNamespace");
        this.compressionOnMobile = appRestrictions.getBoolean("compressionOnMobile");
        this.compressionOnWiFi = appRestrictions.getBoolean("compressionOnWiFi");
        this.compressionOnOther = appRestrictions.getBoolean("compressionOnOther");
        //    SMTP Configs
        this.smtpServer = appRestrictions.getString("smtpServer");
        this.smtpSecurity = appRestrictions.getString("smtpSecurity");
        this.smtpPort = appRestrictions.getInt("smtpPort");
        this.requireSignIn = appRestrictions.getBoolean("requireSignIn");
        this.smtpUsername = appRestrictions.getString("smtpUsername");
        this.smtpAuthentication = appRestrictions.getString("smtpAuthentication");
        // POP3 Config
        this.pop3Server= appRestrictions.getString("pop3Server");
        this.pop3Security= appRestrictions.getString("pop3Security");
        this.pop3Port= appRestrictions.getInt("pop3Port");
        this.pop3Username= appRestrictions.getString("pop3Username");
        this.pop3Authentication= appRestrictions.getString("pop3Authentication");
        //    General Config
        this.folderPollFrequency= appRestrictions.getInt("folderPollFrequency");
        this.numberOfMessagesToDisplay= appRestrictions.getInt("numberOfMessagesToDisplay");
        this.notifyMeWhenMailArrives= appRestrictions.getBoolean("notifyMeWhenMailArrives");
        this.accountName= appRestrictions.getString("imapServer");
        this.senderName= appRestrictions.getString("imapServer");
    }

    public String getAccountName() {
        return accountName;
    }






}
