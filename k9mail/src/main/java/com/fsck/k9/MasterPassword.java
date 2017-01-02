package com.fsck.k9;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Date;

import static java.lang.System.currentTimeMillis;

public class MasterPassword implements ActivityLifecycleCallbacks {
    private static long mLastActive = 0;

    private void work(Activity activity) {
        if (!K9.useMasterPassword())
            return;
        long now = currentTimeMillis();
        if (mLastActive > 0 && now - mLastActive < K9.getMasterPasswordInterval()*60000 )
            return;
        mLastActive = now;
        (new PasswordDialog()).show(activity);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        work(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        work(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mLastActive = currentTimeMillis();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mLastActive = currentTimeMillis();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private static class PasswordDialog implements TextWatcher {
        private AlertDialog mDialog;
        private EditText mPasswordView;
        private String mPassword;
        public void show(final Activity activity){
            final ScrollView scrollView = new ScrollView(activity);
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.master_password_dialog_title);
            builder.setView(scrollView);
            builder.setPositiveButton(R.string.okay_action,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = mPasswordView.getText().toString();
                        if (password == K9.getMasterPassword()) {
                            dialog.dismiss();
                            mLastActive = currentTimeMillis();
                            return;
                        }
                        mPasswordView.setText("");
                    }
                }
            );
            mDialog = builder.create();
            View layout = mDialog.getLayoutInflater().inflate(R.layout.master_password_prompt,scrollView);
            TextView intro = (TextView) layout.findViewById(R.id.master_password_prompt_intro);
            intro.setText(R.string.master_password_prompt_intro);
            TextView prompt = (TextView) layout.findViewById(R.id.master_password_prompt);
            prompt.setText(R.string.master_password_prompt);
            mPasswordView = (EditText) layout.findViewById(R.id.master_password_input);
            mPasswordView.addTextChangedListener(this);

            mDialog.setCancelable(false);
            mDialog.show();

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean enable = mPasswordView.getText().length()>0;
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enable);
        }
    }
}
