
package com.fsck.k9;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

import com.fsck.k9.crypto.Apg;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID.
 */
public class Account implements BaseAccount
{
    public static final String EXPUNGE_IMMEDIATELY = "EXPUNGE_IMMEDIATELY";
    public static final String EXPUNGE_MANUALLY = "EXPUNGE_MANUALLY";
    public static final String EXPUNGE_ON_POLL = "EXPUNGE_ON_POLL";

    public static final int DELETE_POLICY_NEVER = 0;
    public static final int DELETE_POLICY_7DAYS = 1;
    public static final int DELETE_POLICY_ON_DELETE = 2;
    public static final int DELETE_POLICY_MARK_AS_READ = 3;

    public static final String TYPE_WIFI = "WIFI";
    public static final String TYPE_MOBILE = "MOBILE";
    public static final String TYPE_OTHER = "OTHER";
    private static String[] networkTypes = { TYPE_WIFI, TYPE_MOBILE, TYPE_OTHER };

    private static final String DEFAULT_QUOTE_PREFIX = ">";

    /**
     * <pre>
     * 0 - Never (DELETE_POLICY_NEVER)
     * 1 - After 7 days (DELETE_POLICY_7DAYS)
     * 2 - When I delete from inbox (DELETE_POLICY_ON_DELETE)
     * 3 - Mark as read (DELETE_POLICY_MARK_AS_READ)
     * </pre>
     */
    private int mDeletePolicy;

    private String mUuid;
    private String mStoreUri;
    private String mLocalStoreUri;
    private String mTransportUri;
    private String mDescription;
    private String mAlwaysBcc;
    private int mAutomaticCheckIntervalMinutes;
    private int mDisplayCount;
    private int mChipColor;
    private int mLedColor;
    private long mLastAutomaticCheckTime;
    private boolean mNotifyNewMail;
    private boolean mNotifySelfNewMail;
    private String mDraftsFolderName;
    private String mSentFolderName;
    private String mTrashFolderName;
    private String mArchiveFolderName;
    private String mSpamFolderName;
    private String mOutboxFolderName;
    private String mAutoExpandFolderName;
    private FolderMode mFolderDisplayMode;
    private FolderMode mFolderSyncMode;
    private FolderMode mFolderPushMode;
    private FolderMode mFolderTargetMode;
    private int mAccountNumber;
    private boolean mVibrate;
    private int mVibratePattern;
    private int mVibrateTimes;
    private boolean mRing;
    private boolean mSaveAllHeaders;
    private boolean mPushPollOnConnect;
    private String mRingtoneUri;
    private boolean mNotifySync;
    private HideButtons mHideMessageViewButtons;
    private HideButtons mHideMessageViewMoveButtons;
    private ShowPictures mShowPictures;
    private boolean mEnableMoveButtons;
    private boolean mIsSignatureBeforeQuotedText;
    private String mExpungePolicy = EXPUNGE_IMMEDIATELY;
    private int mMaxPushFolders;
    private int mIdleRefreshMinutes;
    private boolean goToUnreadMessageSearch;
    private Map<String, Boolean> compressionMap = new ConcurrentHashMap<String, Boolean>();
    private Searchable searchableFolders;
    private boolean subscribedFoldersOnly;
    private int maximumPolledMessageAge;
    private int maximumAutoDownloadMessageSize;
    // Tracks if we have sent a notification for this account for
    // current set of fetched messages
    private boolean mRingNotified;
    private String mQuotePrefix;
    private boolean mSyncRemoteDeletions;
    private String mCryptoApp;
    private boolean mCryptoAutoSignature;

    /**
     * Name of the folder that was last selected for a copy or move operation.
     *
     * Note: For now this value isn't persisted. So it will be reset when
     *       K-9 Mail is restarted.
     */
    private String lastSelectedFolderName = null;

    private List<Identity> identities;

    public enum FolderMode
    {
        NONE, ALL, FIRST_CLASS, FIRST_AND_SECOND_CLASS, NOT_SECOND_CLASS;
    }

