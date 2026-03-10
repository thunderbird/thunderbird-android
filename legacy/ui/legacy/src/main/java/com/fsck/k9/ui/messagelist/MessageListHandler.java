package com.fsck.k9.ui.messagelist;


import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Handler;

/**
 * This class is used to run operations that modify UI elements in the UI thread.
 *
 * <p>We are using convenience methods that add a {@link android.os.Message} instance or a
 * {@link Runnable} to the message queue.</p>
 *
 * <p><strong>Note:</strong> If you add a method to this class make sure you don't accidentally
 * perform the operation in the calling thread.</p>
 */
public class MessageListHandler extends Handler {
    private static final int ACTION_FOLDER_LOADING = 1;
    private static final int ACTION_REFRESH_TITLE = 2;
    private static final int ACTION_PROGRESS = 3;
    private static final int ACTION_REMOTE_SEARCH_FINISHED = 4;

    private WeakReference<BaseMessageListFragment> mFragment;

    public MessageListHandler(BaseMessageListFragment fragment) {
        mFragment = new WeakReference<>(fragment);
    }
    public void folderLoading(long folderId, boolean loading) {
        android.os.Message msg = android.os.Message.obtain(this, ACTION_FOLDER_LOADING,
                (loading) ? 1 : 0, 0, folderId);
        sendMessage(msg);
    }

    public void refreshTitle() {
        android.os.Message msg = android.os.Message.obtain(this, ACTION_REFRESH_TITLE);
        sendMessage(msg);
    }

    public void progress(final boolean progress) {
        android.os.Message msg = android.os.Message.obtain(this, ACTION_PROGRESS,
                (progress) ? 1 : 0, 0);
        sendMessage(msg);
    }

    public void remoteSearchFinished() {
        android.os.Message msg = android.os.Message.obtain(this, ACTION_REMOTE_SEARCH_FINISHED);
        sendMessage(msg);
    }

    public void updateFooter(final String message) {
        post(new Runnable() {
            @Override
            public void run() {
                BaseMessageListFragment fragment = mFragment.get();
                if (fragment != null) {
                    fragment.updateFooterText(message);
                }
            }
        });
    }

    @Override
    public void handleMessage(android.os.Message msg) {
        BaseMessageListFragment fragment = mFragment.get();
        if (fragment == null) {
            return;
        }

        // The following messages don't need an attached activity.
        switch (msg.what) {
            case ACTION_REMOTE_SEARCH_FINISHED: {
                fragment.remoteSearchFinished();
                return;
            }
        }

        // Discard messages if the fragment isn't attached to an activity anymore.
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }

        switch (msg.what) {
            case ACTION_FOLDER_LOADING: {
                long folderId = (Long) msg.obj;
                boolean loading = (msg.arg1 == 1);
                fragment.folderLoading(folderId, loading);
                break;
            }
            case ACTION_REFRESH_TITLE: {
                fragment.updateTitle();
                break;
            }
            case ACTION_PROGRESS: {
                boolean progress = (msg.arg1 == 1);
                fragment.progress(progress);
                break;
            }
        }
    }
}
