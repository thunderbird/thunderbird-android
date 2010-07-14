package com.fsck.k9.activity;

import java.text.DateFormat;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.service.MailService;

public class ActivityListener extends MessagingListener
{
    private String mLoadingFolderName = null;
    private String mLoadingHeaderFolderName = null;
    private String mLoadingAccountDescription = null;
    private String mSendingAccountDescription = null;
    private int mFolderCompleted = 0;
    private int mFolderTotal = 0;
    private String mProcessingAccountDescription = null;
    private String mProcessingCommandTitle = null;


    public String formatHeader(Context context, String activityPrefix, int unreadMessageCount, DateFormat timeFormat)
    {
        String operation = null;
        String progress = null;
        if (mLoadingAccountDescription  != null
                || mSendingAccountDescription != null
                || mLoadingHeaderFolderName != null
                || mProcessingAccountDescription != null)
        {
            progress = (mFolderTotal > 0 ?
                        context.getString(R.string.folder_progress, mFolderCompleted, mFolderTotal) : "");

            if (mLoadingFolderName != null || mLoadingHeaderFolderName != null)
            {
                String displayName = mLoadingFolderName;
                if (K9.INBOX.equalsIgnoreCase(displayName))
                {
                    displayName = context.getString(R.string.special_mailbox_name_inbox);
                }

                if (mLoadingHeaderFolderName != null)
                {

                    operation = context.getString(R.string.status_loading_account_folder_headers, mLoadingAccountDescription, displayName, progress);
                }
                else
                {
                    operation = context.getString(R.string.status_loading_account_folder, mLoadingAccountDescription, displayName, progress);
                }
            }

            else if (mSendingAccountDescription != null)
            {
                operation = context.getString(R.string.status_sending_account, mSendingAccountDescription, progress);
            }
            else if (mProcessingAccountDescription != null)
            {
                operation = context.getString(R.string.status_processing_account, mProcessingAccountDescription,
                                              mProcessingCommandTitle != null ? mProcessingCommandTitle : "",
                                              progress);
            }
        }
        else
        {
            long nextPollTime = MailService.getNextPollTime();
            if (nextPollTime != -1)
            {
                operation = context.getString(R.string.status_next_poll, timeFormat.format(nextPollTime));
            }
            else
            {
                operation = context.getString(R.string.status_polling_off);
            }
        }

        return context.getString(R.string.activity_header_format, activityPrefix,
                                 (unreadMessageCount > 0 ? context.getString(R.string.activity_unread_count, unreadMessageCount) : ""),
                                 operation);


    }

    @Override
    public void synchronizeMailboxFinished(
        Account account,
        String folder,
        int totalMessagesInMailbox,
        int numNewMessages)
    {
        mLoadingAccountDescription = null;
        mLoadingFolderName = null;
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folder)
    {
        mLoadingAccountDescription = account.getDescription();
        mLoadingFolderName = folder;
        mFolderCompleted = 0;
        mFolderTotal = 0;
    }


    @Override
    public void synchronizeMailboxHeadersStarted(Account account, String folder)
    {
        mLoadingHeaderFolderName = folder;
    }


    @Override
    public void synchronizeMailboxHeadersProgress(Account account, String folder, int completed, int total)
    {
        mFolderCompleted = completed;
        mFolderTotal = total;
    }

    @Override
    public void synchronizeMailboxHeadersFinished(Account account, String folder,
            int total, int completed)
    {
        mLoadingHeaderFolderName = null;
    }


    @Override
    public void synchronizeMailboxProgress(Account account, String folder, int completed, int total)
    {
        mFolderCompleted = completed;
        mFolderTotal = total;
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folder,
                                         String message)
    {
        mLoadingAccountDescription = null;
        mLoadingFolderName = null;

    }

    @Override
    public void sendPendingMessagesStarted(Account account)
    {
        mSendingAccountDescription = account.getDescription();
    }

    @Override
    public void sendPendingMessagesCompleted(Account account)
    {
        mSendingAccountDescription = null;
    }

    @Override
    public void sendPendingMessagesFailed(Account account)
    {
        mSendingAccountDescription = null;
    }

    @Override
    public void pendingCommandsProcessing(Account account)
    {
        mProcessingAccountDescription = account.getDescription();
        mFolderCompleted = 0;
        mFolderTotal = 0;
    }

    @Override
    public void pendingCommandsFinished(Account account)
    {
        mProcessingAccountDescription = null;
    }

    @Override
    public void pendingCommandStarted(Account account, String commandTitle)
    {
        mProcessingCommandTitle = commandTitle;
    }

    @Override
    public void pendingCommandCompleted(Account account, String commandTitle)
    {
        mProcessingCommandTitle = null;
    }

    public int getFolderCompleted()
    {
        return mFolderCompleted;
    }

    public int getFolderTotal()
    {
        return mFolderTotal;
    }

}