    public enum HideButtons
    {
        NEVER, ALWAYS, KEYBOARD_AVAILABLE;
    }

    public enum ShowPictures
    {
        NEVER, ALWAYS, ONLY_FROM_CONTACTS;
    }

    public enum Searchable
    {
        ALL, DISPLAYABLE, NONE
    }

    protected Account(Context context)
    {
        // TODO Change local store path to something readable / recognizable
        mUuid = UUID.randomUUID().toString();
        mLocalStoreUri = "local://localhost/" + context.getDatabasePath(mUuid + ".db");
        mAutomaticCheckIntervalMinutes = -1;
        mIdleRefreshMinutes = 24;
        mSaveAllHeaders = false;
        mPushPollOnConnect = true;
        mDisplayCount = K9.DEFAULT_VISIBLE_LIMIT;
        mAccountNumber = -1;
        mNotifyNewMail = true;
        mNotifySync = true;
        mVibrate = false;
        mVibratePattern = 0;
        mVibrateTimes = 5;
        mRing = true;
        mNotifySelfNewMail = true;
        mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        mFolderSyncMode = FolderMode.FIRST_CLASS;
        mFolderPushMode = FolderMode.FIRST_CLASS;
        mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        mHideMessageViewButtons = HideButtons.NEVER;
        mHideMessageViewMoveButtons = HideButtons.NEVER;
        mShowPictures = ShowPictures.NEVER;
        mEnableMoveButtons = false;
        mRingtoneUri = "content://settings/system/notification_sound";
        mIsSignatureBeforeQuotedText = false;
        mExpungePolicy = EXPUNGE_IMMEDIATELY;
        mAutoExpandFolderName = "INBOX";
        mMaxPushFolders = 10;
        mChipColor = (new Random()).nextInt(0xffffff) + 0xff000000;
        mLedColor = mChipColor;
        goToUnreadMessageSearch = false;
        subscribedFoldersOnly = false;
        maximumPolledMessageAge = -1;
        maximumAutoDownloadMessageSize = 32768;
        mQuotePrefix = DEFAULT_QUOTE_PREFIX;
        mSyncRemoteDeletions = true;
        mCryptoApp = Apg.NAME;
        mCryptoAutoSignature = false;

        searchableFolders = Searchable.ALL;

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

        mStoreUri = Utility.base64Decode(prefs.getString(mUuid
                                         + ".storeUri", null));
        mLocalStoreUri = prefs.getString(mUuid + ".localStoreUri", null);
        mTransportUri = Utility.base64Decode(prefs.getString(mUuid
                                             + ".transportUri", null));
        mDescription = prefs.getString(mUuid + ".description", null);
        mAlwaysBcc = prefs.getString(mUuid + ".alwaysBcc", mAlwaysBcc);
        mAutomaticCheckIntervalMinutes = prefs.getInt(mUuid
                                         + ".automaticCheckIntervalMinutes", -1);
        mIdleRefreshMinutes = prefs.getInt(mUuid
                                           + ".idleRefreshMinutes", 24);
        mSaveAllHeaders = prefs.getBoolean(mUuid
                                           + ".saveAllHeaders", false);
        mPushPollOnConnect = prefs.getBoolean(mUuid
                                              + ".pushPollOnConnect", true);
        mDisplayCount = prefs.getInt(mUuid + ".displayCount", K9.DEFAULT_VISIBLE_LIMIT);
        if (mDisplayCount < 0)
        {
            mDisplayCount = K9.DEFAULT_VISIBLE_LIMIT;
        }
        mLastAutomaticCheckTime = prefs.getLong(mUuid
                                                + ".lastAutomaticCheckTime", 0);
        mNotifyNewMail = prefs.getBoolean(mUuid + ".notifyNewMail",
                                          false);
        mNotifySelfNewMail = prefs.getBoolean(mUuid + ".notifySelfNewMail",
                                              true);
        mNotifySync = prefs.getBoolean(mUuid + ".notifyMailCheck",
                                       false);
        mDeletePolicy = prefs.getInt(mUuid + ".deletePolicy", 0);
        mDraftsFolderName = prefs.getString(mUuid  + ".draftsFolderName",
                                            "Drafts");
        mSentFolderName = prefs.getString(mUuid  + ".sentFolderName",
                                          "Sent");
        mTrashFolderName = prefs.getString(mUuid  + ".trashFolderName",
                                           "Trash");
        mArchiveFolderName = prefs.getString(mUuid  + ".archiveFolderName",
                                             "Archive");
        mSpamFolderName = prefs.getString(mUuid  + ".spamFolderName",
                                          "Spam");
        mOutboxFolderName = prefs.getString(mUuid  + ".outboxFolderName",
                                            "Outbox");
        mExpungePolicy = prefs.getString(mUuid  + ".expungePolicy", EXPUNGE_IMMEDIATELY);
        mSyncRemoteDeletions = prefs.getBoolean(mUuid  + ".syncRemoteDeletions", true);

        mMaxPushFolders = prefs.getInt(mUuid + ".maxPushFolders", 10);
        goToUnreadMessageSearch = prefs.getBoolean(mUuid + ".goToUnreadMessageSearch",
                                  false);
        subscribedFoldersOnly = prefs.getBoolean(mUuid + ".subscribedFoldersOnly",
                                false);
        maximumPolledMessageAge = prefs.getInt(mUuid
                                               + ".maximumPolledMessageAge", -1);
        maximumAutoDownloadMessageSize = prefs.getInt(mUuid
                                         + ".maximumAutoDownloadMessageSize", 32768);
        mQuotePrefix = prefs.getString(mUuid + ".quotePrefix", DEFAULT_QUOTE_PREFIX);
        for (String type : networkTypes)
        {
            Boolean useCompression = prefs.getBoolean(mUuid + ".useCompression." + type,
                                     true);
            compressionMap.put(type, useCompression);
        }

        mAutoExpandFolderName = prefs.getString(mUuid  + ".autoExpandFolderName",
                                                "INBOX");

        mAccountNumber = prefs.getInt(mUuid + ".accountNumber", 0);

        Random random = new Random((long)mAccountNumber+4);

        mChipColor = prefs.getInt(mUuid+".chipColor",
                                  (random.nextInt(0x70)) +
                                  (random.nextInt(0x70) * 0xff) +
                                  (random.nextInt(0x70) * 0xffff) +
                                  0xff000000);
        mLedColor = prefs.getInt(mUuid+".ledColor", mChipColor);

        mVibrate = prefs.getBoolean(mUuid + ".vibrate", false);
        mVibratePattern = prefs.getInt(mUuid + ".vibratePattern", 0);
        mVibrateTimes = prefs.getInt(mUuid + ".vibrateTimes", 5);

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

        try
        {
            mHideMessageViewMoveButtons = HideButtons.valueOf(prefs.getString(mUuid + ".hideMoveButtonsEnum",
                                          HideButtons.NEVER.name()));
        }
        catch (Exception e)
        {
            mHideMessageViewMoveButtons = HideButtons.NEVER;
        }

        try
        {
            mShowPictures = ShowPictures.valueOf(prefs.getString(mUuid + ".showPicturesEnum",
                                                 ShowPictures.NEVER.name()));
        }
        catch (Exception e)
        {
            mShowPictures = ShowPictures.NEVER;
        }

        mEnableMoveButtons = prefs.getBoolean(mUuid + ".enableMoveButtons", false);

        mRingtoneUri = prefs.getString(mUuid  + ".ringtone",
                                       "content://settings/system/notification_sound");
        try
        {
            mFolderDisplayMode = FolderMode.valueOf(prefs.getString(mUuid  + ".folderDisplayMode",
                                                    FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        }

        try
        {
            mFolderSyncMode = FolderMode.valueOf(prefs.getString(mUuid  + ".folderSyncMode",
                                                 FolderMode.FIRST_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderSyncMode = FolderMode.FIRST_CLASS;
        }

        try
        {
            mFolderPushMode = FolderMode.valueOf(prefs.getString(mUuid  + ".folderPushMode",
                                                 FolderMode.FIRST_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderPushMode = FolderMode.FIRST_CLASS;
        }

        try
        {
            mFolderTargetMode = FolderMode.valueOf(prefs.getString(mUuid  + ".folderTargetMode",
                                                   FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
            mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        }

        try
        {
            searchableFolders = Searchable.valueOf(prefs.getString(mUuid  + ".searchableFolders",
                                                   Searchable.ALL.name()));
        }
        catch (Exception e)
        {
            searchableFolders = Searchable.ALL;
        }

        mIsSignatureBeforeQuotedText = prefs.getBoolean(mUuid  + ".signatureBeforeQuotedText", false);
        identities = loadIdentities(prefs);

        mCryptoApp = prefs.getString(mUuid + ".cryptoApp", Apg.NAME);
        mCryptoAutoSignature = prefs.getBoolean(mUuid + ".cryptoAutoSignature", false);
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
        editor.remove(mUuid + ".name");
        editor.remove(mUuid + ".email");
        editor.remove(mUuid + ".alwaysBcc");
        editor.remove(mUuid + ".automaticCheckIntervalMinutes");
        editor.remove(mUuid + ".pushPollOnConnect");
        editor.remove(mUuid + ".saveAllHeaders");
        editor.remove(mUuid + ".idleRefreshMinutes");
        editor.remove(mUuid + ".lastAutomaticCheckTime");
        editor.remove(mUuid + ".notifyNewMail");
        editor.remove(mUuid + ".notifySelfNewMail");
        editor.remove(mUuid + ".deletePolicy");
        editor.remove(mUuid + ".draftsFolderName");
        editor.remove(mUuid + ".sentFolderName");
        editor.remove(mUuid + ".trashFolderName");
        editor.remove(mUuid + ".archiveFolderName");
        editor.remove(mUuid + ".spamFolderName");
        editor.remove(mUuid + ".outboxFolderName");
        editor.remove(mUuid + ".autoExpandFolderName");
        editor.remove(mUuid + ".accountNumber");
        editor.remove(mUuid + ".vibrate");
        editor.remove(mUuid + ".vibratePattern");
        editor.remove(mUuid + ".vibrateTimes");
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
        editor.remove(mUuid + ".syncRemoteDeletions");
        editor.remove(mUuid + ".maxPushFolders");
        editor.remove(mUuid  + ".searchableFolders");
        editor.remove(mUuid  + ".chipColor");
        editor.remove(mUuid  + ".ledColor");
        editor.remove(mUuid + ".goToUnreadMessageSearch");
        editor.remove(mUuid + ".subscribedFoldersOnly");
        editor.remove(mUuid + ".maximumPolledMessageAge");
        editor.remove(mUuid + ".maximumAutoDownloadMessageSize");
        editor.remove(mUuid + ".quotePrefix");
        for (String type : networkTypes)
        {
            editor.remove(mUuid + ".useCompression." + type);
        }
        deleteIdentities(preferences.getPreferences(), editor);
        editor.commit();
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
        editor.putInt(mUuid + ".idleRefreshMinutes", mIdleRefreshMinutes);
        editor.putBoolean(mUuid + ".saveAllHeaders", mSaveAllHeaders);
        editor.putBoolean(mUuid + ".pushPollOnConnect", mPushPollOnConnect);
        editor.putInt(mUuid + ".displayCount", mDisplayCount);
        editor.putLong(mUuid + ".lastAutomaticCheckTime", mLastAutomaticCheckTime);
        editor.putBoolean(mUuid + ".notifyNewMail", mNotifyNewMail);
        editor.putBoolean(mUuid + ".notifySelfNewMail", mNotifySelfNewMail);
        editor.putBoolean(mUuid + ".notifyMailCheck", mNotifySync);
        editor.putInt(mUuid + ".deletePolicy", mDeletePolicy);
        editor.putString(mUuid + ".draftsFolderName", mDraftsFolderName);
        editor.putString(mUuid + ".sentFolderName", mSentFolderName);
        editor.putString(mUuid + ".trashFolderName", mTrashFolderName);
        editor.putString(mUuid + ".archiveFolderName", mArchiveFolderName);
        editor.putString(mUuid + ".spamFolderName", mSpamFolderName);
        editor.putString(mUuid + ".outboxFolderName", mOutboxFolderName);
        editor.putString(mUuid + ".autoExpandFolderName", mAutoExpandFolderName);
        editor.putInt(mUuid + ".accountNumber", mAccountNumber);
        editor.putBoolean(mUuid + ".vibrate", mVibrate);
        editor.putInt(mUuid + ".vibratePattern", mVibratePattern);
        editor.putInt(mUuid + ".vibrateTimes", mVibrateTimes);
        editor.putBoolean(mUuid + ".ring", mRing);
        editor.putString(mUuid + ".hideButtonsEnum", mHideMessageViewButtons.name());
        editor.putString(mUuid + ".hideMoveButtonsEnum", mHideMessageViewMoveButtons.name());
        editor.putString(mUuid + ".showPicturesEnum", mShowPictures.name());
        editor.putBoolean(mUuid + ".enableMoveButtons", mEnableMoveButtons);
        editor.putString(mUuid + ".ringtone", mRingtoneUri);
        editor.putString(mUuid + ".folderDisplayMode", mFolderDisplayMode.name());
        editor.putString(mUuid + ".folderSyncMode", mFolderSyncMode.name());
        editor.putString(mUuid + ".folderPushMode", mFolderPushMode.name());
        editor.putString(mUuid + ".folderTargetMode", mFolderTargetMode.name());
        editor.putBoolean(mUuid + ".signatureBeforeQuotedText", this.mIsSignatureBeforeQuotedText);
        editor.putString(mUuid + ".expungePolicy", mExpungePolicy);
        editor.putBoolean(mUuid + ".syncRemoteDeletions", mSyncRemoteDeletions);
        editor.putInt(mUuid + ".maxPushFolders", mMaxPushFolders);
        editor.putString(mUuid  + ".searchableFolders", searchableFolders.name());
        editor.putInt(mUuid + ".chipColor", mChipColor);
        editor.putInt(mUuid + ".ledColor", mLedColor);
        editor.putBoolean(mUuid + ".goToUnreadMessageSearch", goToUnreadMessageSearch);
        editor.putBoolean(mUuid + ".subscribedFoldersOnly", subscribedFoldersOnly);
        editor.putInt(mUuid + ".maximumPolledMessageAge", maximumPolledMessageAge);
        editor.putInt(mUuid + ".maximumAutoDownloadMessageSize", maximumAutoDownloadMessageSize);
        editor.putString(mUuid + ".quotePrefix", mQuotePrefix);
        editor.putString(mUuid + ".cryptoApp", mCryptoApp);
        editor.putBoolean(mUuid + ".cryptoAutoSignature", mCryptoAutoSignature);

        for (String type : networkTypes)
        {
            Boolean useCompression = compressionMap.get(type);
            if (useCompression != null)
            {
                editor.putBoolean(mUuid + ".useCompression." + type, useCompression);
            }
        }
        saveIdentities(preferences.getPreferences(), editor);

        editor.commit();

    }

    public AccountStats getStats(Context context) throws MessagingException
    {
        long startTime = System.currentTimeMillis();
        AccountStats stats = new AccountStats();
        int unreadMessageCount = 0;
        int flaggedMessageCount = 0;
        LocalStore localStore = getLocalStore();
        if (K9.measureAccounts())
        {
            stats.size = localStore.getSize();
        }
        Account.FolderMode aMode = getFolderDisplayMode();
        Preferences prefs = Preferences.getPreferences(context);
        long folderLoadStart = System.currentTimeMillis();
        List<? extends Folder> folders = localStore.getPersonalNamespaces(false);
        long folderLoadEnd = System.currentTimeMillis();
        long folderEvalStart = folderLoadEnd;
        for (Folder folder : folders)
        {
            LocalFolder localFolder = (LocalFolder)folder;
            //folder.refresh(prefs);
            Folder.FolderClass fMode = localFolder.getDisplayClass(prefs);

            // Always get stats about the INBOX (see issue 1817)
            if (folder.getName().equals(K9.INBOX) || (
                        !folder.getName().equals(getTrashFolderName()) &&
                        !folder.getName().equals(getDraftsFolderName()) &&
                        !folder.getName().equals(getArchiveFolderName()) &&
                        !folder.getName().equals(getSpamFolderName()) &&
                        !folder.getName().equals(getOutboxFolderName()) &&
                        !folder.getName().equals(getSentFolderName()) &&
                        !folder.getName().equals(getErrorFolderName())))
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
                flaggedMessageCount += folder.getFlaggedMessageCount();

            }
        }
        long folderEvalEnd = System.currentTimeMillis();
        stats.unreadMessageCount = unreadMessageCount;
        stats.flaggedMessageCount = flaggedMessageCount;
        long endTime = System.currentTimeMillis();
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Account.getStats() on " + getDescription() + " took " + (endTime - startTime) + " ms;"
                  + " loading " + folders.size() + " took " + (folderLoadEnd - folderLoadStart) + " ms;"
                  + " evaluating took " + (folderEvalEnd - folderEvalStart) + " ms");
        return stats;
    }


    public synchronized void setChipColor(int color)
    {
        mChipColor = color;
    }

    public synchronized int getChipColor()
    {
        return mChipColor;
    }


    public synchronized void setLedColor(int color)
    {
        mLedColor = color;
    }

    public synchronized int getLedColor()
    {
        return mLedColor;
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

    public synchronized int getVibratePattern()
    {
        return mVibratePattern;
    }

    public synchronized void setVibratePattern(int pattern)
    {
        mVibratePattern = pattern;
    }

    public synchronized int getVibrateTimes()
    {
        return mVibrateTimes;
    }

    public synchronized void setVibrateTimes(int times)
    {
        mVibrateTimes = times;
    }



    /* Have we sent a new mail notification on this account */
    public boolean isRingNotified()
    {
        return mRingNotified;
    }

    public void setRingNotified(boolean ringNotified)
    {
        mRingNotified = ringNotified;
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

    public synchronized String getArchiveFolderName()
    {
        return mArchiveFolderName;
    }

    public synchronized void setArchiveFolderName(String archiveFolderName)
    {
        mArchiveFolderName = archiveFolderName;
    }

    public synchronized String getSpamFolderName()
    {
        return mSpamFolderName;
    }

    public synchronized void setSpamFolderName(String spamFolderName)
    {
        mSpamFolderName = spamFolderName;
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

    public synchronized HideButtons getHideMessageViewMoveButtons()
    {
        return mHideMessageViewMoveButtons;
    }

    public synchronized void setHideMessageViewMoveButtons(HideButtons hideMessageViewButtons)
    {
        mHideMessageViewMoveButtons = hideMessageViewButtons;
    }

    public synchronized ShowPictures getShowPictures()
    {
        return mShowPictures;
    }

    public synchronized void setShowPictures(ShowPictures showPictures)
    {
        mShowPictures = showPictures;
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

    public synchronized boolean shouldRing()
    {
        return mRing;
    }

    public synchronized void setRing(boolean ring)
    {
        mRing = ring;
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

    public synchronized void setCompression(String networkType, boolean useCompression)
    {
        compressionMap.put(networkType, useCompression);
    }

    public synchronized boolean useCompression(String networkType)
    {
        Boolean useCompression = compressionMap.get(networkType);
        if (useCompression == null)
        {
            return true;
        }
        else
        {
            return useCompression;
        }
    }

    public boolean useCompression(int type)
    {
        String networkType = TYPE_OTHER;
        switch (type)
        {
            case ConnectivityManager.TYPE_MOBILE:
                networkType = TYPE_MOBILE;
                break;
            case ConnectivityManager.TYPE_WIFI:
                networkType = TYPE_WIFI;
                break;
        }
        return useCompression(networkType);
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


    private synchronized List<Identity> loadIdentities(SharedPreferences prefs)
    {
        List<Identity> newIdentities = new ArrayList<Identity>();
        int ident = 0;
        boolean gotOne = false;
        do
        {
            gotOne = false;
            String name = prefs.getString(mUuid + ".name." + ident, null);
            String email = prefs.getString(mUuid + ".email." + ident, null);
            boolean signatureUse = prefs.getBoolean(mUuid  + ".signatureUse." + ident, true);
            String signature = prefs.getString(mUuid + ".signature." + ident, null);
            String description = prefs.getString(mUuid + ".description." + ident, null);
            final String replyTo = prefs.getString(mUuid + ".replyTo." + ident, null);
            if (email != null)
            {
                Identity identity = new Identity();
                identity.setName(name);
                identity.setEmail(email);
                identity.setSignatureUse(signatureUse);
                identity.setSignature(signature);
                identity.setDescription(description);
                identity.setReplyTo(replyTo);
                newIdentities.add(identity);
                gotOne = true;
            }
            ident++;
        }
        while (gotOne);

        if (newIdentities.size() == 0)
        {
            String name = prefs.getString(mUuid + ".name", null);
            String email = prefs.getString(mUuid + ".email", null);
            boolean signatureUse = prefs.getBoolean(mUuid  + ".signatureUse", true);
            String signature = prefs.getString(mUuid + ".signature", null);
            Identity identity = new Identity();
            identity.setName(name);
            identity.setEmail(email);
            identity.setSignatureUse(signatureUse);
            identity.setSignature(signature);
            identity.setDescription(email);
            newIdentities.add(identity);
        }

        return newIdentities;
    }

    private synchronized void deleteIdentities(SharedPreferences prefs, SharedPreferences.Editor editor)
    {
        int ident = 0;
        boolean gotOne = false;
        do
        {
            gotOne = false;
            String email = prefs.getString(mUuid + ".email." + ident, null);
            if (email != null)
            {
                editor.remove(mUuid + ".name." + ident);
                editor.remove(mUuid + ".email." + ident);
                editor.remove(mUuid + ".signatureUse." + ident);
                editor.remove(mUuid + ".signature." + ident);
                editor.remove(mUuid + ".description." + ident);
                editor.remove(mUuid + ".replyTo." + ident);
                gotOne = true;
            }
            ident++;
        }
        while (gotOne);
    }

    private synchronized void saveIdentities(SharedPreferences prefs, SharedPreferences.Editor editor)
    {
        deleteIdentities(prefs, editor);
        int ident = 0;

        for (Identity identity : identities)
        {
            editor.putString(mUuid + ".name." + ident, identity.getName());
            editor.putString(mUuid + ".email." + ident, identity.getEmail());
            editor.putBoolean(mUuid + ".signatureUse." + ident, identity.getSignatureUse());
            editor.putString(mUuid + ".signature." + ident, identity.getSignature());
            editor.putString(mUuid + ".description." + ident, identity.getDescription());
            editor.putString(mUuid + ".replyTo." + ident, identity.getReplyTo());
            ident++;
        }
    }

    public synchronized List<Identity> getIdentities()
    {
        return identities;
    }

    public synchronized void setIdentities(List<Identity> newIdentities)
    {
        identities = new ArrayList<Identity>(newIdentities);
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

    public synchronized Searchable getSearchableFolders()
    {
        return searchableFolders;
    }

    public synchronized void setSearchableFolders(Searchable searchableFolders)
    {
        this.searchableFolders = searchableFolders;
    }

    public synchronized int getIdleRefreshMinutes()
    {
        return mIdleRefreshMinutes;
    }

    public synchronized void setIdleRefreshMinutes(int idleRefreshMinutes)
    {
        mIdleRefreshMinutes = idleRefreshMinutes;
    }

    public synchronized boolean isPushPollOnConnect()
    {
        return mPushPollOnConnect;
    }

    public synchronized void setPushPollOnConnect(boolean pushPollOnConnect)
    {
        mPushPollOnConnect = pushPollOnConnect;
    }

    public synchronized boolean isSaveAllHeaders()
    {
        return mSaveAllHeaders;
    }

    public synchronized void setSaveAllHeaders(boolean saveAllHeaders)
    {
        mSaveAllHeaders = saveAllHeaders;
    }

    public synchronized boolean goToUnreadMessageSearch()
    {
        return goToUnreadMessageSearch;
    }

    public synchronized void setGoToUnreadMessageSearch(boolean goToUnreadMessageSearch)
    {
        this.goToUnreadMessageSearch = goToUnreadMessageSearch;
    }

    public synchronized boolean subscribedFoldersOnly()
    {
        return subscribedFoldersOnly;
    }

    public synchronized void setSubscribedFoldersOnly(boolean subscribedFoldersOnly)
    {
        this.subscribedFoldersOnly = subscribedFoldersOnly;
    }

    public synchronized int getMaximumPolledMessageAge()
    {
        return maximumPolledMessageAge;
    }

    public synchronized void setMaximumPolledMessageAge(int maximumPolledMessageAge)
    {
        this.maximumPolledMessageAge = maximumPolledMessageAge;
    }

    public synchronized int getMaximumAutoDownloadMessageSize()
    {
        return maximumAutoDownloadMessageSize;
    }

    public synchronized void setMaximumAutoDownloadMessageSize(int maximumAutoDownloadMessageSize)
    {
        this.maximumAutoDownloadMessageSize = maximumAutoDownloadMessageSize;
    }

    public Date getEarliestPollDate()
    {
        int age = getMaximumPolledMessageAge();
        if (age >= 0)
        {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            if (age < 28)
            {
                now.add(Calendar.DATE, age * -1);
            }
            else switch (age)
                {
                    case 28:
                        now.add(Calendar.MONTH, -1);
                        break;
                    case 56:
                        now.add(Calendar.MONTH, -2);
                        break;
                    case 84:
                        now.add(Calendar.MONTH, -3);
                        break;
                    case 168:
                        now.add(Calendar.MONTH, -6);
                        break;
                    case 365:
                        now.add(Calendar.YEAR, -1);
                        break;
                }

            return now.getTime();
        }
        else
        {
            return null;
        }
    }

    public synchronized String getQuotePrefix()
    {
        return mQuotePrefix;
    }

    public synchronized void setQuotePrefix(String quotePrefix)
    {
        mQuotePrefix = quotePrefix;
    }

    public boolean getEnableMoveButtons()
    {
        return mEnableMoveButtons;
    }

    public void setEnableMoveButtons(boolean enableMoveButtons)
    {
        mEnableMoveButtons = enableMoveButtons;
    }

    public String getCryptoApp()
    {
        return mCryptoApp;
    }

    public void setCryptoApp(String cryptoApp)
    {
        mCryptoApp = cryptoApp;
    }

    public boolean getCryptoAutoSignature()
    {
        return mCryptoAutoSignature;
    }

    public void setCryptoAutoSignature(boolean cryptoAutoSignature)
    {
        mCryptoAutoSignature = cryptoAutoSignature;
    }
    public synchronized boolean syncRemoteDeletions()
    {
        return mSyncRemoteDeletions;
    }

    public synchronized void setSyncRemoteDeletions(boolean syncRemoteDeletions)
    {
        mSyncRemoteDeletions = syncRemoteDeletions;
    }

    public synchronized String getLastSelectedFolderName()
    {
        return lastSelectedFolderName;
    }

    public synchronized void setLastSelectedFolderName(String folderName)
    {
        lastSelectedFolderName = folderName;
    }
}
