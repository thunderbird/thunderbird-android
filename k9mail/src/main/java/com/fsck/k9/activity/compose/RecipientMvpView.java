package com.fsck.k9.activity.compose;


import java.util.Arrays;
import java.util.List;

import android.app.LoaderManager;
import android.app.PendingIntent;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.view.RecipientSelectView;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.TokenListener;
import com.fsck.k9.view.ToolableViewAnimator;


public class RecipientMvpView implements OnFocusChangeListener, OnClickListener {
    private static final int VIEW_INDEX_HIDDEN = -1;

    private static final int VIEW_INDEX_BCC_EXPANDER_VISIBLE = 0;
    private static final int VIEW_INDEX_BCC_EXPANDER_HIDDEN = 1;

    private static final FastOutLinearInInterpolator CRYPTO_ICON_OUT_ANIMATOR = new FastOutLinearInInterpolator();
    private static final int CRYPTO_ICON_OUT_DURATION = 195;
    private static final LinearOutSlowInInterpolator CRYPTO_ICON_IN_ANIMATOR = new LinearOutSlowInInterpolator();
    private static final int CRYPTO_ICON_IN_DURATION = 225;

    private final MessageCompose activity;
    private final View ccWrapper;
    private final View ccDivider;
    private final View bccWrapper;
    private final View bccDivider;
    private final RecipientSelectView toView;
    private final RecipientSelectView ccView;
    private final RecipientSelectView bccView;
    private final ToolableViewAnimator cryptoStatusView;
    private final ViewAnimator recipientExpanderContainer;
    private final ToolableViewAnimator cryptoSpecialModeIndicator;
    private RecipientPresenter presenter;


    public RecipientMvpView(MessageCompose activity) {
        this.activity = activity;

        toView = (RecipientSelectView) activity.findViewById(R.id.to);
        ccView = (RecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (RecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = activity.findViewById(R.id.cc_wrapper);
        ccDivider = activity.findViewById(R.id.cc_divider);
        bccWrapper = activity.findViewById(R.id.bcc_wrapper);
        bccDivider = activity.findViewById(R.id.bcc_divider);
        recipientExpanderContainer = (ViewAnimator) activity.findViewById(R.id.recipient_expander_container);
        cryptoStatusView = (ToolableViewAnimator) activity.findViewById(R.id.crypto_status);
        cryptoStatusView.setOnClickListener(this);
        cryptoSpecialModeIndicator = (ToolableViewAnimator) activity.findViewById(R.id.crypto_special_mode);
        cryptoSpecialModeIndicator.setOnClickListener(this);

        toView.setOnFocusChangeListener(this);
        ccView.setOnFocusChangeListener(this);
        bccView.setOnFocusChangeListener(this);

        View recipientExpander = activity.findViewById(R.id.recipient_expander);
        recipientExpander.setOnClickListener(this);

        View toLabel = activity.findViewById(R.id.to_label);
        View ccLabel = activity.findViewById(R.id.cc_label);
        View bccLabel = activity.findViewById(R.id.bcc_label);
        toLabel.setOnClickListener(this);
        ccLabel.setOnClickListener(this);
        bccLabel.setOnClickListener(this);
    }

    public void setPresenter(final RecipientPresenter presenter) {
        this.presenter = presenter;

        if (presenter == null) {
            toView.setTokenListener(null);
            ccView.setTokenListener(null);
            bccView.setTokenListener(null);
            return;
        }

        toView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onToTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onToTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onToTokenChanged();
            }
        });

        ccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onCcTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onCcTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onCcTokenChanged();
            }
        });

        bccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onBccTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onBccTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onBccTokenChanged();
            }
        });
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        toView.addTextChangedListener(textWatcher);
        ccView.addTextChangedListener(textWatcher);
        bccView.addTextChangedListener(textWatcher);
    }

    public void setRecipientTokensShowCryptoEnabled(boolean isEnabled) {
        toView.setShowCryptoEnabled(isEnabled);
        ccView.setShowCryptoEnabled(isEnabled);
        bccView.setShowCryptoEnabled(isEnabled);
    }

    public void setCryptoProvider(String openPgpProvider) {
        // TODO move "show advanced" into settings, or somewhere?
        toView.setCryptoProvider(openPgpProvider, false);
        ccView.setCryptoProvider(openPgpProvider, false);
        bccView.setCryptoProvider(openPgpProvider, false);
    }

    public void requestFocusOnToField() {
        toView.requestFocus();
    }

    public void requestFocusOnCcField() {
        ccView.requestFocus();
    }

    public void requestFocusOnBccField() {
        bccView.requestFocus();
    }

    public void setFontSizes(FontSizes fontSizes, int fontSize) {
        fontSizes.setViewTextSize(toView, fontSize);
        fontSizes.setViewTextSize(ccView, fontSize);
        fontSizes.setViewTextSize(bccView, fontSize);
    }

    public void addRecipients(RecipientType recipientType, Recipient... recipients) {
        switch (recipientType) {
            case TO: {
                toView.addRecipients(recipients);
                break;
            }
            case CC: {
                ccView.addRecipients(recipients);
                break;
            }
            case BCC: {
                bccView.addRecipients(recipients);
                break;
            }
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
        int childToDisplay = visible ? VIEW_INDEX_BCC_EXPANDER_VISIBLE : VIEW_INDEX_BCC_EXPANDER_HIDDEN;
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

    public boolean recipientToHasUncompletedText() {
        return toView.hasUncompletedText();
    }

    public boolean recipientCcHasUncompletedText() {
        return ccView.hasUncompletedText();
    }

    public boolean recipientBccHasUncompletedText() {
        return bccView.hasUncompletedText();
    }

    public boolean recipientToTryPerformCompletion() {
        return toView.tryPerformCompletion();
    }

    public boolean recipientCcTryPerformCompletion() {
        return ccView.tryPerformCompletion();
    }

    public boolean recipientBccTryPerformCompletion() {
        return bccView.tryPerformCompletion();
    }

    public void showToUncompletedError() {
        toView.setError(toView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showCcUncompletedError() {
        ccView.setError(ccView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showBccUncompletedError() {
        bccView.setError(bccView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showCryptoSpecialMode(CryptoSpecialModeDisplayType cryptoSpecialModeDisplayType) {
        boolean shouldBeHidden = cryptoSpecialModeDisplayType.childIdToDisplay == VIEW_INDEX_HIDDEN;
        if (shouldBeHidden) {
            cryptoSpecialModeIndicator.setVisibility(View.GONE);
            return;
        }

        cryptoSpecialModeIndicator.setVisibility(View.VISIBLE);
        cryptoSpecialModeIndicator.setDisplayedChildId(cryptoSpecialModeDisplayType.childIdToDisplay);
        activity.invalidateOptionsMenu();
    }

    public void showCryptoStatus(CryptoStatusDisplayType cryptoStatusDisplayType) {
        boolean shouldBeHidden = cryptoStatusDisplayType.childIdToDisplay == VIEW_INDEX_HIDDEN;
        if (shouldBeHidden) {
            cryptoStatusView.animate()
                    .translationXBy(100.0f)
                    .alpha(0.0f)
                    .setDuration(CRYPTO_ICON_OUT_DURATION)
                    .setInterpolator(CRYPTO_ICON_OUT_ANIMATOR)
                    .start();
            return;
        }

        cryptoStatusView.setVisibility(View.VISIBLE);
        cryptoStatusView.setDisplayedChildId(cryptoStatusDisplayType.childIdToDisplay);
        cryptoStatusView.animate()
                .translationX(0.0f)
                .alpha(1.0f)
                .setDuration(CRYPTO_ICON_IN_DURATION)
                .setInterpolator(CRYPTO_ICON_IN_ANIMATOR)
                .start();
    }

    public void showContactPicker(int requestCode) {
        activity.showContactPicker(requestCode);
    }

    public void showErrorIsSignOnly() {
        Toast.makeText(activity, R.string.error_sign_only_no_encryption, Toast.LENGTH_LONG).show();
    }

    public void showErrorContactNoAddress() {
        Toast.makeText(activity, R.string.error_contact_address_not_found, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpRetrieveStatus() {
        Toast.makeText(activity, R.string.error_recipient_crypto_retrieve, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpIncompatible() {
        Toast.makeText(activity, R.string.error_crypto_provider_incompatible, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpConnection() {
        Toast.makeText(activity, R.string.error_crypto_provider_connect, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpUserInteractionRequired() {
        Toast.makeText(activity, R.string.error_crypto_provider_ui_required, Toast.LENGTH_LONG).show();
    }

    public void showErrorMissingSignKey() {
        Toast.makeText(activity, R.string.compose_error_no_signing_key, Toast.LENGTH_LONG).show();
    }

    public void showErrorInlineAttach() {
        Toast.makeText(activity, R.string.error_crypto_inline_attach, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        switch (view.getId()) {
            case R.id.to: {
                presenter.onToFocused();
                break;
            }
            case R.id.cc: {
                presenter.onCcFocused();
                break;
            }
            case R.id.bcc: {
                presenter.onBccFocused();
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.to_label: {
                presenter.onClickToLabel();
                break;
            }
            case R.id.cc_label: {
                presenter.onClickCcLabel();
                break;
            }
            case R.id.bcc_label: {
                presenter.onClickBccLabel();
                break;
            }
            case R.id.recipient_expander: {
                presenter.onClickRecipientExpander();
                break;
            }
            case R.id.crypto_status: {
                presenter.onClickCryptoStatus();
                break;
            }
            case R.id.crypto_special_mode: {
                presenter.onClickCryptoSpecialModeIndicator();
            }
        }
    }

    public void showOpenPgpInlineDialog(boolean firstTime) {
        PgpInlineDialog dialog = PgpInlineDialog.newInstance(firstTime, R.id.crypto_special_mode);
        dialog.show(activity.getFragmentManager(), "openpgp_inline");
    }

    public void showOpenPgpSignOnlyDialog(boolean firstTime) {
        PgpSignOnlyDialog dialog = PgpSignOnlyDialog.newInstance(firstTime, R.id.crypto_special_mode);
        dialog.show(activity.getFragmentManager(), "openpgp_signonly");
    }

    public void showOpenPgpEnabledErrorDialog(final boolean isGotItDialog) {
        PgpEnabledErrorDialog dialog = PgpEnabledErrorDialog.newInstance(isGotItDialog, R.id.crypto_status_anchor);
        dialog.show(activity.getFragmentManager(), "openpgp_error");
    }

    public void showOpenPgpEncryptExplanationDialog() {
        PgpEncryptDescriptionDialog dialog = PgpEncryptDescriptionDialog.newInstance(R.id.crypto_status_anchor);
        dialog.show(activity.getFragmentManager(), "openpgp_description");
    }

    public void launchUserInteractionPendingIntent(PendingIntent pendingIntent, int requestCode) {
        activity.launchUserInteractionPendingIntent(pendingIntent, requestCode);
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        toView.setLoaderManager(loaderManager);
        ccView.setLoaderManager(loaderManager);
        bccView.setLoaderManager(loaderManager);
    }

    public enum CryptoStatusDisplayType {
        UNCONFIGURED(VIEW_INDEX_HIDDEN),
        UNINITIALIZED(VIEW_INDEX_HIDDEN),
        SIGN_ONLY(R.id.crypto_status_disabled),
        NO_CHOICE_EMPTY(VIEW_INDEX_HIDDEN),
        NO_CHOICE_UNAVAILABLE(VIEW_INDEX_HIDDEN),
        NO_CHOICE_AVAILABLE(R.id.crypto_status_disabled),
        NO_CHOICE_AVAILABLE_TRUSTED(R.id.crypto_status_disabled),
        NO_CHOICE_MUTUAL(R.id.crypto_status_enabled),
        NO_CHOICE_MUTUAL_TRUSTED(R.id.crypto_status_trusted),
        CHOICE_ENABLED_UNTRUSTED(R.id.crypto_status_enabled),
        CHOICE_ENABLED_TRUSTED(R.id.crypto_status_trusted),
        CHOICE_ENABLED_ERROR(R.id.crypto_status_error),
        CHOICE_DISABLED_UNTRUSTED(R.id.crypto_status_disabled),
        CHOICE_DISABLED_TRUSTED(R.id.crypto_status_disabled),
        CHOICE_DISABLED_UNAVAILABLE(VIEW_INDEX_HIDDEN),
        ERROR(R.id.crypto_status_error);


        final int childIdToDisplay;

        CryptoStatusDisplayType(int childIdToDisplay) {
            this.childIdToDisplay = childIdToDisplay;
        }
    }

    public enum CryptoSpecialModeDisplayType {
        NONE(VIEW_INDEX_HIDDEN),
        PGP_INLINE(R.id.crypto_special_inline),
        SIGN_ONLY(R.id.crypto_special_sign_only),
        SIGN_ONLY_PGP_INLINE(R.id.crypto_special_sign_only_inline);


        final int childIdToDisplay;

        CryptoSpecialModeDisplayType(int childIdToDisplay) {
            this.childIdToDisplay = childIdToDisplay;
        }
    }
}
