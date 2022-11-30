package com.fsck.k9.view;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.FontSizes;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPicture;
import com.fsck.k9.contacts.ContactPictureLoader;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.ReplyAction;
import com.fsck.k9.message.ReplyActionStrategy;
import com.fsck.k9.message.ReplyActions;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter;
import com.fsck.k9.ui.messageview.MessageHeaderOnMenuItemClickListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;


public class MessageHeader extends LinearLayout implements OnClickListener, OnLongClickListener {
    private static final int DEFAULT_SUBJECT_LINES = 3;

    private final ReplyActionStrategy replyActionStrategy = DI.get(ReplyActionStrategy.class);
    private final FontSizes fontSizes = K9.getFontSizes();

    private Chip accountChip;
    private TextView subjectView;
    private ImageView starView;
    private ImageView contactPictureView;
    private TextView fromView;
    private ImageView cryptoStatusIcon;
    private TextView toView;
    private TextView dateView;
    private ImageView menuPrimaryActionView;

    private MessageHelper messageHelper;
    private RelativeDateTimeFormatter relativeDateTimeFormatter;
    private Contacts contacts;

    private MessageHeaderOnMenuItemClickListener onMenuItemClickListener;
    private ReplyActions replyActions;


    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            messageHelper = MessageHelper.getInstance(getContext());
            relativeDateTimeFormatter = DI.get(RelativeDateTimeFormatter.class);
            contacts = Contacts.getInstance(context);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        accountChip = findViewById(R.id.chip);
        subjectView = findViewById(R.id.subject);
        starView = findViewById(R.id.flagged);
        contactPictureView = findViewById(R.id.contact_picture);
        fromView = findViewById(R.id.from);
        cryptoStatusIcon = findViewById(R.id.crypto_status_icon);
        toView = findViewById(R.id.to);
        dateView = findViewById(R.id.date);

        fontSizes.setViewTextSize(subjectView, fontSizes.getMessageViewSubject());
        fontSizes.setViewTextSize(dateView, fontSizes.getMessageViewDate());
        fontSizes.setViewTextSize(fromView, fontSizes.getMessageViewSender());
        fontSizes.setViewTextSize(toView, fontSizes.getMessageViewRecipients());

        subjectView.setOnClickListener(this);
        subjectView.setOnLongClickListener(this);

        menuPrimaryActionView = findViewById(R.id.menu_primary_action);
        menuPrimaryActionView.setOnClickListener(this);

        View menuOverflowView = findViewById(R.id.menu_overflow);
        menuOverflowView.setOnClickListener(this);
        String menuOverflowDescription =
                getContext().getString(androidx.appcompat.R.string.abc_action_menu_overflow_description);
        TooltipCompat.setTooltipText(menuOverflowView, menuOverflowDescription);

