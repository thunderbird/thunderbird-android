package com.fsck.k9.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.provider.EmailProvider;

/**
 * Cache to bridge the time needed to write (user-initiated) changes to the database.
 */
public class EmailProviderCache {
    public static final String ACTION_CACHE_UPDATED = "EmailProviderCache.ACTION_CACHE_UPDATED";

    private static Context sContext;
    private static Map<String, EmailProviderCache> sInstances =
            new HashMap<String, EmailProviderCache>();

    public static synchronized EmailProviderCache getCache(String accountUuid, Context context) {

        if (sContext == null) {
            sContext = context.getApplicationContext();
        }

        EmailProviderCache instance = sInstances.get(accountUuid);
        if (instance == null) {
            instance = new EmailProviderCache(accountUuid);
            sInstances.put(accountUuid, instance);
        }

        return instance;
    }


    private String mAccountUuid;
    private final Map<Long, Map<String, String>> mMessageCache = new HashMap<Long, Map<String, String>>();
    private final Map<Long, Map<String, String>> mThreadCache = new HashMap<Long, Map<String, String>>();
    private final Map<Long, Long> mHiddenMessageCache = new HashMap<Long, Long>();


    private EmailProviderCache(String accountUuid) {
        mAccountUuid = accountUuid;
    }

    public String getValueForMessage(Long messageId, String columnName) {
        synchronized (mMessageCache) {
            Map<String, String> map = mMessageCache.get(messageId);
            return (map == null) ? null : map.get(columnName);
        }
    }

    public String getValueForThread(Long threadRootId, String columnName) {
        synchronized (mThreadCache) {
            Map<String, String> map = mThreadCache.get(threadRootId);
            return (map == null) ? null : map.get(columnName);
        }
    }

    public void setValueForMessages(List<Long> messageIds, String columnName, String value) {
        synchronized (mMessageCache) {
            for (Long messageId : messageIds) {
                Map<String, String> map = mMessageCache.get(messageId);
                if (map == null) {
                    map = new HashMap<String, String>();
                    mMessageCache.put(messageId, map);
                }
                map.put(columnName, value);
            }
        }

        notifyChange();
    }

    public void setValueForThreads(List<Long> threadRootIds, String columnName, String value) {
        synchronized (mThreadCache) {
            for (Long threadRootId : threadRootIds) {
                Map<String, String> map = mThreadCache.get(threadRootId);
                if (map == null) {
                    map = new HashMap<String, String>();
                    mThreadCache.put(threadRootId, map);
                }
                map.put(columnName, value);
            }
        }

        notifyChange();
    }

    public void removeValueForMessages(List<Long> messageIds, String columnName) {
        synchronized (mMessageCache) {
            for (Long messageId : messageIds) {
                Map<String, String> map = mMessageCache.get(messageId);
                if (map != null) {
                    map.remove(columnName);
                    if (map.isEmpty()) {
                        mMessageCache.remove(messageId);
                    }
                }
            }
        }
    }

    public void removeValueForThreads(List<Long> threadRootIds, String columnName) {
        synchronized (mThreadCache) {
            for (Long threadRootId : threadRootIds) {
                Map<String, String> map = mThreadCache.get(threadRootId);
                if (map != null) {
                    map.remove(columnName);
                    if (map.isEmpty()) {
                        mThreadCache.remove(threadRootId);
                    }
                }
            }
        }
    }

    public void hideMessages(List<LocalMessage> messages) {
        synchronized (mHiddenMessageCache) {
            for (LocalMessage message : messages) {
                long messageId = message.getId();
                mHiddenMessageCache.put(messageId, message.getFolder().getId());
            }
        }

        notifyChange();
    }

    public boolean isMessageHidden(Long messageId, long folderId) {
        synchronized (mHiddenMessageCache) {
            Long hiddenInFolder = mHiddenMessageCache.get(messageId);
            return (hiddenInFolder != null && hiddenInFolder.longValue() == folderId);
        }
    }

    public void unhideMessages(List<? extends Message> messages) {
        synchronized (mHiddenMessageCache) {
            for (Message message : messages) {
                LocalMessage localMessage = (LocalMessage) message;
                long messageId = localMessage.getId();
                long folderId = ((LocalFolder) localMessage.getFolder()).getId();
                Long hiddenInFolder = mHiddenMessageCache.get(messageId);

                if (hiddenInFolder != null && hiddenInFolder.longValue() == folderId) {
                    mHiddenMessageCache.remove(messageId);
                }
            }
        }
    }

    /**
     * Notify all concerned parties that the message list has changed.
     *
     * <p><strong>Note:</strong>
     * Notifying the content resolver of the change will cause the {@code CursorLoader} in
     * {@link MessageListFragment} to reload the cursor. But especially with flag changes this will
     * block because of the DB write operation to update the flags. So additionally we use
     * {@link LocalBroadcastManager} to send a {@link #ACTION_CACHE_UPDATED} broadcast. This way
     * {@code MessageListFragment} can update the view without reloading the cursor.
     * </p>
     */
    private void notifyChange() {
        LocalBroadcastManager.getInstance(sContext).sendBroadcast(new Intent(ACTION_CACHE_UPDATED));

        Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + mAccountUuid +
                "/messages");
        sContext.getContentResolver().notifyChange(uri, null);
    }
}
