
package com.fsck.k9.mail;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.activity.setup.AccountSetupOutgoing;
import com.fsck.k9.controller.MessagingController;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;

    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(final String message,
            Throwable throwable, final Account account, final boolean incoming) {
        super(message, throwable);
        /*
         * We get here because of an SSLException. We are only interested in
         * creating a notification if the underlying cause is certificate
         * related.
         */
        while (throwable != null
                && !(throwable instanceof CertPathValidatorException)
                && !(throwable instanceof CertificateException)) {
            throwable = throwable.getCause();
        }
        if (throwable == null)
            return;
        final Application app = K9.app;
        final String title = app
                .getString(R.string.notification_certificate_error_title);
        final String text = app
                .getString(R.string.notification_certificate_error_text);
        final Class<?> className;
        final String extraName;
        final int id;
        if (incoming) {
            className = AccountSetupIncoming.class;
            extraName = AccountSetupIncoming.EXTRA_ACCOUNT;
            id = K9.CERTIFICATE_EXCEPTION_NOTIFICATION_INCOMING;
        } else {
            className = AccountSetupOutgoing.class;
            extraName = AccountSetupOutgoing.EXTRA_ACCOUNT;
            id = K9.CERTIFICATE_EXCEPTION_NOTIFICATION_OUTGOING;
        }
        final Intent i = new Intent(app, className);
        final String uuid = account.getUuid();
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(extraName, uuid);
        final PendingIntent pi = PendingIntent.getActivity(app, 0, i, 0);
        MessagingController controller = MessagingController.getInstance(app);
        controller.notify(uuid, id, title, text, pi);
    }
}