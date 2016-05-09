package com.fsck.k9.ui.messageview;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageCryptoDisplayStatus;


public class MessageCryptoPresenter implements OnCryptoClickListener {
    public static final int REQUEST_CODE_UNKNOWN_KEY = 123;


    MessageCryptoMvpView messageCryptoMvpView;

    private MessageViewInfo messageViewInfo;


    public MessageCryptoPresenter(MessageCryptoMvpView messageCryptoMvpView) {
        this.messageCryptoMvpView = messageCryptoMvpView;
    }

    public void setMessageViewInfo(MessageViewInfo messageViewInfo) {
        this.messageViewInfo = messageViewInfo;
    }

    @Override
    public void onCryptoClick() {
        if (messageViewInfo == null) {
            return;
        }
        MessageCryptoDisplayStatus displayStatus =
                MessageCryptoDisplayStatus.fromResultAnnotation(messageViewInfo.cryptoResultAnnotation);
        switch (displayStatus) {
            case LOADING:
                // no need to do anything, there is a progress bar...
                break;
            case UNENCRYPTED_SIGN_UNKNOWN:
                launchPendingIntent(messageViewInfo);
                break;
            default:
                displayCryptoInfoDialog(displayStatus);
                break;
        }
    }

    @SuppressWarnings("UnusedParameters") // for consistency with Activity.onActivityResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_UNKNOWN_KEY) {
            throw new IllegalStateException("got an activity result that wasn't meant for us. this is a bug!");
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        messageCryptoMvpView.restartMessageCryptoProcessing();
    }

    private void displayCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus) {
        messageCryptoMvpView.showCryptoInfoDialog(displayStatus);
    }

    private void launchPendingIntent(MessageViewInfo messageViewInfo) {
        try {
            PendingIntent pendingIntent = messageViewInfo.cryptoResultAnnotation.getOpenPgpPendingIntent();
            if (pendingIntent != null) {
                messageCryptoMvpView.startPendingIntentForCryptoPresenter(pendingIntent.getIntentSender(),
                        REQUEST_CODE_UNKNOWN_KEY, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(K9.LOG_TAG, "SendIntentException", e);
        }
    }

    public interface MessageCryptoMvpView {
        void restartMessageCryptoProcessing();

        void startPendingIntentForCryptoPresenter(IntentSender si, int requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) throws IntentSender.SendIntentException;

        void showCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus);
    }
}
