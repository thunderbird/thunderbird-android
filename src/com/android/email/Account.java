
package com.android.email;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import com.android.email.mail.Address;
import com.android.email.mail.Folder;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID. 
 */
public class Account implements Serializable {
    public static final int DELETE_POLICY_NEVER = 0;
    public static final int DELETE_POLICY_7DAYS = 1;
    public static final int DELETE_POLICY_ON_DELETE = 2;
    public static final int DELETE_POLICY_MARK_AS_READ = 3;
    
    private static final long serialVersionUID = 2975156672298625121L;

    String mUuid;
    String mStoreUri;
    String mLocalStoreUri;
    String mTransportUri;
    String mDescription;
    String mName;
    String mEmail;
    String mSignature;
    String mAlwaysBcc;
    int mAutomaticCheckIntervalMinutes;
    int mDisplayCount;
    long mLastAutomaticCheckTime;
    boolean mNotifyNewMail;
    String mDraftsFolderName;
    String mSentFolderName;
    String mTrashFolderName;
    String mOutboxFolderName;
    FolderMode mFolderDisplayMode;
    FolderMode mFolderSyncMode;
    FolderMode mFolderTargetMode;
    int mAccountNumber;
    boolean mVibrate;
    String mRingtoneUri;
    boolean mNotifySync;
    HideButtons mHideMessageViewButtons;
    
    public enum FolderMode {
    	ALL, FIRST_CLASS, FIRST_AND_SECOND_CLASS, NOT_SECOND_CLASS;
    }
    
    public enum HideButtons {
      NEVER, ALWAYS, KEYBOARD_AVAILABLE;
    }

    /**
     * <pre>
     * 0 Never 
     * 1 After 7 days 
     * 2 When I delete from inbox
     * </pre>
     */
    int mDeletePolicy;

    public Account(Context context) {
        // TODO Change local store path to something readable / recognizable
        mUuid = UUID.randomUUID().toString();
        mLocalStoreUri = "local://localhost/" + context.getDatabasePath(mUuid + ".db");
        mAutomaticCheckIntervalMinutes = -1;
        mDisplayCount = -1;
        mAccountNumber = -1;
        mNotifyNewMail = true;
        mNotifySync = true;
        mSignature = "Sent from my Android phone with K-9. Please excuse my brevity.";
        mVibrate = false;
        mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        mFolderSyncMode = FolderMode.FIRST_CLASS;
        mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        mHideMessageViewButtons = HideButtons.NEVER;
        mRingtoneUri = "content://settings/system/notification_sound";
    }

    Account(Preferences preferences, String uuid) {
        this.mUuid = uuid;
        refresh(preferences);
    }
    
