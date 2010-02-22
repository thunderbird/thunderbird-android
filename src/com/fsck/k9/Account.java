
package com.fsck.k9;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID.
 */
public class Account
{
    public static final String EXPUNGE_IMMEDIATELY = "EXPUNGE_IMMEDIATELY";
    public static final String EXPUNGE_MANUALLY = "EXPUNGE_MANUALLY";
    public static final String EXPUNGE_ON_POLL = "EXPUNGE_ON_POLL";

    public static final int DELETE_POLICY_NEVER = 0;
    public static final int DELETE_POLICY_7DAYS = 1;
    public static final int DELETE_POLICY_ON_DELETE = 2;
    public static final int DELETE_POLICY_MARK_AS_READ = 3;

    /**
     * <pre>
     * 0 - Never (DELETE_POLICY_NEVER)
     * 1 - After 7 days (DELETE_POLICY_7DAYS)
     * 2 - When I delete from inbox (DELETE_POLICY_ON_DELETE)
     * 3 - Mark as read (DELETE_POLICY_MARK_AS_READ)
     * </pre>
     */
    private int mDeletePolicy;

    private final String mUuid;
    private String mStoreUri;
    private String mLocalStoreUri;
    private String mTransportUri;
    private String mDescription;
    private String mAlwaysBcc;
    private int mAutomaticCheckIntervalMinutes;
    private int mDisplayCount;
    private long mLastAutomaticCheckTime;
    private boolean mNotifyNewMail;
    private boolean mNotifySelfNewMail;
    private String mDraftsFolderName;
    private String mSentFolderName;
    private String mTrashFolderName;
    private String mOutboxFolderName;
    private String mAutoExpandFolderName;
    private FolderMode mFolderDisplayMode;
    private FolderMode mFolderSyncMode;
    private FolderMode mFolderPushMode;
    private FolderMode mFolderTargetMode;
    private int mAccountNumber;
    private boolean mVibrate;
    private boolean mRing;
    private String mRingtoneUri;
    private boolean mNotifySync;
    private HideButtons mHideMessageViewButtons;
    private boolean mIsSignatureBeforeQuotedText;
    private String mExpungePolicy = EXPUNGE_IMMEDIATELY;
    private int mMaxPushFolders;
    private boolean mStoreAttachmentsOnSdCard;

    private List<Identity> identities;

    public enum FolderMode
    {
        NONE, ALL, FIRST_CLASS, FIRST_AND_SECOND_CLASS, NOT_SECOND_CLASS;
    }

    public enum HideButtons
    {
        NEVER, ALWAYS, KEYBOARD_AVAILABLE;
    }


    protected Account(Context context)
    {
        // TODO Change local store path to something readable / recognizable
        mUuid = UUID.randomUUID().toString();
        mLocalStoreUri = "local://localhost/" + context.getDatabasePath(mUuid + ".db");
        mAutomaticCheckIntervalMinutes = -1;
        mDisplayCount = -1;
        mAccountNumber = -1;
        mNotifyNewMail = true;
        mNotifySync = true;
        mVibrate = false;
        mRing = true;
        mNotifySelfNewMail = true;
        mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        mFolderSyncMode = FolderMode.FIRST_CLASS;
        mFolderPushMode = FolderMode.FIRST_CLASS;
        mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        mHideMessageViewButtons = HideButtons.NEVER;
        mRingtoneUri = "content://settings/system/notification_sound";
        mIsSignatureBeforeQuotedText = false;
        mExpungePolicy = EXPUNGE_IMMEDIATELY;
        mAutoExpandFolderName = "INBOX";
        mMaxPushFolders = 10;

        identities = new ArrayList<Identity>();

        Identity identity = new Identity();
        identity.setSignatureUse(true);
        identity.setSignature(context.getString(R.string.default_signature));
        identity.setDescription(context.getString(R.string.default_identity_description));
        identities.add(identity);
    }

    protected Account(Preferences preferences, String uuid)
    {
        this.mUuid = uuid;
        loadAccount(preferences);
    }

