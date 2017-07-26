package com.fsck.k9.activity.setup.names;


import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.names.NamesContract.Presenter;


public class NamesView implements NamesContract.View, OnClickListener {
    Presenter presenter;
    AbstractAccountSetup activity;
    private EditText description;
    private EditText name;
    private Button doneButton;

    public NamesView(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void start() {
        description = (EditText) activity.findViewById(R.id.account_description);
        name = (EditText) activity.findViewById(R.id.account_name);
        doneButton = (Button) activity.findViewById(R.id.done);
        doneButton.setOnClickListener(this);

        presenter = new NamesPresenter(this, activity.getState());
    }

    @Override
    public void onClick(View v) {
        presenter.onNextButtonClicked(name.getText().toString(), description.getText().toString());
    }

    @Override
    public void onSetupFinished() {
        activity.listAccounts();
    }
}
