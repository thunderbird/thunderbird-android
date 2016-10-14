package com.fsck.k9.ui.messageview.ical;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.ICalendarHelper;
import com.fsck.k9.ical.ICalData.ICalendarData;
import com.fsck.k9.mailstore.ICalendarViewInfo;

import biweekly.util.Frequency;
import biweekly.util.Recurrence;

public class ICalendarReplyView extends ICalendarView implements View.OnClickListener, View.OnLongClickListener {

    private Context mContext;
    private TextView summaryView;
    private TextView summaryLabel;
    private TextView organizerView;
    private TextView organizerLabel;

    private TextView acceptedView;
    private TextView acceptedLabel;
    private TextView declinedView;
    private TextView declinedLabel;
    private TextView tentativeView;
    private TextView tentativeLabel;

    private TextView locationView;
    private TextView locationLabel;
    private TextView dateTimeView;
    private TextView dateTimeLabel;
    private TextView mRecurrenceView;

    private FontSizes mFontSizes = K9.getFontSizes();
    private Contacts mContacts;

    private ICalendarViewInfo viewInfo;
    private ICalendarData iCalendar;

    private Button viewButton;
    private Button downloadButton;
    private ICalendarViewCallback callback;
    private boolean showSummary;


    public ICalendarReplyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mContacts = Contacts.getInstance(mContext);
        
    }
    public void setCallback(ICalendarViewCallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        summaryView = (TextView) findViewById(R.id.summary);
        summaryLabel = (TextView) findViewById(R.id.summary_label);
        organizerView = (TextView) findViewById(R.id.organizer);
        organizerLabel = (TextView) findViewById(R.id.organizer_label);

        acceptedView = (TextView) findViewById(R.id.accepted);
        acceptedLabel = (TextView) findViewById(R.id.accepted_label);
        declinedLabel = (TextView) findViewById(R.id.declined_label);
        declinedView = (TextView) findViewById(R.id.declined);
        declinedLabel = (TextView) findViewById(R.id.declined_label);
        tentativeView = (TextView) findViewById(R.id.tentative);
        tentativeLabel = (TextView) findViewById(R.id.tentative_label);

        locationView = (TextView) findViewById(R.id.location);
        locationLabel = (TextView) findViewById(R.id.location_label);
        dateTimeView = (TextView) findViewById(R.id.date_time);
        dateTimeLabel = (TextView) findViewById(R.id.date_time_label);
        mRecurrenceView = (TextView) findViewById(R.id.recurrence);

        mFontSizes.setViewTextSize(organizerView, mFontSizes.getICalendarViewOrganizer());
        mFontSizes.setViewTextSize(organizerLabel, mFontSizes.getICalendarViewOrganizer());
        mFontSizes.setViewTextSize(locationView, mFontSizes.getICalendarViewLocation());
        mFontSizes.setViewTextSize(locationLabel, mFontSizes.getICalendarViewLocation());
        mFontSizes.setViewTextSize(dateTimeView, mFontSizes.getICalendarViewDateTime());
        mFontSizes.setViewTextSize(dateTimeLabel, mFontSizes.getICalendarViewDateTime());
        mFontSizes.setViewTextSize(mRecurrenceView, mFontSizes.getICalendarViewDateTime());


        viewButton = (Button) findViewById(R.id.view);
        downloadButton = (Button) findViewById(R.id.download);

        viewButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        downloadButton.setOnLongClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.view: {
                onViewButtonClick();
                break;
            }
            case R.id.download: {
                onSaveButtonClick();
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.download) {
            onSaveButtonLongClick();
            return true;
        }

        return false;
    }

    private void onViewButtonClick() {
        callback.onViewICalendar(viewInfo);
    }

    private void onSaveButtonClick() {
        callback.onSaveICalendar(viewInfo);
    }

    private void onSaveButtonLongClick() {
        callback.onSaveICalendarToUserProvidedDirectory(viewInfo);
    }


    public void setICalendar(ICalendarViewInfo viewInfo, ICalendarData iCalendar) {
        this.viewInfo = viewInfo;
        this.iCalendar = iCalendar;

        displayICalendarInformation();
    }

    private void displayICalendarInformation() {
        final Contacts contacts = K9.showContactName() ? mContacts : null;

        final CharSequence organizer = ICalendarHelper.toFriendly(iCalendar.getOrganizer(), contacts);
        final CharSequence required = ICalendarHelper.toFriendly(iCalendar.getRequired(), contacts);
        final CharSequence optional = ICalendarHelper.toFriendly(iCalendar.getOptional(), contacts);
        final CharSequence fyi = ICalendarHelper.toFriendly(iCalendar.getFyi(), contacts);

        final CharSequence accepted = ICalendarHelper.toFriendly(iCalendar.getAccepted(), contacts);
        final CharSequence declined = ICalendarHelper.toFriendly(iCalendar.getDeclined(), contacts);
        final CharSequence tentative = ICalendarHelper.toFriendly(iCalendar.getTentative(), contacts);

        if(iCalendar.getRecurrenceRule() == null) {
            mRecurrenceView.setVisibility(GONE);
        } else {
            mRecurrenceView.setText(buildRule(iCalendar.getRecurrenceRule().getValue(), getResources()));
        }
        
        if (showSummary) {
            updateField(summaryView, iCalendar.getSummary(), summaryLabel);
        } else {
            summaryView.setVisibility(GONE);
            summaryLabel.setVisibility(GONE);
        }
        updateField(acceptedView, accepted, acceptedLabel);
        updateField(declinedView, declined, declinedLabel);
        updateField(tentativeView, tentative, tentativeLabel);
        updateField(locationView, iCalendar.getLocation(), locationLabel);
        updateField(dateTimeView, iCalendar.getDateTime(), dateTimeLabel);

    }

    private String buildRule(Recurrence recurrence, Resources resources) {
        Frequency frequency = recurrence.getFrequency();
        switch (frequency) {
            case SECONDLY: return resources.getString(R.string.ical_recurrence_secondly);
            case MINUTELY: return resources.getString(R.string.ical_recurrence_minutely);
            case HOURLY: return resources.getString(R.string.ical_recurrence_hourly);
            case DAILY: return resources.getString(R.string.ical_recurrence_daily);
            case WEEKLY: return resources.getString(R.string.ical_recurrence_weekly);
            case MONTHLY: return resources.getString(R.string.ical_recurrence_monthly);
            case YEARLY: return resources.getString(R.string.ical_recurrence_yearly);
        }
        return "";
    }

    //TODO: Copied from MessageHeader - consider static method
    private void updateField(TextView v, CharSequence text, View label) {
        boolean hasText = !TextUtils.isEmpty(text);

        v.setText(text);
        v.setVisibility(hasText ? View.VISIBLE : View.GONE);
        label.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }


    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
    }
}
