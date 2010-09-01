package com.fsck.k9.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.CryptoProvider;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;
import com.fsck.k9.mail.store.LocalStore.LocalTextBody;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.web.AccessibleWebView;

public class MessageView extends K9Activity implements OnClickListener
{
    private static final String EXTRA_MESSAGE_REFERENCE = "com.fsck.k9.MessageView_messageReference";
    private static final String EXTRA_MESSAGE_REFERENCES = "com.fsck.k9.MessageView_messageReferences";
    private static final String EXTRA_NEXT = "com.fsck.k9.MessageView_next";

    private static final String SHOW_PICTURES = "showPictures";
    private static final String STATE_PGP_DATA = "pgpData";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;

    private TextView mFromView;
    private TextView mDateView;
    private TextView mTimeView;
    private TextView mToView;
    private TextView mCcView;
    private TextView mSubjectView;
    public View chip;
    private CheckBox mFlagged;
    private int defaultSubjectColor;
    private View mDecryptLayout;
    private Button mDecryptButton;
    private LinearLayout mCryptoSignatureLayout = null;
    private ImageView mCryptoSignatureStatusImage = null;
    private TextView mCryptoSignatureUserId = null;
    private TextView mCryptoSignatureUserIdRest = null;
    private WebView mMessageContentView;

    private boolean mScreenReaderEnabled;

    private AccessibleWebView mAccessibleMessageContentView;

    private LinearLayout mHeaderContainer;
    private LinearLayout mAttachments;
    private LinearLayout mToContainerView;
    private LinearLayout mCcContainerView;
    private TextView mAdditionalHeadersView;
    private View mAttachmentIcon;
    private View mShowPicturesSection;
    private boolean mShowPictures;

    private Button mDownloadRemainder;


    View next;
    View next_scrolling;
    View previous;
    View previous_scrolling;

    private View mDelete;
    private View mArchive;
    private View mMove;
    private View mSpam;
    private View mArchiveScrolling;
    private View mMoveScrolling;
    private View mSpamScrolling;
    private ToggleScrollView mToggleScrollView;

    private Account mAccount;
    private MessageReference mMessageReference;
    private ArrayList<MessageReference> mMessageReferences;

    private Message mMessage;
    private PgpData mPgpData = null;

    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;

    private int mLastDirection = PREVIOUS;

    private MessageReference mNextMessage = null;
    private MessageReference mPreviousMessage = null;

    private Menu optionsMenu = null;

    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();

    private FontSizes mFontSizes = K9.getFontSizes();

    private Contacts mContacts;

    /**
     * Pair class is only available since API Level 5, so we need
     * this helper class unfortunately
     */
    private class HeaderEntry
    {
        public String label;
        public String value;

