package com.fsck.k9.account;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings.Type;
import com.larswerkman.colorpicker.ColorPicker;


/**
 * Deals with logic surrounding account creation.
 * <p/>
 * TODO Move much of the code from com.fsck.k9.activity.setup.* into here
 */
public class AccountCreator {
    /*
     * https://developer.android.com/design/style/color.html
     * Note: Order does matter, it's the order in which they will be picked.
     */
    private static final Integer[] PREDEFINED_COLORS = new Integer[] {
            Color.parseColor("#0099CC"),    // blue
            Color.parseColor("#669900"),    // green
            Color.parseColor("#FF8800"),    // orange
            Color.parseColor("#CC0000"),    // red
            Color.parseColor("#9933CC")     // purple
    };


    public static DeletePolicy getDefaultDeletePolicy(Type type) {
        switch (type) {
            case IMAP: {
                return DeletePolicy.ON_DELETE;
            }
            case POP3: {
                return DeletePolicy.NEVER;
            }
            case WebDAV: {
                return DeletePolicy.ON_DELETE;
            }
            case SMTP: {
                throw new IllegalStateException("Delete policy doesn't apply to SMTP");
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    public static int getDefaultPort(ConnectionSecurity securityType, Type storeType) {
        switch (securityType) {
            case NONE:
            case STARTTLS_REQUIRED: {
                return storeType.defaultPort;
            }
            case SSL_TLS_REQUIRED: {
                return storeType.defaultTlsPort;
            }
        }

        throw new AssertionError("Unhandled ConnectionSecurity type encountered: " + securityType);
    }

    /*
     * Pick a nice Android guidelines color if we haven't used them all yet.
     */
    public static int pickColor(Context context) {
        List<Account> accounts = Preferences.getPreferences(context).getAccounts();

        List<Integer> availableColors = new ArrayList<>(PREDEFINED_COLORS.length);
        Collections.addAll(availableColors, PREDEFINED_COLORS);

        for (Account account : accounts) {
            Integer color = account.getChipColor();
            if (availableColors.contains(color)) {
                availableColors.remove(color);
                if (availableColors.isEmpty()) {
                    break;
                }
            }
        }

        return (availableColors.isEmpty()) ? ColorPicker.getRandomColor() : availableColors.get(0);
    }
}
