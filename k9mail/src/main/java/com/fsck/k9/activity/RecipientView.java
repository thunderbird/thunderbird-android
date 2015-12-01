package com.fsck.k9.activity;


import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;


public class RecipientView {

    private final Activity activity;

    private final LinearLayout ccWrapper;
    private final LinearLayout bccWrapper;
    private final RecipientSelectView toView;
    private final RecipientSelectView ccView;
    private final RecipientSelectView bccView;

    public RecipientView(Activity activity) {

        this.activity = activity;

        toView = (RecipientSelectView) activity.findViewById(R.id.to);
        ccView = (RecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (RecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = (LinearLayout) activity.findViewById(R.id.cc_wrapper);
        bccWrapper = (LinearLayout) activity.findViewById(R.id.bcc_wrapper);

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

    public void addToAddresses(Address... addresses) {
        toView.addAddress(addresses);
    }

    public void addCcAddresses(Address... addresses) {
        ccView.addAddress(addresses);
    }

    public void addBccAddresses(Address... addresses) {
        bccView.addAddress(addresses);
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

    public void setToError(@StringRes int message_compose_error_no_recipients) {
        toView.setError(toView.getContext().getString(message_compose_error_no_recipients));
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

}