        public HeaderEntry(String label, String value)
        {
            this.label = label;
            this.value = value;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (ev.getAction() == MotionEvent.ACTION_UP)
        {
            // Text selection is finished. Allow scrolling again.
            mToggleScrollView.setScrolling(true);
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        boolean ret = false;

        if (KeyEvent.ACTION_DOWN == event.getAction())
        {
            ret = onKeyDown(event.getKeyCode(), event);
        }
        if (!ret)
        {
            ret = super.dispatchKeyEvent(event);
        }
        return ret;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                if (K9.useVolumeKeysForNavigationEnabled())
                {
                    onNext(true);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                if (K9.useVolumeKeysForNavigationEnabled())
                {
                    onPrevious(true);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            {
                /*
                 * Selecting text started via shift key. Disable scrolling as
                 * this causes problems when selecting text.
                 */
                mToggleScrollView.setScrolling(false);
                break;
            }
            case KeyEvent.KEYCODE_DEL:
            {
                onDelete();
                return true;
            }
            case KeyEvent.KEYCODE_D:
            {
                onDelete();
                return true;
            }
            case KeyEvent.KEYCODE_F:
            {
                onForward();
                return true;
            }
            case KeyEvent.KEYCODE_A:
            {
                onReplyAll();
                return true;
            }
            case KeyEvent.KEYCODE_R:
            {
                onReply();
                return true;
            }
            case KeyEvent.KEYCODE_G:
            {
                onFlag();
                return true;
            }

            case KeyEvent.KEYCODE_M:
            {
                onMove();
                return true;
            }
            case KeyEvent.KEYCODE_S:
            {
                onSpam();
                return true;
            }
            case KeyEvent.KEYCODE_V:
            {
                onArchive();
                return true;
            }
            case KeyEvent.KEYCODE_Y:
            {
                onCopy();
                return true;
            }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P:
            {
                onPrevious(K9.showAnimations());
                return true;
            }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K:
            {
                onNext(K9.showAnimations());
                return true;
            }
            case KeyEvent.KEYCODE_Z:
            {
                if (event.isShiftPressed())
                {
                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            if (mScreenReaderEnabled)
                            {
                                mAccessibleMessageContentView.zoomIn();
                            }
                            else
                            {
                                mMessageContentView.zoomIn();
                            }
                        }
                    });
                }
                else
                {
                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            if (mScreenReaderEnabled)
                            {
                                mAccessibleMessageContentView.zoomIn();
                            }
                            else
                            {
                                mMessageContentView.zoomOut();
                            }
                        }
                    });
                }
                return true;
            }
            case KeyEvent.KEYCODE_H:
            {
                Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForNavigationEnabled())
        {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode,event);
    }

    class MessageViewHandler extends Handler
    {
        public void progress(final boolean progress)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(progress);

                }
            });
        }

        public void addAttachment(final View attachmentView)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mAttachments.addView(attachmentView);
                    mAttachments.setVisibility(View.VISIBLE);

                }
            });
        }

        public void removeAllAttachments()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
                    {
                        mAttachments.removeView(mAttachments.getChildAt(i));
                    }
                }

            });
        }


        public void setAttachmentsEnabled(final boolean enabled)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++)
                    {
                        Attachment attachment = (Attachment) mAttachments.getChildAt(i).getTag();
                        attachment.viewButton.setEnabled(enabled);
                        attachment.downloadButton.setEnabled(enabled);
                    }

                }
            });
        }

        public void setHeaders(
            final   String subject,
            final   CharSequence from,
            final   String date,
            final   String time,
            final   CharSequence to,
            final   CharSequence cc,
            final   int accountColor,
            final   boolean unread,
            final   boolean hasAttachments,
            final   boolean flagged,
            final   boolean answered)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    setTitle(subject);
                    if (subject == null || subject.equals(""))
                    {
                        mSubjectView.setText(getText(R.string.general_no_subject));
                    }
                    else
                    {
                        mSubjectView.setText(subject);
                    }
                    mFromView.setText(from);
                    if (date != null)
                    {
                        mDateView.setText(date);
                        mDateView.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        mDateView.setVisibility(View.GONE);
                    }
                    mTimeView.setText(time);
                    mToContainerView.setVisibility((to != null && to.length() > 0)? View.VISIBLE : View.GONE);
                    mToView.setText(to);

                    mCcContainerView.setVisibility((cc != null && cc.length() > 0)? View.VISIBLE : View.GONE);

                    mCcView.setText(cc);
                    mAttachmentIcon.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);
                    if (flagged)
                    {
                        mFlagged.setChecked(true);
                    }
                    else
                    {
                        mFlagged.setChecked(false);
                    }
                    mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);

                    chip.setBackgroundColor(accountColor);
                    chip.getBackground().setAlpha(unread ? 255 : 127);

                    if (answered)
                    {
                        Drawable answeredIcon = getResources().getDrawable(
                                                    R.drawable.ic_mms_answered_small);
                        mSubjectView.setCompoundDrawablesWithIntrinsicBounds(
                            answeredIcon, // left
                            null, // top
                            null, // right
                            null); // bottom
                    }
                    else
                    {
                        mSubjectView.setCompoundDrawablesWithIntrinsicBounds(
                            null, // left
                            null, // top
                            null, // right
                            null); // bottom
                    }

                    if (mMessage.isSet(Flag.X_DOWNLOADED_FULL))
                    {
                        mDownloadRemainder.setVisibility(View.GONE);
                    }
                    else
                    {
                        mDownloadRemainder.setEnabled(true);
                        mDownloadRemainder.setVisibility(View.VISIBLE);

                    }

                }
            });
        }



        public void networkError()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   R.string.status_network_error, Toast.LENGTH_LONG).show();

                }
            });
        }

        public void invalidIdError()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   R.string.status_invalid_id_error, Toast.LENGTH_LONG).show();

                }
            });
        }

        public void attachmentSaved(final String filename)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this, String.format(
                                       getString(R.string.message_view_status_attachment_saved), filename),
                                   Toast.LENGTH_LONG).show();


                }
            });
        }

        public void attachmentNotSaved()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {

                    Toast.makeText(MessageView.this,
                                   getString(R.string.message_view_status_attachment_not_saved),
                                   Toast.LENGTH_LONG).show();

                }
            });
        }

        public void fetchingAttachment()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(MessageView.this,
                                   getString(R.string.message_view_fetching_attachment_toast),
                                   Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void showShowPictures(final boolean show)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mShowPicturesSection.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

        }



        private void showHeaderContainer()
        {
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        mHeaderContainer.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        private void hideHeaderContainer()
        {
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        mHeaderContainer.setVisibility(View.GONE);
                    }
                });
            }
        }




        /**
         * Clear the text field for the additional headers display if they are
         * not shown, to save UI resources.
         */
        public void hideAdditionalHeaders()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mAdditionalHeadersView.setVisibility(View.GONE);
                    mAdditionalHeadersView.setText("");
                    mTopView.scrollTo(0, 0);
                }
            });
        }

        /**
         * Set up and then show the additional headers view. Called by
         * {@link #onShowAdditionalHeaders()} and
         * {@link #setHeaders(Account, String, String, Message)}
         * (when switching between messages).
         */
        public void showAdditionalHeaders()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Integer messageToShow = null;
                    try
                    {
                        // Retrieve additional headers
                        boolean allHeadersDownloaded = mMessage.isSet(Flag.X_GOT_ALL_HEADERS);
                        List<HeaderEntry> additionalHeaders = getAdditionalHeaders(mMessage);

                        if (!additionalHeaders.isEmpty())
                        {
                            // Show the additional headers that we have got.
                            setupAdditionalHeadersView(additionalHeaders);
                            mAdditionalHeadersView.setVisibility(View.VISIBLE);
                        }

                        if (!allHeadersDownloaded)
                        {
                            /*
                             * Tell the user about the "save all headers" setting
                             *
                             * NOTE: This is only a temporary solution... in fact,
                             * the system should download headers on-demand when they
                             * have not been saved in their entirety initially.
                             */
                            messageToShow = R.string.message_additional_headers_not_downloaded;
                        }
                        else if (additionalHeaders.isEmpty())
                        {
                            // All headers have been downloaded, but there are no additional headers.
                            messageToShow = R.string.message_no_additional_headers_available;
                        }
                    }
                    catch (MessagingException e)
                    {
                        messageToShow = R.string.message_additional_headers_retrieval_failed;
                    }

                    // Show a message to the user, if any
                    if (messageToShow != null)
                    {
                        Toast toast = Toast.makeText(MessageView.this, messageToShow, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                    }
                }
            });
        }

        /**
         * Set up the additional headers text view with the supplied header data.
         *
         * @param additionalHeaders
         *          List of header entries. Each entry consists of a header
         *          name and a header value. Header names may appear multiple
         *          times.
         *
         * This method is always called from within the UI thread by
         * {@link #showAdditionalHeaders()}.
         */
        private void setupAdditionalHeadersView(final List<HeaderEntry> additionalHeaders)
        {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            boolean first = true;
            for (HeaderEntry additionalHeader : additionalHeaders)
            {
                if (!first)
                {
                    sb.append("\n");
                }
                else
                {
                    first = false;
                }

                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                SpannableString label = new SpannableString(additionalHeader.label + ": ");
                label.setSpan(boldSpan, 0, label.length(), 0);

                sb.append(label);
                sb.append(MimeUtility.unfoldAndDecode(additionalHeader.value));
            }

            mAdditionalHeadersView.setText(sb);
        }
    }

    class Attachment
    {
        public String name;
        public String contentType;
        public long size;
        public LocalAttachmentBodyPart part;
        public Button viewButton;
        public Button downloadButton;
        public ImageView iconView;
    }

    public static void actionView(Context context, MessageReference messRef, List<MessageReference> messReferences)
    {
        actionView(context, messRef, messReferences, null);
    }

    public static void actionView(Context context, MessageReference messRef, List<MessageReference> messReferences, Bundle extras)
    {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messRef);
        i.putExtra(EXTRA_MESSAGE_REFERENCES, (Serializable)messReferences);
        if (extras != null)
        {
            i.putExtras(extras);
        }
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle, false);

        mContacts = Contacts.getInstance(this);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.message_view);

        mHeaderContainer = (LinearLayout)findViewById(R.id.header_container);

        mFromView = (TextView)findViewById(R.id.from);
        mToView = (TextView)findViewById(R.id.to);
        mCcView = (TextView)findViewById(R.id.cc);
        mToContainerView = (LinearLayout)findViewById(R.id.to_container);
        mCcContainerView = (LinearLayout)findViewById(R.id.cc_container);
        mSubjectView = (TextView)findViewById(R.id.subject);
        defaultSubjectColor = mSubjectView.getCurrentTextColor();

        mAdditionalHeadersView = (TextView)findViewById(R.id.additional_headers_view);

        chip = findViewById(R.id.chip);

        mDateView = (TextView)findViewById(R.id.date);
        mTimeView = (TextView)findViewById(R.id.time);
        mTopView = mToggleScrollView = (ToggleScrollView)findViewById(R.id.top_view);
        mMessageContentView = (WebView)findViewById(R.id.message_content);
        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);

        mScreenReaderEnabled = isScreenReaderActive();

        if (mScreenReaderEnabled)
        {
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
        }
        else
        {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
        }

        mDecryptLayout = (View)findViewById(R.id.layout_decrypt);
        mDecryptButton = (Button)findViewById(R.id.btn_decrypt);
        mDecryptButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    String data = null;
                    Part part = MimeUtility.findFirstPartByMimeType(mMessage, "text/plain");
                    if (part == null)
                    {
                        part = MimeUtility.findFirstPartByMimeType(mMessage, "text/html");
                    }
                    if (part != null)
                    {
                        data = MimeUtility.getTextFromPart(part);
                    }
                    mAccount.getCryptoProvider().decrypt(MessageView.this, data, mPgpData);
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Unable to decrypt email.", me);
                }
            }
        });
        mCryptoSignatureLayout = (LinearLayout) findViewById(R.id.crypto_signature);
        mCryptoSignatureStatusImage = (ImageView) findViewById(R.id.ic_crypto_signature_status);
        mCryptoSignatureUserId = (TextView) findViewById(R.id.userId);
        mCryptoSignatureUserIdRest = (TextView) findViewById(R.id.userIdRest);
        mCryptoSignatureLayout.setVisibility(View.INVISIBLE);

        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mAttachmentIcon = findViewById(R.id.attachment);
        mShowPicturesSection = findViewById(R.id.show_pictures_section);
        mShowPictures = false;

        mDownloadRemainder = (Button)findViewById(R.id.download_remainder);

        mFlagged = (CheckBox)findViewById(R.id.flagged);
        mFlagged.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                onFlag();
            }
        });

        mMessageContentView.setVerticalScrollBarEnabled(true);
        mMessageContentView.setVerticalScrollbarOverlay(true);
        mMessageContentView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        final WebSettings webSettings = mMessageContentView.getSettings();

        webSettings.setSupportZoom(true);
        webSettings.setLoadsImagesAutomatically(true);
        //webSettings.setBuiltInZoomControls(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

        webSettings.setTextSize(mFontSizes.getMessageViewContent());

        mFromView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewSender());
        mToView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTo());
        ((TextView)findViewById(R.id.to_label)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTo());
        mCcView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewCC());
        ((TextView)findViewById(R.id.cc_label)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewCC());
        mSubjectView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewSubject());
        mTimeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewTime());
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewDate());
        mAdditionalHeadersView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mFontSizes.getMessageViewAdditionalHeaders());
        mAdditionalHeadersView.setVisibility(View.GONE);
        mAttachments.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);

        setOnClickListener(R.id.from);
        setOnClickListener(R.id.reply);
        setOnClickListener(R.id.reply_all);
        setOnClickListener(R.id.delete);
        setOnClickListener(R.id.forward);
        setOnClickListener(R.id.next);
        setOnClickListener(R.id.previous);
        setOnClickListener(R.id.archive);
        setOnClickListener(R.id.move);
        setOnClickListener(R.id.spam);

        // To show full header
        setOnClickListener(R.id.header_container);

        setOnClickListener(R.id.reply_scrolling);
