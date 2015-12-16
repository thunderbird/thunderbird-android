package com.fsck.k9.message;


import android.content.Context;
import android.os.AsyncTask;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;


public class SimpleMessageBuilder extends MessageBuilder {

    public SimpleMessageBuilder(Context context) {
        super(context);
    }

    @Override
    public void buildAsync(Callback callback) {
        super.buildAsync(callback);

        new AsyncTask<Void, Void, MimeMessage>() {
            MessagingException me;

            @Override
            protected MimeMessage doInBackground(Void... params) {
                try {
                        /* TODO this is just for debugging, uncomment to test slow message building
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        */
                    return build();
                } catch (MessagingException me) {
                    this.me = me;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(MimeMessage message) {
                if (this.me != null) {
                    returnMessageBuildException(me);
                } else {
                    returnMessageBuildSuccess(message);
                }
            }
        }.execute();
    }

}