    /**
     * Refresh the account from the stored settings.
     */
    public void refresh(Preferences preferences) {
        mStoreUri = Utility.base64Decode(preferences.getPreferences().getString(mUuid
                + ".storeUri", null));
        mLocalStoreUri = preferences.getPreferences().getString(mUuid + ".localStoreUri", null);
        mTransportUri = Utility.base64Decode(preferences.getPreferences().getString(mUuid
                + ".transportUri", null));
        mDescription = preferences.getPreferences().getString(mUuid + ".description", null);
        mAlwaysBcc = preferences.getPreferences().getString(mUuid + ".alwaysBcc", mAlwaysBcc);
        mName = preferences.getPreferences().getString(mUuid + ".name", mName);
        mEmail = preferences.getPreferences().getString(mUuid + ".email", mEmail);
        mSignature = preferences.getPreferences().getString(mUuid + ".signature", mSignature);
        mAutomaticCheckIntervalMinutes = preferences.getPreferences().getInt(mUuid
                + ".automaticCheckIntervalMinutes", -1);
        mDisplayCount = preferences.getPreferences().getInt(mUuid + ".displayCount", -1);
        mLastAutomaticCheckTime = preferences.getPreferences().getLong(mUuid
                + ".lastAutomaticCheckTime", 0);
        mNotifyNewMail = preferences.getPreferences().getBoolean(mUuid + ".notifyNewMail", 
                false);
        mNotifySync = preferences.getPreferences().getBoolean(mUuid + ".notifyMailCheck", 
																   false);
        mDeletePolicy = preferences.getPreferences().getInt(mUuid + ".deletePolicy", 0);
        mDraftsFolderName = preferences.getPreferences().getString(mUuid  + ".draftsFolderName", 
                "Drafts");
        mSentFolderName = preferences.getPreferences().getString(mUuid  + ".sentFolderName", 
                "Sent");
        mTrashFolderName = preferences.getPreferences().getString(mUuid  + ".trashFolderName", 
                "Trash");
        mOutboxFolderName = preferences.getPreferences().getString(mUuid  + ".outboxFolderName", 
                "Outbox");
        mAccountNumber = preferences.getPreferences().getInt(mUuid + ".accountNumber", 0);
        mVibrate = preferences.getPreferences().getBoolean(mUuid + ".vibrate", false);

        try
        {
          mHideMessageViewButtons = HideButtons.valueOf(preferences.getPreferences().getString(mUuid + ".hideButtonsEnum", 
              HideButtons.NEVER.name()));
        }
        catch (Exception e)
        {
          mHideMessageViewButtons = HideButtons.NEVER;
        }

        mRingtoneUri = preferences.getPreferences().getString(mUuid  + ".ringtone", 
                "content://settings/system/notification_sound");
        try
        {
        	mFolderDisplayMode = FolderMode.valueOf(preferences.getPreferences().getString(mUuid  + ".folderDisplayMode", 
        			FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
        	mFolderDisplayMode = FolderMode.NOT_SECOND_CLASS;
        }

        try
        {
        	mFolderSyncMode = FolderMode.valueOf(preferences.getPreferences().getString(mUuid  + ".folderSyncMode", 
        			FolderMode.FIRST_CLASS.name()));
        }
        catch (Exception e)
        {
        	mFolderSyncMode = FolderMode.FIRST_CLASS;
        }
        
        try
        {
          mFolderTargetMode = FolderMode.valueOf(preferences.getPreferences().getString(mUuid  + ".folderTargetMode", 
              FolderMode.NOT_SECOND_CLASS.name()));
        }
        catch (Exception e)
        {
          mFolderTargetMode = FolderMode.NOT_SECOND_CLASS;
        }

    }

    public String getUuid() {
        return mUuid;
    }

    public String getStoreUri() {
        return mStoreUri;
    }

    public void setStoreUri(String storeUri) {
        this.mStoreUri = storeUri;
    }

    public String getTransportUri() {
        return mTransportUri;
    }

    public void setTransportUri(String transportUri) {
        this.mTransportUri = transportUri;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getSignature() {
        return mSignature;
    }

    public void setSignature(String signature) {
        this.mSignature = signature;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

    public String getAlwaysBcc() {
        return mAlwaysBcc;
    }

    public void setAlwaysBcc(String alwaysBcc) {
        this.mAlwaysBcc = alwaysBcc;
    }

    
    public boolean isVibrate() {
        return mVibrate;
    }

    public void setVibrate(boolean vibrate) {
        mVibrate = vibrate;
    }

    public String getRingtone() {
        return mRingtoneUri;
    }

    public void setRingtone(String ringtoneUri) {
        mRingtoneUri = ringtoneUri;
    }

    public void delete(Preferences preferences) {
        String[] uuids = preferences.getPreferences().getString("accountUuids", "").split(",");
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = uuids.length; i < length; i++) {
            if (!uuids[i].equals(mUuid)) {
                if (sb.length() > 0) {
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
        editor.remove(mUuid + ".lastAutomaticCheckTime");
        editor.remove(mUuid + ".notifyNewMail");
        editor.remove(mUuid + ".deletePolicy");
        editor.remove(mUuid + ".draftsFolderName");
        editor.remove(mUuid + ".sentFolderName");
        editor.remove(mUuid + ".trashFolderName");
        editor.remove(mUuid + ".outboxFolderName");
        editor.remove(mUuid + ".accountNumber");
        editor.remove(mUuid + ".vibrate");
        editor.remove(mUuid + ".ringtone");
        editor.remove(mUuid + ".lastFullSync");
        editor.remove(mUuid + ".folderDisplayMode");
        editor.remove(mUuid + ".folderSyncMode");
        editor.remove(mUuid + ".folderTargetMode");
        editor.remove(mUuid + ".hideButtonsEnum");
        editor.commit();
    }

    public void save(Preferences preferences) {
      SharedPreferences.Editor editor = preferences.getPreferences().edit();

        if (!preferences.getPreferences().getString("accountUuids", "").contains(mUuid)) {
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
            for (int i = 0; i < accounts.length; i++) {
                accountNumbers[i] = accounts[i].getAccountNumber();
            }
            Arrays.sort(accountNumbers);
            for (int accountNumber : accountNumbers) {
                if (accountNumber > mAccountNumber + 1) {
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
        editor.putString(mUuid + ".name", mName);
        editor.putString(mUuid + ".email", mEmail);
        editor.putString(mUuid + ".signature", mSignature);
        editor.putString(mUuid + ".alwaysBcc", mAlwaysBcc);
        editor.putInt(mUuid + ".automaticCheckIntervalMinutes", mAutomaticCheckIntervalMinutes);
        editor.putInt(mUuid + ".displayCount", mDisplayCount);
        editor.putLong(mUuid + ".lastAutomaticCheckTime", mLastAutomaticCheckTime);
        editor.putBoolean(mUuid + ".notifyNewMail", mNotifyNewMail);
        editor.putBoolean(mUuid + ".notifyMailCheck", mNotifySync);
        editor.putInt(mUuid + ".deletePolicy", mDeletePolicy);
        editor.putString(mUuid + ".draftsFolderName", mDraftsFolderName);
        editor.putString(mUuid + ".sentFolderName", mSentFolderName);
        editor.putString(mUuid + ".trashFolderName", mTrashFolderName);
        editor.putString(mUuid + ".outboxFolderName", mOutboxFolderName);
        editor.putInt(mUuid + ".accountNumber", mAccountNumber);
        editor.putBoolean(mUuid + ".vibrate", mVibrate);
        editor.putString(mUuid + ".hideButtonsEnum", mHideMessageViewButtons.name());
        editor.putString(mUuid + ".ringtone", mRingtoneUri);
        editor.putString(mUuid + ".folderDisplayMode", mFolderDisplayMode.name());
        editor.putString(mUuid + ".folderSyncMode", mFolderSyncMode.name());
        editor.putString(mUuid + ".folderTargetMode", mFolderTargetMode.name());
       
        editor.commit();
    }

    public String toString() {
        return mDescription;
    }

    public Uri getContentUri() {
        return Uri.parse("content://accounts/" + getUuid());
    }

    public String getLocalStoreUri() {
        return mLocalStoreUri;
    }

    public void setLocalStoreUri(String localStoreUri) {
        this.mLocalStoreUri = localStoreUri;
    }

    /**
     * Returns -1 for never.
     */
    public int getAutomaticCheckIntervalMinutes() {
        return mAutomaticCheckIntervalMinutes;
    }
    
    public int getUnreadMessageCount(Context context, Application application) throws MessagingException
    {
    	int unreadMessageCount = 0;
      LocalStore localStore = (LocalStore) Store.getInstance(
              getLocalStoreUri(),
              application);
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
    
    // TODO: When there are multiple identities, this method should try all of them
    public boolean isAnIdentity(Address addr)
    {
      return getEmail().equals(addr.getAddress());
    }

    public int getDisplayCount() {
        if (mDisplayCount == -1) {
            this.mDisplayCount = Email.DEFAULT_VISIBLE_LIMIT;
        }
        return mDisplayCount;
    }

    /**
     * @param automaticCheckIntervalMinutes or -1 for never.
     */
    public void setAutomaticCheckIntervalMinutes(int automaticCheckIntervalMinutes) {
        this.mAutomaticCheckIntervalMinutes = automaticCheckIntervalMinutes;
    }

    /**
     * @param displayCount
     */
    public void setDisplayCount(int displayCount) {
        if (displayCount != -1) {
            this.mDisplayCount = displayCount;
        } else {
            this.mDisplayCount = Email.DEFAULT_VISIBLE_LIMIT;
        }
    }

    public long getLastAutomaticCheckTime() {
        return mLastAutomaticCheckTime;
    }

    public void setLastAutomaticCheckTime(long lastAutomaticCheckTime) {
        this.mLastAutomaticCheckTime = lastAutomaticCheckTime;
    }

    public boolean isNotifyNewMail() {
        return mNotifyNewMail;
    }

    public void setNotifyNewMail(boolean notifyNewMail) {
        this.mNotifyNewMail = notifyNewMail;
    }

    public int getDeletePolicy() {
        return mDeletePolicy;
    }

    public void setDeletePolicy(int deletePolicy) {
        this.mDeletePolicy = deletePolicy;
    }
    
    public String getDraftsFolderName() {
        return mDraftsFolderName;
    }

    public void setDraftsFolderName(String draftsFolderName) {
        mDraftsFolderName = draftsFolderName;
    }

    public String getSentFolderName() {
        return mSentFolderName;
    }
    
    public String getErrorFolderName()
    {
    	return Email.ERROR_FOLDER_NAME;
    }

    public void setSentFolderName(String sentFolderName) {
        mSentFolderName = sentFolderName;
    }

    public String getTrashFolderName() {
        return mTrashFolderName;
    }

    public void setTrashFolderName(String trashFolderName) {
        mTrashFolderName = trashFolderName;
    }
    
    public String getOutboxFolderName() {
        return mOutboxFolderName;
    }

    public void setOutboxFolderName(String outboxFolderName) {
        mOutboxFolderName = outboxFolderName;
    }
    
    public int getAccountNumber() {
        return mAccountNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Account) {
            return ((Account)o).mUuid.equals(mUuid);
        }
        return super.equals(o);
    }

		public FolderMode getFolderDisplayMode()
		{
			return mFolderDisplayMode;
		}

		public void setFolderDisplayMode(FolderMode displayMode)
		{
			mFolderDisplayMode = displayMode;
		}
		
		public FolderMode getFolderSyncMode()
		{
			return mFolderSyncMode;
		}

		public void setFolderSyncMode(FolderMode syncMode)
		{
			mFolderSyncMode = syncMode;
		}

    public boolean isShowOngoing()
    {
      return mNotifySync;
    }

    public void setShowOngoing(boolean showOngoing)
    {
      this.mNotifySync = showOngoing;
    }

    public HideButtons getHideMessageViewButtons()
    {
      return mHideMessageViewButtons;
    }

    public void setHideMessageViewButtons(HideButtons hideMessageViewButtons)
    {
      mHideMessageViewButtons = hideMessageViewButtons;
    }

    public FolderMode getFolderTargetMode()
    {
      return mFolderTargetMode;
    }

    public void setFolderTargetMode(FolderMode folderTargetMode)
    {
      mFolderTargetMode = folderTargetMode;
    }

}