    /**
     * Load stored settings for this account.
     */
    private synchronized void loadAccount(Preferences preferences)
    {
        SharedPreferences prefs = preferences.getPreferences();
        
        mStoreUri = Utility.base64Decode(prefs.getString(mUuid + ".storeUri", null));
        mLocalStoreUri = prefs.getString(mUuid + ".localStoreUri", null);
        mTransportUri = Utility.base64Decode(prefs.getString(mUuid + ".transportUri", null));
        mDescription = prefs.getString(mUuid + ".description", null);
        mAlwaysBcc = prefs.getString(mUuid + ".alwaysBcc", mAlwaysBcc);
        mAutomaticCheckIntervalMinutes = prefs.getInt(mUuid + ".automaticCheckIntervalMinutes", -1);
        mDisplayCount = prefs.getInt(mUuid + ".displayCount", -1);
        mLastAutomaticCheckTime = prefs.getLong(mUuid + ".lastAutomaticCheckTime", 0);
        mNotifyNewMail = prefs.getBoolean(mUuid + ".notifyNewMail", false);
        mNotifySelfNewMail = prefs.getBoolean(mUuid + ".notifySelfNewMail", true);
        mNotifySync = prefs.getBoolean(mUuid + ".notifyMailCheck", false);
        mDeletePolicy = prefs.getInt(mUuid + ".deletePolicy", 0);
        mDraftsFolderName = prefs.getString(mUuid + ".draftsFolderName", "Drafts");
        mSentFolderName = prefs.getString(mUuid + ".sentFolderName", "Sent");
        mTrashFolderName = prefs.getString(mUuid  + ".trashFolderName", "Trash");
        mOutboxFolderName = prefs.getString(mUuid  + ".outboxFolderName", "Outbox");
        mExpungePolicy = prefs.getString(mUuid  + ".expungePolicy", EXPUNGE_IMMEDIATELY);
        mMaxPushFolders = prefs.getInt(mUuid + ".maxPushFolders", 10);

        // Between r418 and r431 (version 0.103), folder names were set empty if the Incoming settings were
        // opened for non-IMAP accounts.  0.103 was never a market release, so perhaps this code
        // should be deleted sometime soon
        if (mDraftsFolderName == null || mDraftsFolderName.equals(""))
        {
            mDraftsFolderName = "Drafts";
        }
        if (mSentFolderName == null || mSentFolderName.equals(""))
        {
            mSentFolderName = "Sent";
        }
        if (mTrashFolderName == null || mTrashFolderName.equals(""))
        {
            mTrashFolderName = "Trash";
        }
        if (mOutboxFolderName == null || mOutboxFolderName.equals(""))
        {
            mOutboxFolderName = "Outbox";
        }
        // End of 0.103 repair

        mAutoExpandFolderName = prefs.getString(mUuid  + ".autoExpandFolderName", "INBOX");

        mAccountNumber = prefs.getInt(mUuid + ".accountNumber", 0);
        mVibrate = prefs.getBoolean(mUuid + ".vibrate", false);
        mRing = prefs.getBoolean(mUuid + ".ring", true);

        try
        {
            mHideMessageViewButtons = HideButtons.valueOf(prefs.getString(mUuid + ".hideButtonsEnum",
                                      HideButtons.NEVER.name()));
        }
        catch (Exception e)
        {
            mHideMessageViewButtons = HideButtons.NEVER;
        }

        mRingtoneUri = prefs.getString(mUuid  + ".ringtone",
                       "content://settings/system/notification_sound");
        try
        {
            mFolderDisplayMode = FolderMode.valueOf(prefs.getString(mUuid + ".folderDisplayMode",
                                                    FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        }

        try
        {
            mFolderSyncMode = FolderMode.valueOf(prefs.getString(mUuid + ".folderSyncMode",
                                                 FolderMode.FIRST_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderSyncMode = FolderMode.FIRST_CLASS;
        }

        try
        {
            mFolderPushMode = FolderMode.valueOf(prefs.getString(mUuid + ".folderPushMode",
                                                 FolderMode.FIRST_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderPushMode = FolderMode.FIRST_CLASS;
        }

        try
        {
            mFolderTargetMode = FolderMode.valueOf(prefs.getString(mUuid + ".folderTargetMode",
                                                   FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        }

        mIsSignatureBeforeQuotedText = prefs.getBoolean(mUuid  + ".signatureBeforeQuotedText", false);
        mStoreAttachmentsOnSdCard = prefs.getBoolean(mUuid  + ".storeAttachmentOnSdCard", false);
        identities = loadIdentities(preferences);
    }


    protected synchronized void delete(Preferences preferences)
    {
        String[] uuids = preferences.getPreferences().getString("accountUuids", "").split(",");
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = uuids.length; i < length; i++)
        {
            if (!uuids[i].equals(mUuid))
            {
                if (sb.length() > 0)
                {
                    sb.append(',');
                }
                sb.append(uuids[i]);
            }
        }
        String accountUuids = sb.toString();
        SharedPreferences.Editor editor = preferences.getPreferences().edit();
        editor.putString("accountUuids", accountUuids);

        editor.remove(mUuid + ".storeUri");
        editor.remove(mUuid + ".localStoreUri");
        editor.remove(mUuid + ".transportUri");
        editor.remove(mUuid + ".description");
        editor.remove(mUuid + ".alwaysBcc");
        editor.remove(mUuid + ".automaticCheckIntervalMinutes");
        editor.remove(mUuid + ".lastAutomaticCheckTime");
        editor.remove(mUuid + ".notifyNewMail");
        editor.remove(mUuid + ".notifySelfNewMail");
        editor.remove(mUuid + ".deletePolicy");
        editor.remove(mUuid + ".draftsFolderName");
        editor.remove(mUuid + ".sentFolderName");
        editor.remove(mUuid + ".trashFolderName");
        editor.remove(mUuid + ".outboxFolderName");
        editor.remove(mUuid + ".autoExpandFolderName");
        editor.remove(mUuid + ".accountNumber");
        editor.remove(mUuid + ".vibrate");
        editor.remove(mUuid + ".ring");
        editor.remove(mUuid + ".ringtone");
        editor.remove(mUuid + ".lastFullSync");
        editor.remove(mUuid + ".folderDisplayMode");
        editor.remove(mUuid + ".folderSyncMode");
        editor.remove(mUuid + ".folderPushMode");
        editor.remove(mUuid + ".folderTargetMode");
        editor.remove(mUuid + ".hideButtonsEnum");
        editor.remove(mUuid + ".signatureBeforeQuotedText");
        editor.remove(mUuid + ".expungePolicy");
        editor.remove(mUuid + ".maxPushFolders");
        editor.remove(mUuid + ".storeAttachmentOnSdCard");
        editor.commit();

        for (Identity identity : identities)
        {
            identity.delete(preferences);
        }
    }

    public synchronized void save(Preferences preferences)
    {
        SharedPreferences.Editor editor = preferences.getPreferences().edit();

        if (!preferences.getPreferences().getString("accountUuids", "").contains(mUuid))
        {
            /*
             * When the account is first created we assign it a unique account number. The
             * account number will be unique to that account for the lifetime of the account.
             * So, we get all the existing account numbers, sort them ascending, loop through
             * the list and check if the number is greater than 1 + the previous number. If so
             * we use the previous number + 1 as the account number. This refills gaps.
             * mAccountNumber starts as -1 on a newly created account. It must be -1 for this
             * algorithm to work.
             *
             * I bet there is a much smarter way to do this. Anyone like to suggest it?
             */
            Account[] accounts = preferences.getAccounts();
            int[] accountNumbers = new int[accounts.length];
            for (int i = 0; i < accounts.length; i++)
            {
                accountNumbers[i] = accounts[i].getAccountNumber();
            }
            Arrays.sort(accountNumbers);
            for (int accountNumber : accountNumbers)
            {
                if (accountNumber > mAccountNumber + 1)
                {
                    break;
                }
                mAccountNumber = accountNumber;
            }
            mAccountNumber++;

            String accountUuids = preferences.getPreferences().getString("accountUuids", "");
            accountUuids += (accountUuids.length() != 0 ? "," : "") + mUuid;
            editor.putString("accountUuids", accountUuids);
        }

        editor.putString(mUuid + ".storeUri", Utility.base64Encode(mStoreUri));
        editor.putString(mUuid + ".localStoreUri", mLocalStoreUri);
        editor.putString(mUuid + ".transportUri", Utility.base64Encode(mTransportUri));
        editor.putString(mUuid + ".description", mDescription);
        editor.putString(mUuid + ".alwaysBcc", mAlwaysBcc);
        editor.putInt(mUuid + ".automaticCheckIntervalMinutes", mAutomaticCheckIntervalMinutes);
        editor.putInt(mUuid + ".displayCount", mDisplayCount);
        editor.putLong(mUuid + ".lastAutomaticCheckTime", mLastAutomaticCheckTime);
        editor.putBoolean(mUuid + ".notifyNewMail", mNotifyNewMail);
        editor.putBoolean(mUuid + ".notifySelfNewMail", mNotifySelfNewMail);
        editor.putBoolean(mUuid + ".notifyMailCheck", mNotifySync);
        editor.putInt(mUuid + ".deletePolicy", mDeletePolicy);
        editor.putString(mUuid + ".draftsFolderName", mDraftsFolderName);
        editor.putString(mUuid + ".sentFolderName", mSentFolderName);
        editor.putString(mUuid + ".trashFolderName", mTrashFolderName);
        editor.putString(mUuid + ".outboxFolderName", mOutboxFolderName);
        editor.putString(mUuid + ".autoExpandFolderName", mAutoExpandFolderName);
        editor.putInt(mUuid + ".accountNumber", mAccountNumber);
        editor.putBoolean(mUuid + ".vibrate", mVibrate);
        editor.putBoolean(mUuid + ".ring", mRing);
        editor.putString(mUuid + ".hideButtonsEnum", mHideMessageViewButtons.name());
        editor.putString(mUuid + ".ringtone", mRingtoneUri);
        editor.putString(mUuid + ".folderDisplayMode", mFolderDisplayMode.name());
        editor.putString(mUuid + ".folderSyncMode", mFolderSyncMode.name());
        editor.putString(mUuid + ".folderPushMode", mFolderPushMode.name());
        editor.putString(mUuid + ".folderTargetMode", mFolderTargetMode.name());
        editor.putBoolean(mUuid + ".signatureBeforeQuotedText", this.mIsSignatureBeforeQuotedText);
        editor.putString(mUuid + ".expungePolicy", mExpungePolicy);
        editor.putInt(mUuid + ".maxPushFolders", mMaxPushFolders);
        editor.putBoolean(mUuid + ".storeAttachmentOnSdCard", mStoreAttachmentsOnSdCard);
        editor.commit();
        
        for (Identity identity : identities)
        {
            identity.save(preferences);
        }

        try
        {
            getLocalStore().setStoreAttachmentsOnSdCard(mStoreAttachmentsOnSdCard);
        }
        catch (MessagingException e)
        {
            //Should not happen
            Log.w(K9.LOG_TAG, null, e);
        }
    }

    //TODO: Shouldn't this live in MessagingController?
    public int getUnreadMessageCount(Context context) throws MessagingException
    {
        int unreadMessageCount = 0;
        LocalStore localStore = getLocalStore();
        Account.FolderMode aMode = getFolderDisplayMode();
        Preferences prefs = Preferences.getPreferences(context);
        for (LocalFolder folder : localStore.getPersonalNamespaces())
        {
            folder.refresh(prefs);
            Folder.FolderClass fMode = folder.getDisplayClass();

            if (folder.getName().equals(getTrashFolderName()) == false &&
                    folder.getName().equals(getDraftsFolderName()) == false &&
                    folder.getName().equals(getOutboxFolderName()) == false &&
                    folder.getName().equals(getSentFolderName()) == false &&
                    folder.getName().equals(getErrorFolderName()) == false)
            {
                if (aMode == Account.FolderMode.NONE)
                {
                    continue;
                }
                if (aMode == Account.FolderMode.FIRST_CLASS &&
                        fMode != Folder.FolderClass.FIRST_CLASS)
                {
                    continue;
                }
                if (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                        fMode != Folder.FolderClass.FIRST_CLASS &&
                        fMode != Folder.FolderClass.SECOND_CLASS)
                {
                    continue;
                }
                if (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                        fMode == Folder.FolderClass.SECOND_CLASS)
                {
                    continue;
                }
                unreadMessageCount += folder.getUnreadMessageCount();
            }
        }

        return unreadMessageCount;
    }

    public String getUuid()
    {
        return mUuid;
    }

    public Uri getContentUri()
    {
        return Uri.parse("content://accounts/" + getUuid());
    }

    public synchronized String getStoreUri()
    {
        return mStoreUri;
    }

    public synchronized void setStoreUri(String storeUri)
    {
        this.mStoreUri = storeUri;
    }

    public synchronized String getTransportUri()
    {
        return mTransportUri;
    }

    public synchronized void setTransportUri(String transportUri)
    {
        this.mTransportUri = transportUri;
    }

    public synchronized String getDescription()
    {
        return mDescription;
    }

    public synchronized void setDescription(String description)
    {
        this.mDescription = description;
    }

    public synchronized String getName()
    {
        return identities.get(0).getName();
    }

    public synchronized void setName(String name)
    {
        identities.get(0).setName(name);
    }

    public synchronized boolean getSignatureUse()
    {
        return identities.get(0).getSignatureUse();
    }

    public synchronized void setSignatureUse(boolean signatureUse)
    {
        identities.get(0).setSignatureUse(signatureUse);
    }

    public synchronized String getSignature()
    {
        return identities.get(0).getSignature();
    }

    public synchronized void setSignature(String signature)
    {
        identities.get(0).setSignature(signature);
    }

    public synchronized String getEmail()
    {
        return identities.get(0).getEmail();
    }

    public synchronized void setEmail(String email)
    {
        identities.get(0).setEmail(email);
    }

    public synchronized String getAlwaysBcc()
    {
        return mAlwaysBcc;
    }

    public synchronized void setAlwaysBcc(String alwaysBcc)
    {
        this.mAlwaysBcc = alwaysBcc;
    }

    public synchronized boolean isVibrate()
    {
        return mVibrate;
    }

    public synchronized void setVibrate(boolean vibrate)
    {
        mVibrate = vibrate;
    }

    public synchronized String getRingtone()
    {
        return mRingtoneUri;
    }

    public synchronized void setRingtone(String ringtoneUri)
    {
        mRingtoneUri = ringtoneUri;
    }

    public synchronized String getLocalStoreUri()
    {
        return mLocalStoreUri;
    }

    public synchronized void setLocalStoreUri(String localStoreUri)
    {
        this.mLocalStoreUri = localStoreUri;
    }

    /**
     * Returns -1 for never.
     */
    public synchronized int getAutomaticCheckIntervalMinutes()
    {
        return mAutomaticCheckIntervalMinutes;
    }

    /**
     * @param automaticCheckIntervalMinutes or -1 for never.
     */
    public synchronized boolean setAutomaticCheckIntervalMinutes(int automaticCheckIntervalMinutes)
    {
        int oldInterval = this.mAutomaticCheckIntervalMinutes;
        int newInterval = automaticCheckIntervalMinutes;
        this.mAutomaticCheckIntervalMinutes = automaticCheckIntervalMinutes;
        
        return (oldInterval != newInterval);
    }

    public synchronized int getDisplayCount()
    {
        if (mDisplayCount == -1)
        {
            this.mDisplayCount = K9.DEFAULT_VISIBLE_LIMIT;
        }
        return mDisplayCount;
    }

    public synchronized void setDisplayCount(int displayCount)
    {
        if (displayCount != -1)
        {
            this.mDisplayCount = displayCount;
        }
        else
        {
            this.mDisplayCount = K9.DEFAULT_VISIBLE_LIMIT;
        }
    }

    public synchronized long getLastAutomaticCheckTime()
    {
        return mLastAutomaticCheckTime;
    }

    public synchronized void setLastAutomaticCheckTime(long lastAutomaticCheckTime)
    {
        this.mLastAutomaticCheckTime = lastAutomaticCheckTime;
    }

    public synchronized boolean isNotifyNewMail()
    {
        return mNotifyNewMail;
    }

    public synchronized void setNotifyNewMail(boolean notifyNewMail)
    {
        this.mNotifyNewMail = notifyNewMail;
    }

    public synchronized int getDeletePolicy()
    {
        return mDeletePolicy;
    }

    public synchronized void setDeletePolicy(int deletePolicy)
    {
        this.mDeletePolicy = deletePolicy;
    }

    public synchronized String getDraftsFolderName()
    {
        return mDraftsFolderName;
    }

    public synchronized void setDraftsFolderName(String draftsFolderName)
    {
        mDraftsFolderName = draftsFolderName;
    }

    public synchronized String getSentFolderName()
    {
        return mSentFolderName;
    }

    public synchronized String getErrorFolderName()
    {
        return K9.ERROR_FOLDER_NAME;
    }

    public synchronized void setSentFolderName(String sentFolderName)
    {
        mSentFolderName = sentFolderName;
    }

    public synchronized String getTrashFolderName()
    {
        return mTrashFolderName;
    }

    public synchronized void setTrashFolderName(String trashFolderName)
    {
        mTrashFolderName = trashFolderName;
    }

    public synchronized String getOutboxFolderName()
    {
        return mOutboxFolderName;
    }

    public synchronized void setOutboxFolderName(String outboxFolderName)
    {
        mOutboxFolderName = outboxFolderName;
    }

    public synchronized String getAutoExpandFolderName()
    {
        return mAutoExpandFolderName;
    }

    public synchronized void setAutoExpandFolderName(String autoExpandFolderName)
    {
        mAutoExpandFolderName = autoExpandFolderName;
    }

    public synchronized int getAccountNumber()
    {
        return mAccountNumber;
    }

    public synchronized FolderMode getFolderDisplayMode()
    {
        return mFolderDisplayMode;
    }

    public synchronized boolean setFolderDisplayMode(FolderMode displayMode)
    {
        FolderMode oldDisplayMode = mFolderDisplayMode;
        mFolderDisplayMode = displayMode;
        return oldDisplayMode != displayMode;
    }

    public synchronized FolderMode getFolderSyncMode()
    {
        return mFolderSyncMode;
    }

    public synchronized boolean setFolderSyncMode(FolderMode syncMode)
    {
        FolderMode oldSyncMode = mFolderSyncMode;
        mFolderSyncMode = syncMode;
        
        if (syncMode == FolderMode.NONE && oldSyncMode != FolderMode.NONE)
        {
            return true;
        }
        if (syncMode != FolderMode.NONE && oldSyncMode == FolderMode.NONE)
        {
            return true;
        }
        return false;
    }

    public synchronized FolderMode getFolderPushMode()
    {
        return mFolderPushMode;
    }

    public synchronized boolean setFolderPushMode(FolderMode pushMode)
    {
        FolderMode oldPushMode = mFolderPushMode;
        
        mFolderPushMode = pushMode;
        return pushMode != oldPushMode;
    }

    public synchronized boolean isShowOngoing()
    {
        return mNotifySync;
    }

    public synchronized void setShowOngoing(boolean showOngoing)
    {
        this.mNotifySync = showOngoing;
    }

    public synchronized HideButtons getHideMessageViewButtons()
    {
        return mHideMessageViewButtons;
    }

    public synchronized void setHideMessageViewButtons(HideButtons hideMessageViewButtons)
    {
        mHideMessageViewButtons = hideMessageViewButtons;
    }

    public synchronized FolderMode getFolderTargetMode()
    {
        return mFolderTargetMode;
    }

    public synchronized void setFolderTargetMode(FolderMode folderTargetMode)
    {
        mFolderTargetMode = folderTargetMode;
    }

    public synchronized boolean isSignatureBeforeQuotedText()
    {
        return mIsSignatureBeforeQuotedText;
    }

    public synchronized void setSignatureBeforeQuotedText(boolean mIsSignatureBeforeQuotedText)
    {
        this.mIsSignatureBeforeQuotedText = mIsSignatureBeforeQuotedText;
    }

    public synchronized boolean isNotifySelfNewMail()
    {
        return mNotifySelfNewMail;
    }

    public synchronized void setNotifySelfNewMail(boolean notifySelfNewMail)
    {
        mNotifySelfNewMail = notifySelfNewMail;
    }

    public synchronized String getExpungePolicy()
    {
        return mExpungePolicy;
    }

    public synchronized void setExpungePolicy(String expungePolicy)
    {
        mExpungePolicy = expungePolicy;
    }

    public synchronized int getMaxPushFolders()
    {
        return mMaxPushFolders;
    }

    public synchronized boolean setMaxPushFolders(int maxPushFolders)
    {
        int oldMaxPushFolders = mMaxPushFolders;
        mMaxPushFolders = maxPushFolders;
        return oldMaxPushFolders != maxPushFolders;
    }

    public synchronized boolean isRing()
    {
        return mRing;
    }

    public synchronized void setRing(boolean ring)
    {
        mRing = ring;
    }

    public synchronized boolean isStoreAttachmentOnSdCard()
    {
        return mStoreAttachmentsOnSdCard;
    }

    public synchronized void setStoreAttachmentOnSdCard(boolean mStoreAttachmentOnSdCard)
    {
        this.mStoreAttachmentsOnSdCard = mStoreAttachmentOnSdCard;
    }


    public LocalStore getLocalStore() throws MessagingException
    {
        return Store.getLocalInstance(this, K9.app);        
    }
    
    public Store getRemoteStore() throws MessagingException
    {
        return Store.getRemoteInstance(this);        
    }

    @Override
    public synchronized String toString()
    {
        return mDescription;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Account)
        {
            return ((Account)o).mUuid.equals(mUuid);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return mUuid.hashCode();
    }


    private synchronized List<Identity> loadIdentities(Preferences preferences)
    {
        SharedPreferences prefs = preferences.getPreferences();
        List<Identity> newIdentities = new ArrayList<Identity>();

        String identityUuids = prefs.getString(mUuid + ".identityUuids", null);
        if ((identityUuids != null) && (identityUuids.length() != 0))
        {
            String[] uuids = identityUuids.split(",");
            for (int i = 0, length = uuids.length; i < length; i++)
            {
                Identity identity = new Identity(uuids[i], preferences);
                if (identity.getEmail() != null)
                {
                    newIdentities.add(identity);
                }
            }
        }

        return newIdentities;
    }

    public synchronized Identity[] getIdentities()
    {
        return identities.toArray(new Identity[0]);
    }

    public synchronized Identity newIdentity()
    {
        Identity identity = new Identity();
        identities.add(identity);

        return identity;
    }

    public synchronized void deleteIdentity(Identity identity, Preferences preferences)
    {
        if (identities.size() > 1)
        {
            identities.remove(identity);
            identity.delete(preferences);
        }
    }

    public synchronized Identity getIdentity(String uuid)
    {
        for (Identity identity: identities)
        {
            if (identity.getUuid().equals(uuid))
            {
                return identity;
            }
        }

        return null;
    }

    public synchronized Identity getIdentity(int i)
    {
        if (i < identities.size())
        {
            return identities.get(i);
        }
        return null;
    }

    public boolean isAnIdentity(Address[] addrs)
    {
        if (addrs == null)
        {
            return false;
        }
        for (Address addr : addrs)
        {
            if (findIdentity(addr) != null)
            {
                return true;
            }
        }

        return false;
    }

    public boolean isAnIdentity(Address addr)
    {
        return findIdentity(addr) != null;
    }

    public synchronized Identity findIdentity(Address addr)
    {
        for (Identity identity : identities)
        {
            String email = identity.getEmail();
            if (email != null && email.equalsIgnoreCase(addr.getAddress()))
            {
                return identity;
            }
        }
        return null;
    }

    public void identityMoveUp(Identity identity, Preferences preferences)
    {
        for (int i = 1, length = identities.size(); i < length; i++)
        {
            if (identity == identities.get(i))
            {
                identities.remove(i);
                identities.add(i-1, identity);
                saveIdentityOrder(preferences);
                break;
            }
        }
    }

    public void identityMoveDown(Identity identity, Preferences preferences)
    {
        for (int i = 0, length = identities.size() - 1; i < length; i++)
        {
            if (identity == identities.get(i))
            {
                identities.remove(i);
                identities.add(i+1, identity);
                saveIdentityOrder(preferences);
                break;
            }
        }
    }

    public void identityMoveToTop(Identity identity, Preferences preferences)
    {
        for (int i = 1, length = identities.size(); i < length; i++)
        {
            if (identity == identities.get(i))
            {
                identities.remove(i);
                identities.add(0, identity);
                saveIdentityOrder(preferences);
                break;
            }
        }
    }

    private void saveIdentityOrder(Preferences preferences)
    {
        SharedPreferences prefs = preferences.getPreferences();
        StringBuffer sb = new StringBuffer();
        for (Identity identity : identities)
        {
            if (sb.length() > 0)
            {
                sb.append(',');
            }
            sb.append(identity.getUuid());
        }
        String identityUuids = sb.toString();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(mUuid + ".identityUuids", identityUuids);
        editor.commit();
    }

    public class Identity
    {
        private final String mUuid;
        private String mDescription;
        private String mName;
        private String mEmail;
        private String mSignature;
        private boolean mSignatureUse;

        private Identity()
        {
            mUuid = UUID.randomUUID().toString();
        }

        private Identity(String uuid, Preferences preferences)
        {
            mUuid = uuid;
            load(preferences);
        }

        public String getUuid()
        {
            return mUuid;
        }
        
        public synchronized String getName()
        {
            return mName;
        }

        public synchronized void setName(String name)
        {
            mName = name;
        }

        public synchronized String getEmail()
        {
            return mEmail;
        }

        public synchronized void setEmail(String email)
        {
            mEmail = email;
        }

        public synchronized boolean getSignatureUse()
        {
            return mSignatureUse;
        }

        public synchronized void setSignatureUse(boolean signatureUse)
        {
            mSignatureUse = signatureUse;
        }

        public synchronized String getSignature()
        {
            return mSignature;
        }

        public synchronized void setSignature(String signature)
        {
            mSignature = signature;
        }

        public synchronized String getDescription()
        {
            return mDescription;
        }

        public synchronized void setDescription(String description)
        {
            mDescription = description;
        }

        @Override
        public synchronized String toString()
        {
            return "Account.Identity(description=" + mDescription +
                ", name=" + mName + ", email=" + mEmail + ", signature=" + mSignature;
        }

        private synchronized void load(Preferences preferences)
        {
            SharedPreferences prefs = preferences.getPreferences();
            
            mName = prefs.getString(Account.this.mUuid + "." + mUuid + ".name", null);
            mEmail = prefs.getString(Account.this.mUuid + "." + mUuid + ".email", null);
            mSignatureUse = prefs.getBoolean(Account.this.mUuid + "." + mUuid  + ".signatureUse", true);
            mSignature = prefs.getString(Account.this.mUuid + "." + mUuid + ".signature", null);
            mDescription = prefs.getString(Account.this.mUuid + "." + mUuid + ".description", null);
        }

        public synchronized void save(Preferences preferences)
        {
            if (mEmail == null)
            {
                delete(preferences);
            }

            SharedPreferences prefs = preferences.getPreferences();
            SharedPreferences.Editor editor = prefs.edit();

            String identityUuids = prefs.getString(Account.this.mUuid + ".identityUuids", "");
            if (!identityUuids.contains(mUuid))
            {
                identityUuids += (identityUuids.length() != 0 ? "," : "") + mUuid;
                editor.putString(Account.this.mUuid + ".identityUuids", identityUuids);
            }

            editor.putString(Account.this.mUuid + "." + mUuid + ".name", mName);
            editor.putString(Account.this.mUuid + "." + mUuid + ".email", mEmail);
            editor.putBoolean(Account.this.mUuid + "." + mUuid + ".signatureUse", mSignatureUse);
            editor.putString(Account.this.mUuid + "." + mUuid + ".signature", mSignature);
            editor.putString(Account.this.mUuid + "." + mUuid + ".description", mDescription);
            editor.commit();
        }

        public synchronized void delete(Preferences preferences)
        {
            SharedPreferences prefs = preferences.getPreferences();
            String[] uuids = prefs.getString(Account.this.mUuid + ".identityUuids", "").split(",");
            StringBuffer sb = new StringBuffer();
            for (int i = 0, length = uuids.length; i < length; i++)
            {
                if (!uuids[i].equals(mUuid))
                {
                    if (sb.length() > 0)
                    {
                        sb.append(',');
                    }
                    sb.append(uuids[i]);
                }
            }
            String identityUuids = sb.toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Account.this.mUuid + ".identityUuids", identityUuids);

            editor.remove(Account.this.mUuid + "." + mUuid + ".name");
            editor.remove(Account.this.mUuid + "." + mUuid + ".email");
            editor.remove(Account.this.mUuid + "." + mUuid + ".signatureUse");
            editor.remove(Account.this.mUuid + "." + mUuid + ".signature");
            editor.remove(Account.this.mUuid + "." + mUuid + ".description");
            editor.commit();
        }
    }
}
