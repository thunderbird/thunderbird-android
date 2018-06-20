
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
    Activity mActivity;
    OnClientCertificateChangedListener mListener;

    Button mSelection;
    ImageButton mDeleteButton;

    String mAlias;

    public interface OnClientCertificateChangedListener {
        void onClientCertificateChanged(String alias);
    }

    public void setOnClientCertificateChangedListener(OnClientCertificateChangedListener listener) {
        mListener = listener;
    }

    public ClientCertificateSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof Activity) {
            mActivity = (Activity) context;
        } else {
            Timber.e("ClientCertificateSpinner init failed! Please inflate with Activity!");
        }

        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.client_certificate_spinner, this, true);

        mSelection = (Button) findViewById(R.id.client_certificate_spinner_button);
        mSelection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseCertificate();
            }
        });

        mDeleteButton = (ImageButton) findViewById(R.id.client_certificate_spinner_delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {
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

        mAlias = alias;
        // Note: KeyChainAliasCallback is a different thread than the UI
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView();
                if (mListener != null) {
                    mListener.onClientCertificateChanged(mAlias);
                }
            }
        });
    }

    public String getAlias() {
        String alias = mSelection.getText().toString();
        if (alias.equals(mActivity.getString(R.string.client_certificate_spinner_empty))) {
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
        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
            @Override
            public void alias(String alias) {
                Timber.d("User has selected client certificate alias: %s", alias);

                setAlias(alias);
            }
        }, null, null, null, -1, getAlias());
    }

    private void updateView() {
        if (mAlias != null) {
            mSelection.setText(mAlias);
        } else {
            mSelection.setText(R.string.client_certificate_spinner_empty);
        }
    }

}
