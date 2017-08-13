package com.fsck.k9.activity;


import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.activity.setup.CheckDirection;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.StoreConfig;


public interface AccountConfig extends StoreConfig {
    public ConnectionSecurity getIncomingSecurityType();
    public AuthType getIncomingAuthType();
    public String getIncomingPort();
    public ConnectionSecurity getOutgoingSecurityType();
    public AuthType getOutgoingAuthType();
    public String getOutgoingPort();
    public boolean isNotifyNewMail();
    public boolean isShowOngoing();
    public int getAutomaticCheckIntervalMinutes();
    public int getDisplayCount();
    public FolderMode getFolderPushMode();
    public String getName();
    DeletePolicy getDeletePolicy();

    void init(String email, String password);

    public String getEmail();
    public String getDescription();
    Store getRemoteStore() throws MessagingException;

    public void setName(String name);
    public void setDescription(String description);
    public void setDeletePolicy(DeletePolicy deletePolicy);
    public void setEmail(String email);
    public void setStoreUri(String storeUri);
    public void setTransportUri(String transportUri);
    void setCompression(NetworkType networkType, boolean useCompression);

    void addCertificate(CheckDirection direction, X509Certificate certificate) throws CertificateException;

    void setSubscribedFoldersOnly(boolean subscribedFoldersOnly);

    void deleteCertificate(String newHost, int newPort, CheckDirection direction);
}
