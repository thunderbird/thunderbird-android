package com.fsck.k9.activity.setup;


import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.ssl.LocalKeyStore;
import com.fsck.k9.mail.store.RemoteStore;


class ManualSetupInfo implements Parcelable {
    private String name;
    private String description;
    private String email;

    private String storeUri;
    private String transportUri;

    private String inboxFolderName = Account.INBOX;
    private String outboxFolderName = Account.OUTBOX;
    private String draftsFolderName;
    private String sentFolderName;
    private String trashFolderName;
    private String archiveFolderName;
    private String spamFolderName;
    private String autoExpandFolderName = Account.INBOX;

    private int maximumAutoDownloadMessageSize = 32768;
    private boolean subscribedFoldersOnly = false;

    private Map<NetworkType, Boolean> compressionMap = new HashMap<>();

    private boolean allowRemoteSearch = false;
    private boolean remoteSearchFullText = false;
    private boolean pushPollOnConnect = true;

    private int displayCount = K9.DEFAULT_VISIBLE_LIMIT;
    private int idleRefreshMinutes = 24;

    private ConnectionSecurity incomingSecurityType;
    private AuthType incomingAuthType;
    private String incomingPort;

    private ConnectionSecurity outgoingSecurityType;
    private AuthType outgoingAuthType;
    private String outgoingPort;

    private boolean notifyNewMail;
    private boolean showOngoing;
    private int automaticCheckIntervalMinutes = 5;
    private Account.FolderMode folderPushMode = FolderMode.FIRST_CLASS;

    private DeletePolicy deletePolicy = DeletePolicy.NEVER;

    ManualSetupInfo() {
    }

    public String getStoreUri() {
        return storeUri;
    }

    public String getTransportUri() {
        return transportUri;
    }

    public boolean subscribedFoldersOnly() {
        return subscribedFoldersOnly;
    }

    public boolean useCompression(NetworkType type) {
        Boolean useCompression = compressionMap.get(type);
        if (useCompression == null) {
            return true;
        }

        return useCompression;
    }

    public void setCompression(NetworkType networkType, boolean useCompression) {
        compressionMap.put(networkType, useCompression);
    }

    public String getInboxFolderName() {
        return inboxFolderName;
    }

    public String getOutboxFolderName() {
        return outboxFolderName;
    }

    public String getDraftsFolderName() {
        return draftsFolderName;
    }

    public String getArchiveFolderName() {
        return archiveFolderName;
    }

    public String getTrashFolderName() {
        return trashFolderName;
    }

    public String getSpamFolderName() {
        return spamFolderName;
    }

    public String getSentFolderName() {
        return sentFolderName;
    }

    public String getAutoExpandFolderName() {
        return autoExpandFolderName;
    }

    public void setArchiveFolderName(String name) {
        archiveFolderName = name;
    }

    public void setDraftsFolderName(String name) {
        draftsFolderName = name;
    }

    public void setTrashFolderName(String name) {
        trashFolderName = name;
    }

    public void setSpamFolderName(String name) {
        spamFolderName = name;
    }

    public void setSentFolderName(String name) {
        sentFolderName = name;
    }

    public void setAutoExpandFolderName(String name) {
        autoExpandFolderName = name;
    }

    public void setInboxFolderName(String name) {
        inboxFolderName = name;
    }

    public int getMaximumAutoDownloadMessageSize() {
        return maximumAutoDownloadMessageSize;
    }

    public boolean allowRemoteSearch() {
        return allowRemoteSearch;
    }

    public boolean isRemoteSearchFullText() {
        return remoteSearchFullText;
    }

    public boolean isPushPollOnConnect() {
        return pushPollOnConnect;
    }

    public ConnectionSecurity getIncomingSecurityType() {
        return incomingSecurityType;
    }

    public AuthType getIncomingAuthType() {
        return incomingAuthType;
    }

    public String getIncomingPort() {
        return incomingPort;
    }

    public ConnectionSecurity getOutgoingSecurityType() {
        return outgoingSecurityType;
    }

    public AuthType getOutgoingAuthType() {
        return outgoingAuthType;
    }

    public String getOutgoingPort() {
        return outgoingPort;
    }

