package com.fsck.k9.view;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.fsck.k9.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.ViewTarget;


public class HighlightDialogFragment extends DialogFragment {
    public static final String ARG_HIGHLIGHT_VIEW = "highlighted_view";
    public static final float BACKGROUND_DIM_AMOUNT = 0.25f;


    private ShowcaseView showcaseView;


    protected void highlightViewInBackground() {
        if (!getArguments().containsKey(ARG_HIGHLIGHT_VIEW)) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("fragment must be attached to set highlight!");
        }

        boolean alreadyShowing = showcaseView != null && showcaseView.isShowing();
        if (alreadyShowing) {
            return;
        }

        int highlightedView = getArguments().getInt(ARG_HIGHLIGHT_VIEW);
        showcaseView = new Builder(activity)
                .setTarget(new ViewTarget(highlightedView, activity))
                .hideOnTouchOutside()
                .blockAllTouches()
                .withMaterialShowcase()
                .setStyle(R.style.ShowcaseTheme)
                .build();
        showcaseView.hideButton();
    }

    @Override
    public void onStart() {
        super.onStart();

        hideKeyboard();
        highlightViewInBackground();
        setDialogBackgroundDim();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        hideShowcaseView();
    }

    private void setDialogBackgroundDim() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        dialog.getWindow().setDimAmount(BACKGROUND_DIM_AMOUNT);
    }

    private void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // check if no view has focus
        View v = activity.getCurrentFocus();
        if (v == null) {
            return;
        }

        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void hideShowcaseView() {
        if (showcaseView != null && showcaseView.isShowing()) {
            showcaseView.hide();
        }
        showcaseView = null;
    }
}
