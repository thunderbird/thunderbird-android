package com.fsck.k9.view;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.fragment.app.DialogFragment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.fsck.k9.ui.R;
import com.fsck.k9.ui.compose.SimpleHighlightView;


public class HighlightDialogFragment extends DialogFragment {
    public static final String ARG_HIGHLIGHT_VIEW = "highlighted_view";
    public static final float BACKGROUND_DIM_AMOUNT = 0.25f;


    private SimpleHighlightView highlightView;


    protected void highlightViewInBackground() {
        if (!getArguments().containsKey(ARG_HIGHLIGHT_VIEW)) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("fragment must be attached to set highlight!");
        }

        boolean alreadyShowing = highlightView != null;
        if (alreadyShowing) {
            return;
        }

        int highlightedViewId = getArguments().getInt(ARG_HIGHLIGHT_VIEW);
        View highlightedView = activity.findViewById(highlightedViewId);
        highlightView = SimpleHighlightView.createAndInsert(activity, highlightedView, R.style.MessageComposeHighlight);
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

        hideHighlightView();
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

    private void hideHighlightView() {
        if (highlightView != null) {
            highlightView.remove();
            highlightView = null;
        }
    }
}
