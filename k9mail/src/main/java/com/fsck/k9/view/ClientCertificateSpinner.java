
package com.fsck.k9.view;

import com.fsck.k9.R;

import android.app.Activity;
import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.AttributeSet;
import timber.log.Timber;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class ClientCertificateSpinner extends LinearLayout {
    Activity activity;
    OnClientCertificateChangedListener listener;

    Button selection;
    ImageButton deleteButton;

    String alias;

    public interface OnClientCertificateChangedListener {
        void onClientCertificateChanged(String alias);
    }

    public void setOnClientCertificateChangedListener(OnClientCertificateChangedListener listener) {
        this.listener = listener;
    }

    public ClientCertificateSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            Timber.e("ClientCertificateSpinner init failed! Please inflate with Activity!");
        }

        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.client_certificate_spinner, this, true);

        selection = (Button) findViewById(R.id.client_certificate_spinner_button);
        selection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseCertificate();
            }
        });

        deleteButton = (ImageButton) findViewById(R.id.client_certificate_spinner_delete);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDelete();
            }
        });
    }

    public void setAlias(String alias) {
        // Note: KeyChainAliasCallback gives back "" on cancel
        if (alias != null && alias.equals("")) {
            alias = null;
        }

        this.alias = alias;
        // Note: KeyChainAliasCallback is a different thread than the UI
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView();
                if (listener != null) {
                    listener.onClientCertificateChanged(ClientCertificateSpinner.this.alias);
                }
            }
        });
    }

    public String getAlias() {
        String alias = selection.getText().toString();
        if (alias.equals(activity.getString(R.string.client_certificate_spinner_empty))) {
            return null;
        } else {
            return alias;
        }
    }

    private void onDelete() {
        setAlias(null);
    }

    public void chooseCertificate() {
        // NOTE: keyTypes, issuers, hosts, port are not known before we actually
        // open a connection, thus we cannot set them here!
        KeyChain.choosePrivateKeyAlias(activity, new KeyChainAliasCallback() {
            @Override
            public void alias(String alias) {
                Timber.d("User has selected client certificate alias: %s", alias);

                setAlias(alias);
            }
        }, null, null, null, -1, getAlias());
    }

    private void updateView() {
        if (alias != null) {
            selection.setText(alias);
        } else {
            selection.setText(R.string.client_certificate_spinner_empty);
        }
    }

}
