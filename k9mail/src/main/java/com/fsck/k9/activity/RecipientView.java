package com.fsck.k9.activity;


import java.util.Arrays;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.RecipientSelectView;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.tokenautocomplete.TokenCompleteTextView.TokenListener;


public class RecipientView implements OnFocusChangeListener, OnClickListener {

    private final MessageCompose activity;

    private final View ccWrapper, ccDivider;
    private final View bccWrapper, bccDivider;
    private final RecipientSelectView toView;
    private final RecipientSelectView ccView;
    private final RecipientSelectView bccView;
    private final ViewAnimator cryptoStatusView;
    private final ViewAnimator recipientExpanderContainer;

    private RecipientPresenter presenter;

    public RecipientView(MessageCompose activity) {
        this.activity = activity;

        toView = (RecipientSelectView) activity.findViewById(R.id.to);
        ccView = (RecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (RecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = activity.findViewById(R.id.cc_wrapper);
        ccDivider = activity.findViewById(R.id.cc_divider);
        bccWrapper = activity.findViewById(R.id.bcc_wrapper);
        bccDivider = activity.findViewById(R.id.bcc_divider);
        recipientExpanderContainer = (ViewAnimator) activity.findViewById(R.id.recipient_expander_container);
        cryptoStatusView = (ViewAnimator) activity.findViewById(R.id.crypto_status);
        cryptoStatusView.setOnClickListener(this);

        toView.setOnFocusChangeListener(this);
        ccView.setOnFocusChangeListener(this);
        bccView.setOnFocusChangeListener(this);

        View recipientExpander = activity.findViewById(R.id.recipient_expander);
        recipientExpander.setOnClickListener(this);
    }

    public void setPresenter(final RecipientPresenter presenter) {
        this.presenter = presenter;

        if (presenter == null) {
            toView.setTokenListener(null);
            ccView.setTokenListener(null);
            bccView.setTokenListener(null);
            return;
        }

        // wire the view listeners directly to the presenter - saves a stack frame
        toView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onToTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onToTokenRemoved(recipient);
            }
        });

        ccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onCcTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onCcTokenRemoved(recipient);
            }
        });

        bccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onBccTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onBccTokenRemoved(recipient);
            }
        });
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        toView.addTextChangedListener(textWatcher);
        ccView.addTextChangedListener(textWatcher);
        bccView.addTextChangedListener(textWatcher);
    }

    public void setCryptoProvider(String openPgpProvider) {
        toView.setCryptoProvider(openPgpProvider);
        ccView.setCryptoProvider(openPgpProvider);
        bccView.setCryptoProvider(openPgpProvider);
    }

    public void requestFocusOnToFied() {
        toView.requestFocus();
    }

    public void setFontSizes(FontSizes fontSizes, int fontSize) {
        fontSizes.setViewTextSize(toView, fontSize);
        fontSizes.setViewTextSize(ccView, fontSize);
        fontSizes.setViewTextSize(bccView, fontSize);
    }

    public void addRecipients(RecipientType recipientType, Recipient... recipients) {
        switch (recipientType) {
            case TO:
                toView.addRecipients(recipients);
                break;
            case CC:
                ccView.addRecipients(recipients);
                break;
            case BCC:
                bccView.addRecipients(recipients);
                break;
        }
    }

    public void setCcVisibility(boolean visible) {
        ccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
        ccDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setBccVisibility(boolean visible) {
        bccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
        bccDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setRecipientExpanderVisibility(boolean visible) {
        int childToDisplay = visible ? 0 : 1;
        if (recipientExpanderContainer.getDisplayedChild() != childToDisplay) {
            recipientExpanderContainer.setDisplayedChild(childToDisplay);
        }
    }

    public boolean isCcVisible() {
        return ccWrapper.getVisibility() == View.VISIBLE;
    }

    public boolean isBccVisible() {
        return bccWrapper.getVisibility() == View.VISIBLE;
    }

    public void showNoRecipientsError() {
        toView.setError(toView.getContext().getString(R.string.message_compose_error_no_recipients));
    }

    public List<Address> getToAddresses() {
        return Arrays.asList(toView.getAddresses());
    }

    public List<Address> getCcAddresses() {
        return Arrays.asList(ccView.getAddresses());
    }

    public List<Address> getBccAddresses() {
        return Arrays.asList(bccView.getAddresses());
    }

    public List<Recipient> getToRecipients() {
        return toView.getObjects();
    }
    public List<Recipient> getCcRecipients() {
        return ccView.getObjects();
    }
    public List<Recipient> getBccRecipients() {
        return bccView.getObjects();
    }

    public void hideCryptoStatus() {
        if (cryptoStatusView.getVisibility() == View.GONE) {
            return;
        }

        cryptoStatusView.animate().translationX(100).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                cryptoStatusView.setVisibility(View.GONE);
            }
        }).start();
    }

    public enum CryptoStatusType {
        DISABLED(4), SIGN_ONLY(3), OPPORTUNISTIC_NOKEY(0), OPPORTUNISTIC_UNTRUSTED(1), OPPORTUNISTIC_TRUSTED(2);
        final int childToDisplay;
        CryptoStatusType(int childToDisplay) {
            this.childToDisplay = childToDisplay;
        }
    }
    public void showCryptoStatus(final CryptoStatusType childToDisplay) {
        if (cryptoStatusView.getVisibility() == View.VISIBLE) {
            switchCryptoStatus(childToDisplay);
            return;
        }

        cryptoStatusView.setTranslationX(100);
        cryptoStatusView.setVisibility(View.VISIBLE);
        cryptoStatusView.animate().translationX(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                switchCryptoStatus(childToDisplay);
            }
        }).start();
    }

    private void switchCryptoStatus(CryptoStatusType cryptoStatus) {
        int childToDisplay = cryptoStatus.childToDisplay;
        if (cryptoStatusView.getDisplayedChild() != childToDisplay) {
            cryptoStatusView.setDisplayedChild(childToDisplay);
        }
    }

    public void showContactPicker(int requestCode) {
        // this is an extra indirection to keep the view here clear
        activity.showContactPicker(requestCode);
    }

    public void showErrorContactNoAddress() {
        Toast.makeText(activity, activity.getString(R.string.error_contact_address_not_found), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }
        switch(v.getId()) {
            case R.id.to:
                presenter.onToFocused();
                break;
            case R.id.cc:
                presenter.onCcFocused();
                break;
            case R.id.bcc:
                presenter.onBccFocused();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.recipient_expander:
                presenter.onClickRecipientExpander();
                break;
            case R.id.crypto_status:
                presenter.onClickCryptoStatus();
                break;
        }
    }

    public void showCryptoDialog(CryptoMode currentCryptoMode) {
        CryptoSettingsDialog dialog = CryptoSettingsDialog.newInstance(currentCryptoMode);
        dialog.show(activity.getFragmentManager(), "crypto_settings");
    }

}