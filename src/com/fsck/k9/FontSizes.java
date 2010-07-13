package com.fsck.k9;

import android.content.SharedPreferences;
import android.webkit.WebSettings.TextSize;

/**
 * Manage font size of the information displayed in the account list, folder
 * list, message list and in the message view.
 */
public class FontSizes
{
    /*
     * Keys for the preference storage.
     */
    private static final String ACCOUNT_NAME = "fontSizeAccountName";
    private static final String ACCOUNT_DESCRIPTION = "fontSizeAccountDescription";
    private static final String FOLDER_NAME = "fontSizeFolderName";
    private static final String FOLDER_STATUS = "fontSizeFolderStatus";
    private static final String MESSAGE_LIST_SUBJECT = "fontSizeMessageListSubject";
    private static final String MESSAGE_LIST_SENDER = "fontSizeMessageListSender";
    private static final String MESSAGE_LIST_DATE = "fontSizeMessageListDate";
    private static final String MESSAGE_VIEW_SENDER = "fontSizeMessageViewSender";
    private static final String MESSAGE_VIEW_TO = "fontSizeMessageViewTo";
    private static final String MESSAGE_VIEW_CC = "fontSizeMessageViewCC";
    private static final String MESSAGE_VIEW_ADDITIONAL_HEADERS = "fontSizeMessageViewAdditionalHeaders";
    private static final String MESSAGE_VIEW_SUBJECT = "fontSizeMessageViewSubject";
    private static final String MESSAGE_VIEW_TIME = "fontSizeMessageViewTime";
    private static final String MESSAGE_VIEW_DATE = "fontSizeMessageViewDate";
    private static final String MESSAGE_VIEW_CONTENT = "fontSizeMessageViewContent";

    /*
     * Values for the font sizes in DIP (device independent pixel)
     */
    public static final int FONT_10DIP = 10;
    public static final int FONT_12DIP = 12;
    public static final int SMALL = 14;         // ?android:attr/textAppearanceSmall
    public static final int FONT_16DIP = 16;
    public static final int MEDIUM = 18;        // ?android:attr/textAppearanceMedium
    public static final int FONT_20DIP = 20;
    public static final int LARGE = 22;         // ?android:attr/textAppearanceLarge


    /**
     * Font size of account names in the account list activity.
     */
    private int accountName;

    /**
     * Font size of account descriptions in the account list activity.
     */
    private int accountDescription;

    /**
     * Font size of folder names in the folder list activity.
     */
    private int folderName;

    /**
     * Font size of the folder status in the folder list activity.
     */
    private int folderStatus;

    /**
     * Font size of message subjects in the message list activity.
     */
    private int messageListSubject;

    /**
     * Font size of message senders in the message list activity.
     */
    private int messageListSender;

    /**
     * Font size of message dates in the message list activity.
     */
    private int messageListDate;

    /**
     * Font size of the message sender in the message view activity.
     */
    private int messageViewSender;

    /**
     * Font size of the message receiver(s) (To) in the message view activity.
     */
    private int messageViewTo;

    /**
     * Font size of the message receiver(s) (CC) in the message view activity.
     */
    private int messageViewCC;

    /**
     * Font size of additional headers in the message view activity.
     */
    private int messageViewAdditionalHeaders;

    /**
     * Font size of the message subject in the message view activity.
     */
    private int messageViewSubject;

    /**
     * Font size of the message time in the message view activity.
     */
    private int messageViewTime;

    /**
     * Font size of the message date in the message view activity.
     */
    private int messageViewDate;

    /**
     * Font size of the message content in the message view activity.
     *
     * Note: The unit is WebSettings.TextSize
     */
    private TextSize messageViewContent = TextSize.NORMAL;

    /**
     * Create a <code>FontSizes</code> object with default values.
     */
    public FontSizes()
    {
        accountName = MEDIUM;
        accountDescription = SMALL;

        folderName = LARGE;
        folderStatus = SMALL;

        messageListSubject = SMALL;
        messageListSender = SMALL;
        messageListDate = SMALL;

        messageViewSender = SMALL;
        messageViewTo = FONT_12DIP;
        messageViewCC = FONT_12DIP;
        messageViewAdditionalHeaders = FONT_12DIP;
        messageViewSubject = FONT_12DIP;
        messageViewTime = FONT_10DIP;
        messageViewDate = FONT_10DIP;
    }

    /**
     * Permanently save the font size settings.
     *
     * @param editor Used to save the font size settings.
     */
    public void save(SharedPreferences.Editor editor)
    {
        editor.putInt(ACCOUNT_NAME, accountName);
        editor.putInt(ACCOUNT_DESCRIPTION, accountDescription);

        editor.putInt(FOLDER_NAME, folderName);
        editor.putInt(FOLDER_STATUS, folderStatus);

        editor.putInt(MESSAGE_LIST_SUBJECT, messageListSubject);
        editor.putInt(MESSAGE_LIST_SENDER, messageListSender);
        editor.putInt(MESSAGE_LIST_DATE, messageListDate);

        editor.putInt(MESSAGE_VIEW_SUBJECT, messageViewSubject);
        editor.putInt(MESSAGE_VIEW_TO, messageViewTo);
        editor.putInt(MESSAGE_VIEW_CC, messageViewCC);
        editor.putInt(MESSAGE_VIEW_ADDITIONAL_HEADERS, messageViewAdditionalHeaders);
        editor.putInt(MESSAGE_VIEW_SENDER, messageViewSender);
        editor.putInt(MESSAGE_VIEW_TIME, messageViewTime);
        editor.putInt(MESSAGE_VIEW_DATE, messageViewDate);
        editor.putInt(MESSAGE_VIEW_CONTENT, getMessageViewContentAsInt());
    }

