package com.fsck.k9.activity.setup;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.fsck.k9.Preferences;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.ui.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.message.html.HtmlConverter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Displays a welcome message when no accounts have been created yet.
 */
public class WelcomeMessage extends K9Activity implements OnClickListener{

    public static void showWelcomeMessage(Context context) {
        Intent intent = new Intent(context, WelcomeMessage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setLayout(R.layout.welcome_message);

        TextView welcome = findViewById(R.id.welcome_message);
        welcome.setText(HtmlConverter.htmlToSpanned(getString(R.string.accounts_welcome)));
        welcome.setMovementMethod(LinkMovementMethod.getInstance());

        findViewById(R.id.next).setOnClickListener(this);
        findViewById(R.id.import_settings).setOnClickListener(this);
        checkPhoneStatePermission();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.next) {
            AccountSetupBasics.actionNewAccount(this);
            finish();
        } else if (id == R.id.import_settings) {
            Accounts.importSettings(this);
            finish();
        }
    }

    private boolean hasReadPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPhoneStatePermission() {
        if (!hasReadPhoneStatePermission()) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(getString(R.string.clientID_security_request_title));
            alertBuilder.setMessage(getString(R.string.clientID_security_request_details_message));
            final Activity thisActivity = this;
            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.READ_PHONE_STATE}, 2);
                }
            });
            alertBuilder.setNegativeButton(android.R.string.no, null);
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            addImei();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull
                                           String[] permissions, @NonNull int[] grantResults) {
        if (hasReadPhoneStatePermission()) {
           addImei();
        }
    }

    private void addImei() {
        Preferences newPreference = Preferences.getPreferences(this);
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        StorageEditor editor = newPreference.createStorageEditor();
        editor.putString("imei", imei).commit();
    }

}