    public boolean isNotifyNewMail() {
        return notifyNewMail;
    }

    public boolean isShowOngoing() {
        return showOngoing;
    }

    public int getAutomaticCheckIntervalMinutes() {
        return automaticCheckIntervalMinutes;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public int getIdleRefreshMinutes() {
        return idleRefreshMinutes;
    }

    public boolean shouldHideHostname() {
        // TODO
        return false;
    }

    public FolderMode getFolderPushMode() {
        return folderPushMode;
    }

    public String getName() {
        return name;
    }

    public DeletePolicy getDeletePolicy() {
        return deletePolicy;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeletePolicy(DeletePolicy deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStoreUri(String storeUri) {
        this.storeUri = storeUri;
    }

    public void setTransportUri(String transportUri) {
        this.transportUri = transportUri;
    }

    public void init(String email, String password) {
        this.email = email;

        String[] emailParts = EmailHelper.splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        ServerSettings storeServer = new ServerSettings(ServerSettings.Type.IMAP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, user, password, null);
        ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, user, password, null);
        String storeUri = RemoteStore.createStoreUri(storeServer);
        String transportUri = TransportUris.createTransportUri(transportServer);

        this.storeUri = storeUri;
        this.transportUri = transportUri;

        setupFolderNames(domain);
    }

    private void setupFolderNames(String domain) {
        setDraftsFolderName(K9.getK9String(R.string.special_mailbox_name_drafts));
        setTrashFolderName(K9.getK9String(R.string.special_mailbox_name_trash));
        setSentFolderName(K9.getK9String(R.string.special_mailbox_name_sent));
        setArchiveFolderName(K9.getK9String(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            setSpamFolderName("Bulk Mail");
        } else {
            setSpamFolderName(K9.getK9String(R.string.special_mailbox_name_spam));
        }
    }

    public String getEmail() {
        return email;
    }

    public void addCertificate(CheckDirection direction, X509Certificate certificate) throws CertificateException {
        Uri uri;
        if (direction == CheckDirection.INCOMING) {
            uri = Uri.parse(getStoreUri());
        } else {
            uri = Uri.parse(getTransportUri());
        }
        LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
        localKeyStore.addCertificate(uri.getHost(), uri.getPort(), certificate);
    }

    public void setSubscribedFoldersOnly(boolean subscribedFoldersOnly) {
        this.subscribedFoldersOnly = subscribedFoldersOnly;
    }

    public void deleteCertificate(String newHost, int newPort, CheckDirection direction) {
        Uri uri;
        if (direction == CheckDirection.INCOMING) {
            uri = Uri.parse(getStoreUri());
        } else {
            uri = Uri.parse(getTransportUri());
        }
        String oldHost = uri.getHost();
        int oldPort = uri.getPort();
        if (oldPort == -1) {
            // This occurs when a new account is created
            return;
        }
        if (!newHost.equals(oldHost) || newPort != oldPort) {
            LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
            localKeyStore.deleteCertificate(oldHost, oldPort);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeString(this.email);
        dest.writeString(this.storeUri);
        dest.writeString(this.transportUri);
        dest.writeString(this.inboxFolderName);
        dest.writeString(this.outboxFolderName);
        dest.writeString(this.draftsFolderName);
        dest.writeString(this.sentFolderName);
        dest.writeString(this.trashFolderName);
        dest.writeString(this.archiveFolderName);
        dest.writeString(this.spamFolderName);
        dest.writeString(this.autoExpandFolderName);
        dest.writeInt(this.maximumAutoDownloadMessageSize);
        dest.writeByte(this.subscribedFoldersOnly ? (byte) 1 : (byte) 0);
        dest.writeInt(this.compressionMap.size());
        for (Entry<NetworkType, Boolean> entry : this.compressionMap.entrySet()) {
            dest.writeInt(entry.getKey() == null ? -1 : entry.getKey().ordinal());
            dest.writeValue(entry.getValue());
        }
        dest.writeByte(this.allowRemoteSearch ? (byte) 1 : (byte) 0);
        dest.writeByte(this.remoteSearchFullText ? (byte) 1 : (byte) 0);
        dest.writeByte(this.pushPollOnConnect ? (byte) 1 : (byte) 0);
        dest.writeInt(this.displayCount);
        dest.writeInt(this.idleRefreshMinutes);
        dest.writeInt(this.incomingSecurityType == null ? -1 : this.incomingSecurityType.ordinal());
        dest.writeInt(this.incomingAuthType == null ? -1 : this.incomingAuthType.ordinal());
        dest.writeString(this.incomingPort);
        dest.writeInt(this.outgoingSecurityType == null ? -1 : this.outgoingSecurityType.ordinal());
        dest.writeInt(this.outgoingAuthType == null ? -1 : this.outgoingAuthType.ordinal());
        dest.writeString(this.outgoingPort);
        dest.writeByte(this.notifyNewMail ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showOngoing ? (byte) 1 : (byte) 0);
        dest.writeInt(this.automaticCheckIntervalMinutes);
        dest.writeInt(this.folderPushMode == null ? -1 : this.folderPushMode.ordinal());
        dest.writeInt(this.deletePolicy == null ? -1 : this.deletePolicy.ordinal());
    }

    protected ManualSetupInfo(Parcel in) {
        this.name = in.readString();
        this.description = in.readString();
        this.email = in.readString();
        this.storeUri = in.readString();
        this.transportUri = in.readString();
        this.inboxFolderName = in.readString();
        this.outboxFolderName = in.readString();
        this.draftsFolderName = in.readString();
        this.sentFolderName = in.readString();
        this.trashFolderName = in.readString();
        this.archiveFolderName = in.readString();
        this.spamFolderName = in.readString();
        this.autoExpandFolderName = in.readString();
        this.maximumAutoDownloadMessageSize = in.readInt();
        this.subscribedFoldersOnly = in.readByte() != 0;
        int compressionMapSize = in.readInt();
        this.compressionMap = new HashMap<NetworkType, Boolean>(compressionMapSize);
        for (int i = 0; i < compressionMapSize; i++) {
            int tmpKey = in.readInt();
            NetworkType key = tmpKey == -1 ? null : NetworkType.values()[tmpKey];
            Boolean value = (Boolean) in.readValue(Boolean.class.getClassLoader());
            this.compressionMap.put(key, value);
        }
        this.allowRemoteSearch = in.readByte() != 0;
        this.remoteSearchFullText = in.readByte() != 0;
        this.pushPollOnConnect = in.readByte() != 0;
        this.displayCount = in.readInt();
        this.idleRefreshMinutes = in.readInt();
        int tmpIncomingSecurityType = in.readInt();
        this.incomingSecurityType =
                tmpIncomingSecurityType == -1 ? null : ConnectionSecurity.values()[tmpIncomingSecurityType];
        int tmpIncomingAuthType = in.readInt();
        this.incomingAuthType = tmpIncomingAuthType == -1 ? null : AuthType.values()[tmpIncomingAuthType];
        this.incomingPort = in.readString();
        int tmpOutgoingSecurityType = in.readInt();
        this.outgoingSecurityType =
                tmpOutgoingSecurityType == -1 ? null : ConnectionSecurity.values()[tmpOutgoingSecurityType];
        int tmpOutgoingAuthType = in.readInt();
        this.outgoingAuthType = tmpOutgoingAuthType == -1 ? null : AuthType.values()[tmpOutgoingAuthType];
        this.outgoingPort = in.readString();
        this.notifyNewMail = in.readByte() != 0;
        this.showOngoing = in.readByte() != 0;
        this.automaticCheckIntervalMinutes = in.readInt();
        int tmpFolderPushMode = in.readInt();
        this.folderPushMode = tmpFolderPushMode == -1 ? null : FolderMode.values()[tmpFolderPushMode];
        int tmpDeletePolicy = in.readInt();
        this.deletePolicy = tmpDeletePolicy == -1 ? null : DeletePolicy.values()[tmpDeletePolicy];
    }

    public static final Creator<ManualSetupInfo> CREATOR = new Creator<ManualSetupInfo>() {
        @Override
        public ManualSetupInfo createFromParcel(Parcel source) {
            return new ManualSetupInfo(source);
        }

        @Override
        public ManualSetupInfo[] newArray(int size) {
            return new ManualSetupInfo[size];
        }
    };
}
