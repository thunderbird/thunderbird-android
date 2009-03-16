package com.android.email.mail.internet;

import com.android.email.mail.internet.CodePage;

/**
 * This class is the specific code page for Provision in the ActiveSync protocol.
 * The code page number is 14.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class ProvisionCodePage extends CodePage {
    /**
     * Constructor for ProvisionCodePage.  Initializes all of the code page values.
     */
    public ProvisionCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Provision", 0x05);
        codepageTokens.put("Policies", 0x06);
        codepageTokens.put("Policy", 0x07);
        codepageTokens.put("PolicyType", 0x08);
        codepageTokens.put("PolicyKey", 0x09);
        codepageTokens.put("Data", 0x0a);
        codepageTokens.put("Status", 0x0b);
        codepageTokens.put("RemoteWipe", 0x0c);
        codepageTokens.put("EASProvisionDoc", 0x0d);
        codepageTokens.put("DevicePasswordEnabled", 0x0e);
        codepageTokens.put("AlphanumericDevicePasswordRequired", 0x0f);
        codepageTokens.put("DeviceEncryptionEnabled", 0x10);
        codepageTokens.put("PasswordRecoveryEnabled", 0x11);
        codepageTokens.put("DocumentBrowseEnabled", 0x12);
        codepageTokens.put("AttachmentsEnabled", 0x13);
        codepageTokens.put("MinDevicePasswordLength", 0x14);
        codepageTokens.put("MaxInactivityTimeDeviceLock", 0x15);
        codepageTokens.put("MaxDevicePasswordFailedAttempts", 0x16);
        codepageTokens.put("MaxAttachmentSize", 0x17);
        codepageTokens.put("AllowSimpleDevicePassword", 0x18);
        codepageTokens.put("DevicePasswordExpiration", 0x19);
        codepageTokens.put("DevicePasswordHistory", 0x1a);
        codepageTokens.put("AllowStorageCard", 0x1b);
        codepageTokens.put("AllowCamera", 0x1c);
        codepageTokens.put("RequireDeviceEncryption", 0x1d);
        codepageTokens.put("AllowUnsignedApplications", 0x1e);
        codepageTokens.put("AllowUnsignedInstallationPackages", 0x1f);
        codepageTokens.put("MinDevicePasswordComplexCharacters", 0x20);
        codepageTokens.put("AllowWiFi", 0x21);
        codepageTokens.put("AllowTextMessaging", 0x22);
        codepageTokens.put("AllowPOPIMAPEmail", 0x23);
        codepageTokens.put("AllowBluetooth", 0x24);
        codepageTokens.put("AllowIrDA", 0x25);
        codepageTokens.put("RequireManualSyncWhenRoaming", 0x26);
        codepageTokens.put("AllowDesktopSync", 0x27);
        codepageTokens.put("MaxCalendarAgeFilter", 0x28);
        codepageTokens.put("AllowHTMLEmail", 0x29);
        codepageTokens.put("MaxEmailAgeFilter", 0x2a);
        codepageTokens.put("MaxEmailBodyTruncationSize", 0x2b);
        codepageTokens.put("MaxEmailHTMLBodyTruncationSize", 0x2c);
        codepageTokens.put("RequireSignedSMIMEMessages", 0x2d);
        codepageTokens.put("RequireEncryptedSMIMEMessages", 0x2e);
        codepageTokens.put("RequireSignedSMIMEAlgorithm", 0x2f);
        codepageTokens.put("RequireEncryptionSMIMEAlgorithm", 0x30);
        codepageTokens.put("AllowSMIMEEncryptionAlgorithmNegotiation", 0x31);
        codepageTokens.put("AllowSMIMESoftCerts", 0x32);
        codepageTokens.put("AllowBrowser", 0x33);
        codepageTokens.put("AllowConsumerEmail", 0x34);
        codepageTokens.put("AllowRemoteDesktop", 0x35);
        codepageTokens.put("AllowInternetSharing", 0x36);
        codepageTokens.put("UnapprovedInROMApplicationList", 0x37);
        codepageTokens.put("ApplicationName", 0x38);
        codepageTokens.put("ApprovedApplicationList", 0x39);
        codepageTokens.put("Hash", 0x3a);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x0e;
        codePageName = "Provision";
    }
}