        findViewById(R.id.participants_container).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.subject) {
            toggleSubjectViewMaxLines();
        } else if (id == R.id.menu_primary_action) {
            performPrimaryReplyAction();
        } else if (id == R.id.menu_overflow) {
            showOverflowMenu(view);
        } else if (id == R.id.participants_container) {
            Snackbar.make(getRootView(), "TODO: Display details popup", Snackbar.LENGTH_LONG).show();
        }
    }

    private void performPrimaryReplyAction() {
        ReplyAction defaultAction = replyActions.getDefaultAction();
        if (defaultAction == null) {
            return;
        }

        switch (defaultAction) {
            case REPLY: {
                onMenuItemClickListener.onMenuItemClick(R.id.reply);
                break;
            }
            case REPLY_ALL: {
                onMenuItemClickListener.onMenuItemClick(R.id.reply_all);
                break;
            }
            default: {
                throw new IllegalStateException("Unknown reply action: " + defaultAction);
            }
        }
    }

    private void showOverflowMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.setOnMenuItemClickListener(item -> {
            onMenuItemClickListener.onMenuItemClick(item.getItemId());
            return true;
        });
        popupMenu.inflate(R.menu.single_message_options);
        setAdditionalReplyActions(popupMenu);
        popupMenu.show();
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        if (id == R.id.subject) {
            onAddSubjectToClipboard(subjectView.getText().toString());
        }

        return true;
    }

    private void toggleSubjectViewMaxLines() {
        if (subjectView.getMaxLines() == DEFAULT_SUBJECT_LINES) {
            subjectView.setMaxLines(Integer.MAX_VALUE);
        } else {
            subjectView.setMaxLines(DEFAULT_SUBJECT_LINES);
        }
    }

    private void onAddSubjectToClipboard(String subject) {
        ClipboardManager clipboardManager = DI.get(ClipboardManager.class);
        clipboardManager.setText("subject", subject);

        Toast.makeText(getContext(), createMessageForSubject(), Toast.LENGTH_LONG).show();
    }

    public String createMessageForSubject() {
        return getResources().getString(R.string.copy_subject_to_clipboard);
    }

    public void setOnFlagListener(OnClickListener listener) {
        starView.setOnClickListener(listener);
    }

    public void populate(final Message message, final Account account, boolean showStar, boolean showAccountChip) {
        if (showAccountChip) {
            accountChip.setVisibility(View.VISIBLE);
            accountChip.setText(account.getDisplayName());
            accountChip.setChipBackgroundColor(ColorStateList.valueOf(account.getChipColor()));
        } else {
            accountChip.setVisibility(View.GONE);
        }

        Address fromAddress = null;
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses.length > 0) {
            fromAddress = fromAddresses[0];
        }

        if (K9.isShowContactPicture()) {
            contactPictureView.setVisibility(View.VISIBLE);
            if (fromAddress != null) {
                ContactPictureLoader contactsPictureLoader = ContactPicture.getContactPictureLoader();
                contactsPictureLoader.setContactPicture(contactPictureView, fromAddress);
            } else {
                contactPictureView.setImageResource(R.drawable.ic_contact_picture);
            }
        } else {
            contactPictureView.setVisibility(View.GONE);
        }

        CharSequence from = messageHelper.getSenderDisplayName(fromAddress);
        fromView.setText(from);

        if (showStar) {
            starView.setVisibility(View.VISIBLE);
            starView.setSelected(message.isSet(Flag.FLAGGED));
        } else {
            starView.setVisibility(View.GONE);
        }

        if (message.getSentDate() != null) {
            dateView.setText(relativeDateTimeFormatter.formatDate(message.getSentDate().getTime()));
        } else {
            dateView.setText("");
        }

        toView.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom,
                int leftWas, int topWas, int rightWas, int bottomWas) -> showRecipients(message, account));
        setReplyActions(message, account);

        setVisibility(View.VISIBLE);
    }

    private void showRecipients(Message message, Account account) {
        ArrayList<Address> recipients = new ArrayList<>();
        recipients.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
        recipients.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
        recipients.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));

        boolean sentToMe = false;
        for (Identity identity : account.getIdentities()) {
            if (removeAddress(recipients, identity.getEmail())) {
                sentToMe = true;
            }
        }

        if (recipients.size() <= 1) {
            // If there is exactly one recipient, always show it (truncated by Android).
            toView.setText(concatRecipients(recipients, recipients.size(), sentToMe, contacts));
            return;
        }

        final Contacts contacts = K9.isShowContactName() ? this.contacts : null;
        int recipientsToShow = 0;
        for (; recipientsToShow <= recipients.size(); recipientsToShow++) {
            CharSequence text = concatRecipients(recipients, recipientsToShow, sentToMe, contacts);
            float length = toView.getPaint().measureText(text, 0, text.length());
            if (length >= toView.getWidth()) {
                break;
            }
        }
        recipientsToShow--; // Either it is one too much (too wide) or one too much (out of bounds in for loop)
        if (recipientsToShow < 0) {
            recipientsToShow = 0;
        }
        toView.setText(concatRecipients(recipients, recipientsToShow, sentToMe, contacts));
    }

    private CharSequence concatRecipients(ArrayList<Address> recipients,
            int numOfRecipients, boolean sentToMe, Contacts contacts) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append(getContext().getString(R.string.message_to_label));
        stringBuilder.append(" ");
        if (sentToMe) {
            stringBuilder.append(getContext().getString(R.string.message_to_me_label));
            if (numOfRecipients > 0) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append(MessageHelper.toFriendly(
                recipients.subList(0, numOfRecipients).toArray(new Address[0]), contacts));
        if (numOfRecipients < recipients.size()) {
            if (numOfRecipients > 0) {
                stringBuilder.append(",");
            }
            int plusOneLengthBefore = stringBuilder.length();
            stringBuilder.append(String.format(Locale.getDefault(), " +%d", recipients.size() - numOfRecipients));
            stringBuilder.setSpan(new ForegroundColorSpan(
                    ThemeUtils.getStyledColor(getContext(), android.R.attr.colorPrimary)),
                    plusOneLengthBefore, stringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return stringBuilder;
    }

    private boolean removeAddress(ArrayList<Address> list, String searchedAddress) {
        for (Iterator<Address> iterator = list.iterator(); iterator.hasNext(); ) {
            Address a = iterator.next();
            if (a.getAddress().equals(searchedAddress)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private void setReplyActions(Message message, Account account) {
        ReplyActions replyActions = replyActionStrategy.getReplyActions(account, message);
        this.replyActions = replyActions;

        setDefaultReplyAction(replyActions.getDefaultAction());
    }

    private void setDefaultReplyAction(ReplyAction defaultAction) {
        if (defaultAction == null) {
            menuPrimaryActionView.setVisibility(View.GONE);
        } else {
            int replyIconResource = getReplyImageResource(defaultAction);
            menuPrimaryActionView.setImageResource(replyIconResource);

            String replyActionName = getReplyActionName(defaultAction);
            TooltipCompat.setTooltipText(menuPrimaryActionView, replyActionName);

            menuPrimaryActionView.setVisibility(View.VISIBLE);
        }
    }

    @DrawableRes
    private int getReplyImageResource(@NonNull ReplyAction replyAction) {
        switch (replyAction) {
            case REPLY: {
                return R.drawable.ic_reply;
            }
            case REPLY_ALL: {
                return R.drawable.ic_reply_all;
            }
            default: {
                throw new IllegalStateException("Unknown reply action: " + replyAction);
            }
        }
    }

    @NonNull
    private String getReplyActionName(@NonNull ReplyAction replyAction) {
        Context context = getContext();
        switch (replyAction) {
            case REPLY: {
                return context.getString(R.string.reply_action);
            }
            case REPLY_ALL: {
                return context.getString(R.string.reply_all_action);
            }
            default: {
                throw new IllegalStateException("Unknown reply action: " + replyAction);
            }
        }
    }

    private void setAdditionalReplyActions(PopupMenu popupMenu) {
        List<ReplyAction> additionalActions = replyActions.getAdditionalActions();
        if (!additionalActions.contains(ReplyAction.REPLY)) {
            popupMenu.getMenu().removeItem(R.id.reply);
        }
        if (!additionalActions.contains(ReplyAction.REPLY_ALL)) {
            popupMenu.getMenu().removeItem(R.id.reply_all);
        }
    }

    public void setSubject(@NonNull String subject) {
        subjectView.setText(subject);
    }

    public void hideCryptoStatus() {
        cryptoStatusIcon.setVisibility(View.GONE);
    }

    public void setCryptoStatusLoading() {
        setCryptoDisplayStatus(MessageCryptoDisplayStatus.LOADING);
    }

    public void setCryptoStatusDisabled() {
        setCryptoDisplayStatus(MessageCryptoDisplayStatus.DISABLED);
    }

    public void setCryptoStatus(MessageCryptoDisplayStatus displayStatus) {
        setCryptoDisplayStatus(displayStatus);
    }

    private void setCryptoDisplayStatus(MessageCryptoDisplayStatus displayStatus) {
        int color = ThemeUtils.getStyledColor(getContext(), displayStatus.getColorAttr());
        cryptoStatusIcon.setEnabled(displayStatus.isEnabled());
        cryptoStatusIcon.setVisibility(View.VISIBLE);
        cryptoStatusIcon.setImageResource(displayStatus.getStatusIconRes());
        cryptoStatusIcon.setColorFilter(color);
    }

    public void setOnMenuItemClickListener(MessageHeaderOnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
    }
}
