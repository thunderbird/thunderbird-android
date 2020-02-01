package com.fsck.k9.ui.messageview;

import java.util.Objects;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.fsck.k9.ui.R;

import org.jetbrains.annotations.NotNull;

public class MessageSourceViewFragment extends DialogFragment {
    private final static String BUNDLE_ARGUMENT_ID_TEXT = "text";

    public static MessageSourceViewFragment newInstance(String text) {
        Bundle args = new Bundle();
        args.putString(BUNDLE_ARGUMENT_ID_TEXT, text);
        MessageSourceViewFragment fragment = new MessageSourceViewFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_source_view, null);
        TextView source_text = view.findViewById(R.id.source_text);
        source_text.setText(requireArguments().getString(BUNDLE_ARGUMENT_ID_TEXT));

        builder.setView(view);

        builder.setPositiveButton(R.string.ok, null);

        // set layout params to make dialog have maximum width
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        return dialog;
    }
}
