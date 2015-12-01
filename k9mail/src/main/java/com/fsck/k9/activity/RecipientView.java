package com.fsck.k9.activity;


import java.util.Arrays;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.RecipientSelectView.Recipient;
import com.fsck.k9.mail.Address;
import com.tokenautocomplete.TokenCompleteTextView.TokenListener;


public class RecipientView {

    private final Activity activity;

    private final LinearLayout ccWrapper;
    private final LinearLayout bccWrapper;
    private final RecipientSelectView toView;
    private final RecipientSelectView ccView;
    private final RecipientSelectView bccView;
    private final ViewAnimator cryptoStatus;

    private RecipientPresenter presenter;

    public RecipientView(Activity activity) {
        this.activity = activity;

        toView = (RecipientSelectView) activity.findViewById(R.id.to);
        ccView = (RecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (RecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = (LinearLayout) activity.findViewById(R.id.cc_wrapper);
        bccWrapper = (LinearLayout) activity.findViewById(R.id.bcc_wrapper);
        cryptoStatus = (ViewAnimator) activity.findViewById(R.id.crypto_status);
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

    public void toFieldRequestFocus() {
        toView.requestFocus();
    }

    public void setFontSizes(FontSizes fontSizes, int fontSize) {
        fontSizes.setViewTextSize(toView, fontSize);
        fontSizes.setViewTextSize(ccView, fontSize);
        fontSizes.setViewTextSize(bccView, fontSize);
    }

    public void addToRecipients(Recipient... recipients) {
        toView.addRecipients(recipients);
    }

    public void addCcRecipients(Recipient... recipients) {
        ccView.addRecipients(recipients);
    }

    public void addBccRecipients(Recipient... recipients) {
        bccView.addRecipients(recipients);
    }

    public void setCcVisibility(boolean visible) {
        ccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setBccVisibility(boolean visible) {
        bccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
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

    public void invalidateOptionsMenu() {
        activity.invalidateOptionsMenu();
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
        if (cryptoStatus.getVisibility() == View.GONE) {
            return;
        }

        cryptoStatus.animate().translationX(100).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                cryptoStatus.setVisibility(View.GONE);
            }
        }).start();
    }

    public void showCryptoStatus(final boolean allKeys, final boolean allVerified) {
        if (cryptoStatus.getVisibility() == View.VISIBLE) {
            switchCryptoStatus(allKeys, allVerified);
            return;
        }

        cryptoStatus.setTranslationX(100);
        cryptoStatus.setVisibility(View.VISIBLE);
        cryptoStatus.animate().translationX(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                switchCryptoStatus(allKeys, allVerified);
            }
        }).start();
    }

    private void switchCryptoStatus(boolean allKeys, boolean allVerified) {
        int childtoDisplay = allKeys ? (allVerified ? 2 : 1) : 0;
        if (cryptoStatus.getDisplayedChild() != childtoDisplay) {
            cryptoStatus.setDisplayedChild(childtoDisplay);
        }
    }
}