    /**
     * Load the font size settings from permanent storage.
     *
     * @param prefs Used to load the font size settings.
     */
    public void load(SharedPreferences prefs)
    {
        accountName = prefs.getInt(ACCOUNT_NAME, accountName);
        accountDescription = prefs.getInt(ACCOUNT_DESCRIPTION, accountDescription);

        folderName = prefs.getInt(FOLDER_NAME, folderName);
        folderStatus = prefs.getInt(FOLDER_STATUS, folderStatus);

        messageListSubject = prefs.getInt(MESSAGE_LIST_SUBJECT, messageListSubject);
        messageListSender = prefs.getInt(MESSAGE_LIST_SENDER, messageListSender);
        messageListDate = prefs.getInt(MESSAGE_LIST_DATE, messageListDate);

        messageViewSubject = prefs.getInt(MESSAGE_VIEW_SENDER, FONT_12DIP);
        messageViewTo = prefs.getInt(MESSAGE_VIEW_TO, messageViewTo);
        messageViewCC = prefs.getInt(MESSAGE_VIEW_CC, messageViewCC);
        messageViewAdditionalHeaders = prefs.getInt(MESSAGE_VIEW_ADDITIONAL_HEADERS, messageViewAdditionalHeaders);
        messageViewSender = prefs.getInt(MESSAGE_VIEW_SUBJECT, messageViewSender);
        messageViewTime = prefs.getInt(MESSAGE_VIEW_TIME, messageViewTime);
        messageViewDate = prefs.getInt(MESSAGE_VIEW_DATE, messageViewDate);
        setMessageViewContent(prefs.getInt(MESSAGE_VIEW_CONTENT, 3));
    }

    public int getAccountName()
    {
        return accountName;
    }

    public void setAccountName(int accountName)
    {
        this.accountName = accountName;
    }

    public int getAccountDescription()
    {
        return accountDescription;
    }

    public void setAccountDescription(int accountDescription)
    {
        this.accountDescription = accountDescription;
    }

    public int getFolderName()
    {
        return folderName;
    }

    public void setFolderName(int folderName)
    {
        this.folderName = folderName;
    }

    public int getFolderStatus()
    {
        return folderStatus;
    }

    public void setFolderStatus(int folderStatus)
    {
        this.folderStatus = folderStatus;
    }

    public int getMessageListSubject()
    {
        return messageListSubject;
    }

    public void setMessageListSubject(int messageListSubject)
    {
        this.messageListSubject = messageListSubject;
    }

    public int getMessageListSender()
    {
        return messageListSender;
    }

    public void setMessageListSender(int messageListSender)
    {
        this.messageListSender = messageListSender;
    }

    public int getMessageListDate()
    {
        return messageListDate;
    }

    public void setMessageListDate(int messageListDate)
    {
        this.messageListDate = messageListDate;
    }

    public int getMessageViewSender()
    {
        return messageViewSender;
    }

    public void setMessageViewSender(int messageViewSender)
    {
        this.messageViewSender = messageViewSender;
    }

    public int getMessageViewTo()
    {
        return messageViewTo;
    }

    public void setMessageViewTo(int messageViewTo)
    {
        this.messageViewTo = messageViewTo;
    }

    public int getMessageViewCC()
    {
        return messageViewCC;
    }

    public void setMessageViewCC(int messageViewCC)
    {
        this.messageViewCC = messageViewCC;
    }

    public int getMessageViewAdditionalHeaders()
    {
        return messageViewAdditionalHeaders;
    }

    public void setMessageViewAdditionalHeaders(int messageViewAdditionalHeaders)
    {
        this.messageViewAdditionalHeaders = messageViewAdditionalHeaders;
    }

    public int getMessageViewSubject()
    {
        return messageViewSubject;
    }

    public void setMessageViewSubject(int messageViewSubject)
    {
        this.messageViewSubject = messageViewSubject;
    }

    public int getMessageViewTime()
    {
        return messageViewTime;
    }

    public void setMessageViewTime(int messageViewTime)
    {
        this.messageViewTime = messageViewTime;
    }

    public int getMessageViewDate()
    {
        return messageViewDate;
    }

    public void setMessageViewDate(int messageViewDate)
    {
        this.messageViewDate = messageViewDate;
    }

    public TextSize getMessageViewContent()
    {
        return messageViewContent;
    }

    public int getMessageViewContentAsInt()
    {
        switch (messageViewContent)
        {
            case SMALLEST:
                return 1;
            case SMALLER:
                return 2;
            default:
            case NORMAL:
                return 3;
            case LARGER:
                return 4;
            case LARGEST:
                return 5;
        }
    }

    public void setMessageViewContent(int size)
    {
        switch (size)
        {
            case 1:
                messageViewContent = TextSize.SMALLEST;
                break;
            case 2:
                messageViewContent = TextSize.SMALLER;
                break;
            case 3:
                messageViewContent = TextSize.NORMAL;
                break;
            case 4:
                messageViewContent = TextSize.LARGER;
                break;
            case 5:
                messageViewContent = TextSize.LARGEST;
                break;
        }
    }
}
