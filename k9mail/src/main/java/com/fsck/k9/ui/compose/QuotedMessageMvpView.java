package com.fsck.k9.ui.compose;


import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.ui.EolConvertingEditText;
import com.fsck.k9.view.MessageWebView;


public class QuotedMessageMvpView {
    private final Button quotedTextShow;
    private final View quotedTextBar;
    private final ImageButton quotedTextEdit;
    private final EolConvertingEditText quotedText;
    private final MessageWebView quotedHTML;
    private final EolConvertingEditText messageContentView;
    private final ImageButton quotedTextDelete;


    public QuotedMessageMvpView(MessageCompose messageCompose) {
        quotedTextShow = (Button) messageCompose.findViewById(R.id.quoted_text_show);
        quotedTextBar = messageCompose.findViewById(R.id.quoted_text_bar);
        quotedTextEdit = (ImageButton) messageCompose.findViewById(R.id.quoted_text_edit);
        quotedTextDelete = (ImageButton) messageCompose.findViewById(R.id.quoted_text_delete);
        quotedText = (EolConvertingEditText) messageCompose.findViewById(R.id.quoted_text);
        quotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        quotedHTML = (MessageWebView) messageCompose.findViewById(R.id.quoted_html);
        quotedHTML.configure();
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        quotedHTML.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        messageContentView = (EolConvertingEditText) messageCompose.findViewById(R.id.message_content);
    }

    public void setOnClickPresenter(final QuotedMessagePresenter presenter) {
        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(view.getId()) {
                    case R.id.quoted_text_show:
                        presenter.onClickShowQuotedText();
                        break;
                    case R.id.quoted_text_delete:
                        presenter.onClickDeleteQuotedText();
                        break;
                    case R.id.quoted_text_edit:
                        presenter.onClickEditQuotedText();
                        break;
                }
            }
        };

        quotedTextShow.setOnClickListener(onClickListener);
        quotedTextEdit.setOnClickListener(onClickListener);
        quotedTextDelete.setOnClickListener(onClickListener);
    }

    public void addTextChangedListener(TextWatcher draftNeedsChangingTextWatcher) {
        quotedText.addTextChangedListener(draftNeedsChangingTextWatcher);
    }

    public void showOrHideQuotedText(QuotedTextMode mode, SimpleMessageFormat quotedTextFormat) {
        switch (mode) {
            case NONE: {
                quotedTextShow.setVisibility(View.GONE);
                quotedTextBar.setVisibility(View.GONE);
                quotedText.setVisibility(View.GONE);
                quotedHTML.setVisibility(View.GONE);
                quotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case HIDE: {
                quotedTextShow.setVisibility(View.VISIBLE);
                quotedTextBar.setVisibility(View.GONE);
                quotedText.setVisibility(View.GONE);
                quotedHTML.setVisibility(View.GONE);
                quotedTextEdit.setVisibility(View.GONE);
                break;
            }
            case SHOW: {
                quotedTextShow.setVisibility(View.GONE);
                quotedTextBar.setVisibility(View.VISIBLE);

                if (quotedTextFormat == SimpleMessageFormat.HTML) {
                    quotedText.setVisibility(View.GONE);
                    quotedHTML.setVisibility(View.VISIBLE);
                    quotedTextEdit.setVisibility(View.VISIBLE);
                } else {
                    quotedText.setVisibility(View.VISIBLE);
                    quotedHTML.setVisibility(View.GONE);
                    quotedTextEdit.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    public void setFontSizes(FontSizes mFontSizes, int fontSize) {
        mFontSizes.setViewTextSize(quotedText, fontSize);
    }

    public void setQuotedHtml(String quotedContent, AttachmentResolver attachmentResolver) {
        quotedHTML.displayHtmlContentWithInlineAttachments(
                HtmlConverter.wrapMessageContent(quotedContent),
                attachmentResolver, null);
    }

    public void setQuotedText(String quotedText) {
        this.quotedText.setCharacters(quotedText);
    }

    // TODO we shouldn't have to retrieve the state from the view here
    public String getQuotedText() {
        return quotedText.getCharacters();
    }

    public void setMessageContentCharacters(String text) {
        messageContentView.setCharacters(text);
    }

    public void setMessageContentCursorPosition(int messageContentCursorPosition) {
        messageContentView.setSelection(messageContentCursorPosition);
    }
}