//       setOnClickListener(R.id.reply_all_scrolling);
        setOnClickListener(R.id.delete_scrolling);
        setOnClickListener(R.id.forward_scrolling);
        setOnClickListener(R.id.next_scrolling);
        setOnClickListener(R.id.previous_scrolling);
        setOnClickListener(R.id.archive_scrolling);
        setOnClickListener(R.id.move_scrolling);
        setOnClickListener(R.id.spam_scrolling);

        setOnClickListener(R.id.show_pictures);

        setOnClickListener(R.id.download_remainder);

        setTitle("");

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (icicle != null)
        {
            mMessageReference = (MessageReference)icicle.getSerializable(EXTRA_MESSAGE_REFERENCE);
            mMessageReferences = (ArrayList<MessageReference>)icicle.getSerializable(EXTRA_MESSAGE_REFERENCES);

            mPgpData = (PgpData) icicle.getSerializable(STATE_PGP_DATA);
            updateDecryptLayout();
        }
        else
        {
            if (uri == null)
            {
                mMessageReference = (MessageReference)intent.getSerializableExtra(EXTRA_MESSAGE_REFERENCE);
                mMessageReferences = (ArrayList<MessageReference>)intent.getSerializableExtra(EXTRA_MESSAGE_REFERENCES);
            }
            else
            {
                List<String> segmentList = uri.getPathSegments();
                if (segmentList.size() == 3)
                {
                    String accountId = segmentList.get(0);
                    Account[] accounts = Preferences.getPreferences(this).getAccounts();
                    boolean found = false;
                    for (Account account : accounts)
                    {
                        if (String.valueOf(account.getAccountNumber()).equals(accountId))
                        {
                            mAccount = account;
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        //TODO: Use ressource to externalize message
                        Toast.makeText(this, "Invalid account id: " + accountId, Toast.LENGTH_LONG).show();
                        return;
                    }

                    mMessageReference = new MessageReference();
                    mMessageReference.accountUuid = mAccount.getUuid();
                    mMessageReference.folderName = segmentList.get(1);
                    mMessageReference.uid = segmentList.get(2);

                    mMessageReferences = new ArrayList<MessageReference>();
                }
                else
                {
                    //TODO: Use ressource to externalize message
                    Toast.makeText(this, "Invalid intent uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView got message " + mMessageReference);

        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);

        next_scrolling = findViewById(R.id.next_scrolling);
        previous_scrolling = findViewById(R.id.previous_scrolling);

        mDelete = findViewById(R.id.delete);

        mArchive = findViewById(R.id.archive);
        mMove = findViewById(R.id.move);
        mSpam = findViewById(R.id.spam);

        mArchiveScrolling = findViewById(R.id.archive_scrolling);
        mMoveScrolling = findViewById(R.id.move_scrolling);
        mSpamScrolling = findViewById(R.id.spam_scrolling);

        boolean goNext = intent.getBooleanExtra(EXTRA_NEXT, false);
        if (goNext)
        {
            next.requestFocus();
        }
        // Perhaps the hideButtons should be global, instead of account-specific
        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        Account.HideButtons hideButtons = mAccount.getHideMessageViewButtons();

        //MessagingController.getInstance(getApplication()).addListener(mListener);
        if (Account.HideButtons.ALWAYS == hideButtons)
        {
            hideButtons();
        }
        else if (Account.HideButtons.NEVER == hideButtons)
        {
            showButtons();
        }
        else   // Account.HideButtons.KEYBOARD_AVAIL
        {
            final Configuration config = this.getResources().getConfiguration();
            if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            {
                hideButtons();
            }
            else
            {
                showButtons();
            }
        }

        Account.HideButtons hideMoveButtons = mAccount.getHideMessageViewMoveButtons();
        if (Account.HideButtons.ALWAYS == hideMoveButtons)
        {
            hideMoveButtons();
        }
        else if (Account.HideButtons.NEVER == hideMoveButtons)
        {
            showMoveButtons();
        }
        else   // Account.HideButtons.KEYBOARD_AVAIL
        {
            final Configuration config = this.getResources().getConfiguration();
            if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            {
                hideMoveButtons();
            }
            else
            {
                showMoveButtons();
            }
        }

        if (!mAccount.getEnableMoveButtons())
        {
            View buttons = findViewById(R.id.move_buttons);
            if (buttons != null)
            {
                buttons.setVisibility(View.GONE);
            }
            buttons = findViewById(R.id.scrolling_move_buttons);
            if (buttons != null)
            {
                buttons.setVisibility(View.GONE);
            }
        }

        displayMessage(mMessageReference);
    }

    private boolean isScreenReaderActive()
    {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders)
        {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            if (cursor != null)
            {
                cursor.moveToFirst();
                // These content providers use a special cursor that only has
                // one element,
                // an integer that is 1 if the screen reader is running.
                status = cursor.getInt(0);
                cursor.close();
                if (status == 1)
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(EXTRA_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(EXTRA_MESSAGE_REFERENCES, mMessageReferences);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
        outState.putBoolean(SHOW_PICTURES, mShowPictures);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        mShowPictures = savedInstanceState.getBoolean(SHOW_PICTURES);
        setLoadPictures(mShowPictures);

        mPgpData = (PgpData) savedInstanceState.getSerializable(STATE_PGP_DATA);
        initializeCrypto();

        updateDecryptLayout();
    }

    private void displayMessage(MessageReference ref)
    {
        mMessageReference = ref;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);

        mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        mTopView.setVisibility(View.GONE);
        mTopView.scrollTo(0, 0);
        mMessageContentView.scrollTo(0, 0);

        mHandler.hideHeaderContainer();
        mMessageContentView.clearView();
        setLoadPictures(false);
        mAttachments.removeAllViews();
        findSurroundingMessagesUid();

        // start with fresh, empty PGP data
        mPgpData = null;
        initializeCrypto();

        mTopView.setVisibility(View.VISIBLE);
        MessagingController.getInstance(getApplication()).loadMessageForView(
            mAccount,
            mMessageReference.folderName,
            mMessageReference.uid,
            mListener);
        setupDisplayMessageButtons();
    }

    private void setupDisplayMessageButtons()
    {

        boolean enableNext = (mNextMessage != null);
        boolean enablePrev = (mPreviousMessage != null);

        mDelete.setEnabled(true);

        if (next.isEnabled() != enableNext)
            next.setEnabled(enableNext);
        if (previous.isEnabled() != enablePrev)
            previous.setEnabled(enablePrev);

        if (next_scrolling != null && (next_scrolling.isEnabled() != enableNext))
            next_scrolling.setEnabled(enableNext);
        if (previous_scrolling != null && (previous_scrolling.isEnabled() != enablePrev))
            previous_scrolling.setEnabled(enablePrev);

        // If moving isn't support at all, then all of them must be disabled anyway.
        if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount))

        {
            // Only enable the button if the Archive folder is not the current folder and not NONE.
            boolean enableArchive = !mMessageReference.folderName.equals(mAccount.getArchiveFolderName()) &&
                                    !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName());
            boolean enableMove = true;
            // Only enable the button if the Spam folder is not the current folder and not NONE.
            boolean enableSpam = !mMessageReference.folderName.equals(mAccount.getSpamFolderName()) &&
                                 !K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName());
            mArchive.setEnabled(enableArchive);
            mMove.setEnabled(enableMove);
            mSpam.setEnabled(enableSpam);
            mArchiveScrolling.setEnabled(enableArchive);
            mMoveScrolling.setEnabled(enableMove);
            mSpamScrolling.setEnabled(enableSpam);
        }
        else
        {
            disableMoveButtons();
        }


    }

    private void showButtons()
    {
        View buttons = findViewById(R.id.scrolling_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
    }

    private void hideButtons()
    {
        View buttons = findViewById(R.id.bottom_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
    }

    private void showMoveButtons()
    {
        View buttons = findViewById(R.id.scrolling_move_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
    }

    private void hideMoveButtons()
    {
        View buttons = findViewById(R.id.move_buttons);
        if (buttons != null)
        {
            buttons.setVisibility(View.GONE);
        }
    }

    private void disableButtons()
    {
        setLoadPictures(false);
        disableMoveButtons();
        next.setEnabled(false);
        next_scrolling.setEnabled(false);
        previous.setEnabled(false);
        previous_scrolling.setEnabled(false);
        mDelete.setEnabled(false);
    }

    private void disableMoveButtons()
    {
        mArchive.setEnabled(false);
        mMove.setEnabled(false);
        mSpam.setEnabled(false);
        mArchiveScrolling.setEnabled(false);
        mMoveScrolling.setEnabled(false);
        mSpamScrolling.setEnabled(false);
    }


    private void setOnClickListener(int viewCode)
    {
        View thisView = findViewById(viewCode);
        if (thisView != null)
        {
            thisView.setOnClickListener(this);
        }
    }

    private void findSurroundingMessagesUid()
    {
        mNextMessage = mPreviousMessage = null;
        int i = mMessageReferences.indexOf(mMessageReference);
        if (i < 0)
            return;
        if (i != 0)
            mNextMessage = mMessageReferences.get(i - 1);
        if (i != (mMessageReferences.size() - 1))
            mPreviousMessage = mMessageReferences.get(i + 1);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    /**
     * Called from UI thread when user select Delete
     */
    private void onDelete()
    {
        if (K9.confirmDelete())
        {
            showDialog(R.id.dialog_confirm_delete);
        }
        else
        {
            delete();
        }
    }

    /**
     * @param id
     * @return Never <code>null</code>
     */
    protected Dialog createConfirmDeleteDialog(final int id)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_confirm_delete_title);
        builder.setMessage(R.string.dialog_confirm_delete_message);
        builder.setPositiveButton(R.string.dialog_confirm_delete_confirm_button,
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dismissDialog(id);
                        delete();
                    }
                });
        builder.setNegativeButton(R.string.dialog_confirm_delete_cancel_button,
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dismissDialog(id);
                    }
                });
        final AlertDialog dialog = builder.create();
        return dialog;
    }

    private void delete()
    {
        if (mMessage != null)
        {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            disableButtons();
            Message messageToDelete = mMessage;

            showNextMessageOrReturn();

            MessagingController.getInstance(getApplication()).deleteMessages(
                new Message[] { messageToDelete },
                null);
        }
    }

    private void onArchive()
    {
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mAccount))
        {
            return;
        }
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        String srcFolder = mMessageReference.folderName;
        String dstFolder = mAccount.getArchiveFolderName();
        Message messageToMove = mMessage;
        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder))
        {
            return;
        }
        showNextMessageOrReturn();
        MessagingController.getInstance(getApplication())
        .moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    private void onSpam()
    {
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mAccount))
        {
            return;
        }
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        String srcFolder = mMessageReference.folderName;
        String dstFolder = mAccount.getSpamFolderName();
        Message messageToMove = mMessage;
        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder))
        {
            return;
        }
        showNextMessageOrReturn();
        MessagingController.getInstance(getApplication())
        .moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    private void showNextMessageOrReturn()
    {
        if (K9.messageViewReturnToList())
        {
            finish();
        }
        else
        {
            showNextMessage();
        }
    }

    private void showNextMessage()
    {
        findSurroundingMessagesUid();
        mMessageReferences.remove(mMessageReference);

        if (mLastDirection == NEXT && mNextMessage != null)
        {
            onNext(K9.showAnimations());
        }
        else if (mLastDirection == PREVIOUS && mPreviousMessage != null)
        {
            onPrevious(K9.showAnimations());
        }
        else if (mNextMessage != null)
        {
            onNext(K9.showAnimations());
        }
        else if (mPreviousMessage != null)
        {
            onPrevious(K9.showAnimations());
        }
        else
        {
            finish();
        }
    }

    private void onClickSender()
    {
        if (mMessage != null)
        {
            try
            {
                final Address senderEmail = mMessage.getFrom()[0];
                mContacts.createContact(this, senderEmail);

                Address.clearContactsNameCache();
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Couldn't create contact", e);
            }
        }
    }

    private void onReply()
    {
        if (mMessage != null)
        {
            MessageCompose.actionReply(this, mAccount, mMessage, false, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onReplyAll()
    {
        if (mMessage != null)
        {
            MessageCompose.actionReply(this, mAccount, mMessage, true, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onForward()
    {
        if (mMessage != null)
        {
            MessageCompose.actionForward(this, mAccount, mMessage, mPgpData.getDecryptedData());
            finish();
        }
    }

    private void onFlag()
    {
        if (mMessage != null)
        {
            MessagingController.getInstance(getApplication()).setFlag(mAccount,
                    mMessage.getFolder().getName(), new String[] { mMessage.getUid() }, Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
            try
            {
                mMessage.setFlag(Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
                setHeaders(mAccount, mMessage.getFolder().getName(), mMessage.getUid(), mMessage);
                prepareMenuItems();
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Could not set flag on local message", me);
            }
        }
    }

    private void onMove()
    {
        if ((!MessagingController.getInstance(getApplication()).isMoveCapable(mAccount))
                || (mMessage == null))
        {
            return;
        }
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    private void onCopy()
    {
        if ((!MessagingController.getInstance(getApplication()).isCopyCapable(mAccount))
                || (mMessage == null))
        {
            return;
        }
        if (!MessagingController.getInstance(getApplication()).isCopyCapable(mMessage))
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        Intent intent = new Intent(this, ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    private void onShowAdditionalHeaders()
    {
        int currentVisibility = mAdditionalHeadersView.getVisibility();

        if (currentVisibility == View.VISIBLE)
        {
            mHandler.hideAdditionalHeaders();
        }
        else
        {
            mHandler.showAdditionalHeaders();
        }
    }

    private List<HeaderEntry> getAdditionalHeaders(final Message message)
    throws MessagingException
    {
        List<HeaderEntry> additionalHeaders = new LinkedList<HeaderEntry>();

        /*
         * Remove "Subject" header as it is already shown in the standard
         * message view header. But do show "From", "To", and "Cc" again.
         * This time including the email addresses. See issue 1805.
         */
        Set<String> headerNames = new HashSet<String>(message.getHeaderNames());
        headerNames.remove("Subject");

        for (String headerName : headerNames)
        {
            String[] headerValues = message.getHeader(headerName);
            for (String headerValue : headerValues)
            {
                additionalHeaders.add(new HeaderEntry(headerName, headerValue));
            }
        }
        return additionalHeaders;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (mAccount.getCryptoProvider().onActivityResult(this, requestCode, resultCode, data, mPgpData))
        {
            return;
        }

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode)
        {
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY:
                if (data == null)
                    return;
                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                String srcFolderName = data.getStringExtra(ChooseFolder.EXTRA_CUR_FOLDER);
                MessageReference ref = (MessageReference)data.getSerializableExtra(ChooseFolder.EXTRA_MESSAGE);

                if (mMessageReference.equals(ref))
                {
                    mAccount.setLastSelectedFolderName(destFolderName);

                    switch (requestCode)
                    {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE:
                            Message messageToMove = mMessage;

                            showNextMessageOrReturn();

                            MessagingController.getInstance(getApplication()).moveMessage(mAccount,
                                    srcFolderName, messageToMove, destFolderName, null);
                            break;
                        case ACTIVITY_CHOOSE_FOLDER_COPY:
                            MessagingController.getInstance(getApplication()).copyMessage(mAccount,
                                    srcFolderName, mMessage, destFolderName, null);
                            break;
                    }
                }
                break;


        }
    }


    private void onSendAlternate()
    {
        if (mMessage != null)
        {
            MessagingController.getInstance(getApplication()).sendAlternate(this, mAccount, mMessage);

        }
    }

    @Override
    protected void onNext(boolean animate)
    {
        if (mNextMessage == null)
        {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = NEXT;

        disableButtons();

        if (animate)
        {
            mTopView.startAnimation(outToLeftAnimation());
        }


        displayMessage(mNextMessage);
        next.requestFocus();
    }

    @Override
    protected void onPrevious(boolean animate)
    {
        if (mPreviousMessage == null)
        {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }

        mLastDirection = PREVIOUS;

        disableButtons();

        if (animate)
        {
            mTopView.startAnimation(inFromRightAnimation());
        }
        displayMessage(mPreviousMessage);
        previous.requestFocus();
    }

    private void onMarkAsUnread()
    {
        if (mMessage != null)
        {
            MessagingController.getInstance(getApplication()).setFlag(
                mAccount,
                mMessageReference.folderName,
                new String[] { mMessage.getUid() },
                Flag.SEEN,
                false);
            try
            {
                mMessage.setFlag(Flag.SEEN, false);
                setHeaders(mAccount, mMessage.getFolder().getName(), mMessage.getUid(), mMessage);
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to unset SEEN flag on message", e);
            }
        }
    }

    /**
     * Creates a unique file in the given directory by appending a hyphen
     * and a number to the given filename.
     * @param directory
     * @param filename
     * @return
     */
    private File createUniqueFile(File directory, String filename)
    {
        File file = new File(directory, filename);
        if (!file.exists())
        {
            return file;
        }
        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1)
        {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        }
        else
        {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++)
        {
            file = new File(directory, String.format(format, i));
            if (!file.exists())
            {
                return file;
            }
        }
        return null;
    }

    private void onDownloadRemainder()
    {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL))
        {
            return;
        }



        mDownloadRemainder.setEnabled(false);
        MessagingController.getInstance(getApplication()).loadMessageForViewRemote(
            mAccount,
            mMessageReference.folderName,
            mMessageReference.uid,
            mListener);

    }

    private void onDownloadAttachment(Attachment attachment)
    {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            /*
             * Abort early if there's no place to save the attachment. We don't want to spend
             * the time downloading it and then abort.
             */
            Toast.makeText(this,
                           getString(R.string.message_view_status_attachment_not_saved),
                           Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMessage != null)
        {
            MessagingController.getInstance(getApplication()).loadAttachment(
                mAccount,
                mMessage,
                attachment.part,
                new Object[] { true, attachment },
                mListener);
        }
    }

    private void onViewAttachment(Attachment attachment)
    {
        if (mMessage != null)
        {
            MessagingController.getInstance(getApplication()).loadAttachment(
                mAccount,
                mMessage,
                attachment.part,
                new Object[] { false, attachment },
                mListener);
        }
    }

    private void onShowPictures()
    {
        // TODO: Download attachments that are used as inline image

        setLoadPictures(true);
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
    private void setLoadPictures(boolean enable)
    {
        K9.setBlockNetworkLoads(mMessageContentView.getSettings(), !enable);
        mMessageContentView.getSettings().setBlockNetworkImage(!enable);
        mShowPictures = enable;
        mHandler.showShowPictures(false);
    }

    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.from:
                onClickSender();
                break;
            case R.id.reply:
            case R.id.reply_scrolling:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.delete:
            case R.id.delete_scrolling:
                onDelete();
                break;
            case R.id.forward:
            case R.id.forward_scrolling:
                onForward();
                break;
            case R.id.archive:
            case R.id.archive_scrolling:
                onArchive();
                break;
            case R.id.spam:
            case R.id.spam_scrolling:
                onSpam();
                break;
            case R.id.move:
            case R.id.move_scrolling:
                onMove();
                break;
            case R.id.next:
            case R.id.next_scrolling:
                onNext(K9.showAnimations());
                break;
            case R.id.previous:
            case R.id.previous_scrolling:
                onPrevious(K9.showAnimations());
                break;
            case R.id.download:
                onDownloadAttachment((Attachment) view.getTag());
                break;
            case R.id.view:
                onViewAttachment((Attachment) view.getTag());
                break;
            case R.id.show_pictures:
                onShowPictures();
                break;
            case R.id.header_container:
                onShowAdditionalHeaders();
                break;
            case R.id.download_remainder:
                onDownloadRemainder();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.delete:
                onDelete();
                break;
            case R.id.reply:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.forward:
                onForward();
                break;
            case R.id.send_alternate:
                onSendAlternate();
                break;
            case R.id.mark_as_unread:
                onMarkAsUnread();
                break;
            case R.id.flag:
                onFlag();
                break;
            case R.id.archive:
                onArchive();
                break;
            case R.id.spam:
                onSpam();
                break;
            case R.id.move:
                onMove();
                break;
            case R.id.copy:
                onCopy();
                break;
            case R.id.show_full_header:
                onShowAdditionalHeaders();
                break;
            case R.id.select_text:
                emulateShiftHeld(mMessageContentView);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        optionsMenu = menu;
        prepareMenuItems();
        if (!MessagingController.getInstance(getApplication()).isCopyCapable(mAccount))
        {
            menu.findItem(R.id.copy).setVisible(false);
        }
        if (!MessagingController.getInstance(getApplication()).isMoveCapable(mAccount))
        {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getArchiveFolderName()))
        {
            menu.findItem(R.id.archive).setVisible(false);
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(mAccount.getSpamFolderName()))
        {
            menu.findItem(R.id.spam).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        prepareMenuItems();
        return super.onPrepareOptionsMenu(menu);
    }

    // XXX when switching to API version 8, override onCreateDialog(int, Bundle)
    /**
     * @param id
     *            The id of the dialog.
     * @return The dialog. If you return null, the dialog will not be created.
     * @see android.app.Activity#onCreateDialog(int, Bundle)
     */
    @Override
    protected Dialog onCreateDialog(final int id)
    {
        switch (id)
        {
            case R.id.dialog_confirm_delete:
            {
                final Dialog dialog = createConfirmDeleteDialog(id);
                return dialog;
            }
        }
        return super.onCreateDialog(id);
    }

    private void prepareMenuItems()
    {
        Menu menu = optionsMenu;
        if (menu != null)
        {
            MenuItem flagItem = menu.findItem(R.id.flag);
            if (flagItem != null && mMessage != null)
            {
                flagItem.setTitle((mMessage.isSet(Flag.FLAGGED) ? R.string.unflag_action : R.string.flag_action));
            }

            MenuItem additionalHeadersItem = menu.findItem(R.id.show_full_header);
            if (additionalHeadersItem != null)
            {
                additionalHeadersItem.setTitle((mAdditionalHeadersView.getVisibility() == View.VISIBLE) ?
                                               R.string.hide_full_header_action : R.string.show_full_header_action);
            }
        }
    }

    private Bitmap getPreviewIcon(Attachment attachment) throws MessagingException
    {
        try
        {
            return BitmapFactory.decodeStream(
                       getContentResolver().openInputStream(
                           AttachmentProvider.getAttachmentThumbnailUri(mAccount,
                                   attachment.part.getAttachmentId(),
                                   62,
                                   62)));
        }
        catch (Exception e)
        {
            /*
             * We don't care what happened, we just return null for the preview icon.
             */
            return null;
        }
    }

    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 = 12.3 MB
     */
    public static String formatSize(float size)
    {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb  = (mb * 1024);
        if (size < kb)
        {
            return String.format("%d bytes", (int) size);
        }
        else if (size < mb)
        {
            return String.format("%.1f kB", size / kb);
        }
        else if (size < gb)
        {
            return String.format("%.1f MB", size / mb);
        }
        else
        {
            return String.format("%.1f GB", size / gb);
        }
    }

    private void renderAttachments(Part part, int depth) throws MessagingException
    {
        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
        String name = MimeUtility.getHeaderParameter(contentType, "name");


        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Don't show attachment download buttons for them.

        if (contentDisposition != null &&
                MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)")
                && part.getHeader("Content-ID") != null)
        {
            return;
        }

        if (name == null)
        {
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }
        if (name != null)
        {
            /*
             * We're guaranteed size because LocalStore.fetch puts it there.
             */
            int size = Integer.parseInt(MimeUtility.getHeaderParameter(contentDisposition, "size"));

            Attachment attachment = new Attachment();
            attachment.size = size;
            String mimeType = part.getMimeType();
            if (MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE.equals(mimeType))
            {
                mimeType = MimeUtility.getMimeTypeByExtension(name);
            }
            attachment.contentType = mimeType;
            attachment.name = name;
            attachment.part = (LocalAttachmentBodyPart) part;

            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.message_view_attachment, null);

            TextView attachmentName = (TextView)view.findViewById(R.id.attachment_name);
            TextView attachmentInfo = (TextView)view.findViewById(R.id.attachment_info);
            ImageView attachmentIcon = (ImageView)view.findViewById(R.id.attachment_icon);
            Button attachmentView = (Button)view.findViewById(R.id.view);
            Button attachmentDownload = (Button)view.findViewById(R.id.download);

            if ((!MimeUtility.mimeTypeMatches(attachment.contentType,
                                              K9.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                    || (MimeUtility.mimeTypeMatches(attachment.contentType,
                                                    K9.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES)))
            {
                attachmentView.setVisibility(View.GONE);
            }
            if ((!MimeUtility.mimeTypeMatches(attachment.contentType,
                                              K9.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                    || (MimeUtility.mimeTypeMatches(attachment.contentType,
                                                    K9.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES)))
            {
                attachmentDownload.setVisibility(View.GONE);
            }

            if (attachment.size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE)
            {
                attachmentView.setVisibility(View.GONE);
                attachmentDownload.setVisibility(View.GONE);
            }

            attachment.viewButton = attachmentView;
            attachment.downloadButton = attachmentDownload;
            attachment.iconView = attachmentIcon;

            view.setTag(attachment);
            attachmentView.setOnClickListener(this);
            attachmentView.setTag(attachment);
            attachmentDownload.setOnClickListener(this);
            attachmentDownload.setTag(attachment);

            attachmentName.setText(name);
            attachmentInfo.setText(formatSize(size));

            Bitmap previewIcon = getPreviewIcon(attachment);
            if (previewIcon != null)
            {
                attachmentIcon.setImageBitmap(previewIcon);
            }
            else
            {
                attachmentIcon.setImageResource(R.drawable.attached_image_placeholder);
            }

            mHandler.addAttachment(view);
        }

        if (part.getBody() instanceof Multipart)
        {
            Multipart mp = (Multipart)part.getBody();
            for (int i = 0; i < mp.getCount(); i++)
            {
                renderAttachments(mp.getBodyPart(i), depth + 1);
            }
        }
    }

    private void setHeaders(Account account, String folder, String uid,
                            final Message message) throws MessagingException
    {
        String subjectText = message.getSubject();
        CharSequence fromText = Address.toFriendly(message.getFrom(), mContacts);
        String dateText = getDateFormat().format(message.getSentDate());
        String timeText = getTimeFormat().format(message.getSentDate());
        CharSequence toText = Address.toFriendly(message.getRecipients(RecipientType.TO), mContacts);
        CharSequence ccText = Address.toFriendly(message.getRecipients(RecipientType.CC), mContacts);

        int color = mAccount.getChipColor();
        boolean hasAttachments = ((LocalMessage) message).getAttachmentCount() > 0;
        boolean unread = !message.isSet(Flag.SEEN);

        mHandler.setHeaders(subjectText,
                            fromText,
                            dateText,
                            timeText,
                            toText,
                            ccText,
                            color,
                            unread,
                            hasAttachments,
                            message.isSet(Flag.FLAGGED),
                            message.isSet(Flag.ANSWERED));

        // Update additional headers display, if visible
        if (mAdditionalHeadersView.getVisibility() == View.VISIBLE)
        {
            mHandler.showAdditionalHeaders();
        }
    }

    class Listener extends MessagingListener
    {

        @Override
        public void loadMessageForViewHeadersAvailable(Account account, String folder, String uid,
                final Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            MessageView.this.mMessage = message;


            if (!message.isSet(Flag.X_DOWNLOADED_FULL)
                    && !message.isSet(Flag.X_DOWNLOADED_PARTIAL))
            {
                mHandler.post(new Runnable()
                {
                    public void run()
                    {
                        mMessageContentView.loadUrl("file:///android_asset/downloading.html");
                        updateDecryptLayout();
                    }
                });
            }
            try
            {
                setHeaders(account, folder, uid, message);
                mHandler.showHeaderContainer();
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "loadMessageForViewHeadersAvailable", me);
            }
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
                Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            try
            {
                if (MessageView.this.mMessage!=null
                        && MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)
                        && message.isSet(Flag.X_DOWNLOADED_FULL))
                {

                    setHeaders(account, folder, uid, message);
                    mHandler.showHeaderContainer();
                }

                MessageView.this.mMessage = message;


                mHandler.removeAllAttachments();

                String text;
                String type = "text/html";
                if (mPgpData.getDecryptedData() != null)
                {
                    text = mPgpData.getDecryptedData();
                    type = "text/plain";
                }
                else
                {
                    Part part = MimeUtility.findFirstPartByMimeType(mMessage, "text/html");
                    if (part == null)
                    {
                        part = MimeUtility.findFirstPartByMimeType(mMessage, "text/plain");
                        if (part == null)
                        {
                            text = null;
                        }
                        else
                        {
                            LocalTextBody body = (LocalTextBody)part.getBody();
                            if (body == null)
                            {
                                text = null;
                            }
                            else
                            {
                                text = body.getBodyForDisplay();
                            }
                        }
                    }
                    else
                    {
                        text = MimeUtility.getTextFromPart(part);
                    }
                }

                if (text != null)
                {
                    final String emailText = text;
                    final String mimeType = type;
                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mTopView.scrollTo(0, 0);
                            if (mScreenReaderEnabled)
                            {
                                mAccessibleMessageContentView.loadDataWithBaseURL("http://",
                                        emailText, "text/html", "utf-8", null);
                            }
                            else
                            {
                                mMessageContentView.loadDataWithBaseURL("http://", emailText,
                                                                        "text/html", "utf-8", null);
                                mMessageContentView.scrollTo(0, 0);
                            }
                            updateDecryptLayout();
                        }
                    });

                    // If the message contains external pictures and the "Show pictures"
                    // button wasn't already pressed, see if the user's preferences has us
                    // showing them anyway.
                    if (hasExternalImages(text) && !mShowPictures)
                    {
                        if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                                ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                                 mContacts.isInContacts(message.getFrom()[0].getAddress())))
                        {
                            onShowPictures();
                        }
                        else
                        {
                            mHandler.showShowPictures(true);
                        }
                    }
                }
                else
                {
                    mHandler.post(new Runnable()
                    {
                        public void run()
                        {
                            mMessageContentView.loadUrl("file:///android_asset/empty.html");
                            updateDecryptLayout();
                        }
                    });
                }

                renderAttachments(mMessage, 0);
            }
            catch (Exception e)
            {
                if (Config.LOGV)
                {
                    Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                }
            }
        }//loadMessageForViewBodyAvailable

        private static final String IMG_SRC_REGEX = "(?is:<img[^>]+src\\s*=\\s*['\"]?([a-z]+)\\:)";
        private final Pattern mImgPattern = Pattern.compile(IMG_SRC_REGEX);
        private boolean hasExternalImages(final String message)
        {
            Matcher imgMatches = mImgPattern.matcher(message);
            while (imgMatches.find())
            {
                if (!imgMatches.group(1).equals("content"))
                {
                    if (K9.DEBUG)
                    {
                        Log.d(K9.LOG_TAG, "External images found");
                    }
                    return true;
                }
            }
            if (K9.DEBUG)
            {
                Log.d(K9.LOG_TAG, "No external images.");
            }
            return false;
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid,
                                             final Throwable t)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            mHandler.post(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(false);
                    if (t instanceof IllegalArgumentException)
                    {
                        mHandler.invalidIdError();
                    }
                    else
                    {
                        mHandler.networkError();
                    }
                    if ((MessageView.this.mMessage == null) ||
                            !MessageView.this.mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL))
                    {
                        mMessageContentView.loadUrl("file:///android_asset/empty.html");
                        updateDecryptLayout();
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid,
                                               Message message)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            mHandler.post(new Runnable()
            {
                public void run()
                {
                    setProgressBarIndeterminateVisibility(false);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid)
        {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid()))
            {
                return;
            }

            mHandler.post(new Runnable()
            {
                public void run()
                {
                    updateDecryptLayout();
                    setProgressBarIndeterminateVisibility(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message,
                                          Part part, Object tag, boolean requiresDownload)
        {
            if (mMessage!=message)
            {
                return;
            }

            mHandler.setAttachmentsEnabled(false);
            mHandler.progress(true);
            if (requiresDownload)
            {
                mHandler.fetchingAttachment();
            }
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message,
                                           Part part, Object tag)
        {
            if (mMessage!=message)
            {
                return;
            }

            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);

            Object[] params = (Object[]) tag;
            boolean download = (Boolean) params[0];
            Attachment attachment = (Attachment) params[1];

            if (download)
            {
                try
                {
                    File file = createUniqueFile(Environment.getExternalStorageDirectory(),
                                                 attachment.name);
                    Uri uri = AttachmentProvider.getAttachmentUri(
                                  mAccount,
                                  attachment.part.getAttachmentId());
                    InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(file);
                    IOUtils.copy(in, out);
                    out.flush();
                    out.close();
                    in.close();
                    mHandler.attachmentSaved(file.getName());
                    new MediaScannerNotifier(MessageView.this, file);
                }
                catch (IOException ioe)
                {
                    mHandler.attachmentNotSaved();
                }
            }
            else
            {
                Uri uri = AttachmentProvider.getAttachmentUri(
                              mAccount,
                              attachment.part.getAttachmentId());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try
                {
                    startActivity(intent);
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Could not display attachment of type " + attachment.contentType, e);
                    Toast toast = Toast.makeText(MessageView.this, getString(R.string.message_view_no_viewer, attachment.contentType), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part,
                                         Object tag, String reason)
        {
            if (mMessage!=message)
            {
                return;
            }

            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);
            mHandler.networkError();
        }
    }

    class MediaScannerNotifier implements MediaScannerConnectionClient
    {
        private MediaScannerConnection mConnection;
        private File mFile;

        public MediaScannerNotifier(Context context, File file)
        {
            mFile = file;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected()
        {
            mConnection.scanFile(mFile.getAbsolutePath(), null);
        }

        public void onScanCompleted(String path, Uri uri)
        {
            try
            {
                if (uri != null)
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
            finally
            {
                mConnection.disconnect();
            }
        }
    }


    private Animation inFromRightAnimation()
    {
        return slideAnimation(0.0f, +1.0f);
    }

    private Animation outToLeftAnimation()
    {
        return slideAnimation(0.0f, -1.0f);
    }

    private Animation slideAnimation(float right, float left)
    {

        Animation slide = new TranslateAnimation(
            Animation.RELATIVE_TO_PARENT,  right, Animation.RELATIVE_TO_PARENT,  left,
            Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
        );
        slide.setDuration(125);
        slide.setFillBefore(true);
        slide.setInterpolator(new AccelerateInterpolator());
        return slide;
    }

    private void initializeCrypto()
    {
        if (mPgpData != null)
        {
            return;
        }
        if (mAccount == null)
        {
            mAccount = Preferences.getPreferences(this).getAccount(mMessageReference.accountUuid);
        }
        mPgpData = new PgpData();
    }

    /**
     * Fill the decrypt layout with signature data, if known, make controls visible, if
     * they should be visible.
     */
    public void updateDecryptLayout()
    {
        if (mPgpData.getSignatureKeyId() != 0)
        {
            mCryptoSignatureUserIdRest.setText(
                getString(R.string.key_id, Long.toHexString(mPgpData.getSignatureKeyId() & 0xffffffffL)));
            String userId = mPgpData.getSignatureUserId();
            if (userId == null)
            {
                userId = getString(R.string.unknown_crypto_signature_user_id);
            }
            String chunks[] = userId.split(" <", 2);
            String name = chunks[0];
            if (chunks.length > 1)
            {
                mCryptoSignatureUserIdRest.setText("<" + chunks[1]);
            }
            mCryptoSignatureUserId.setText(name);

            if (mPgpData.getSignatureSuccess())
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
            }
            else if (mPgpData.getSignatureUnknown())
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            else
            {
                mCryptoSignatureStatusImage.setImageResource(R.drawable.overlay_error);
            }
            mCryptoSignatureLayout.setVisibility(View.VISIBLE);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mCryptoSignatureLayout.setVisibility(View.INVISIBLE);
        }

        if (false || ((mMessage == null) && (mPgpData.getDecryptedData() == null)))
        {
            mDecryptLayout.setVisibility(View.GONE);
            return;
        }

        if (mPgpData.getDecryptedData() != null)
        {
            if (mPgpData.getSignatureKeyId() == 0)
            {
                mDecryptLayout.setVisibility(View.GONE);
            }
            else
            {
                // no need to show this after decryption/verification
                mDecryptButton.setVisibility(View.GONE);
            }
            return;
        }

        mDecryptButton.setVisibility(View.VISIBLE);

        CryptoProvider crypto = mAccount.getCryptoProvider();
        if (crypto.isEncrypted(mMessage))
        {
            mDecryptButton.setText(R.string.btn_decrypt);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else if (crypto.isSigned(mMessage))
        {
            mDecryptButton.setText(R.string.btn_verify);
            mDecryptLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mDecryptLayout.setVisibility(View.GONE);
            try
            {
                // check for PGP/MIME encryption
                Part pgp = MimeUtility.findFirstPartByMimeType(mMessage, "application/pgp-encrypted");
                if (pgp != null)
                {
                    Toast.makeText(this, R.string.pgp_mime_unsupported, Toast.LENGTH_LONG).show();
                }
            }
            catch (MessagingException e)
            {
                // nothing to do...
            }
        }
    }

    public void onDecryptDone()
    {
        // TODO: this might not be enough if the orientation was changed while in APG,
        // sometimes shows the original encrypted content
        mMessageContentView.loadDataWithBaseURL("email://", mPgpData.getDecryptedData(), "text/plain", "utf-8", null);
        updateDecryptLayout();
    }

    /*
     * Emulate the shift key being pressed to trigger the text selection mode
     * of a WebView.
     */
    private void emulateShiftHeld(WebView view)
    {
        try
        {
            mToggleScrollView.setScrolling(false);

            KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                                                    KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
            shiftPressEvent.dispatch(view);
            Toast.makeText(this, R.string.select_text_now, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Exception in emulateShiftHeld()", e);
        }
    }
}
