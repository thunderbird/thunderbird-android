package com.fsck.k9.ui.messageview.ical;


import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import biweekly.util.Frequency;
import biweekly.util.Recurrence;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.ICalendarHelper;
import com.fsck.k9.ical.ICalData.ICalendarData;
import com.fsck.k9.mailstore.ICalendarViewInfo;


public class ICalendarRequestView extends ICalendarView implements View.OnClickListener, View.OnLongClickListener {

    private Context mContext;
    private TextView summaryView;
    private TextView summaryLabel;
    private TextView organizerView;
    private TextView organizerLabel;

    private TextView requiredView;
    private TextView requiredLabel;
    private TextView optionalView;
    private TextView optionalLabel;
    private TextView fyiView;
    private TextView fyiLabel;

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


    public ICalendarRequestView(Context context, AttributeSet attrs) {
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

        requiredView = (TextView) findViewById(R.id.required);
        requiredLabel = (TextView) findViewById(R.id.required_label);
        optionalView = (TextView) findViewById(R.id.optional);
        optionalLabel = (TextView) findViewById(R.id.optional_label);
        fyiView = (TextView) findViewById(R.id.fyi);
        fyiLabel = (TextView) findViewById(R.id.fyi_label);

        locationView = (TextView) findViewById(R.id.location);
        locationLabel = (TextView) findViewById(R.id.location_label);
        dateTimeView = (TextView) findViewById(R.id.date_time);
        dateTimeLabel = (TextView) findViewById(R.id.date_time_label);
        mRecurrenceView = (TextView) findViewById(R.id.recurrence);

        mFontSizes.setViewTextSize(organizerView, mFontSizes.getICalendarViewOrganizer());
        mFontSizes.setViewTextSize(organizerLabel, mFontSizes.getICalendarViewOrganizer());
        mFontSizes.setViewTextSize(requiredView, mFontSizes.getICalendarViewRequired());
        mFontSizes.setViewTextSize(requiredLabel, mFontSizes.getICalendarViewRequired());
        mFontSizes.setViewTextSize(optionalView, mFontSizes.getICalendarViewOptional());
        mFontSizes.setViewTextSize(optionalLabel, mFontSizes.getICalendarViewOptional());
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
        //TODO: Not show organizer if it's the same as the email sender
        updateField(organizerView, organizer, organizerLabel);
        updateField(requiredView, required, requiredLabel);
        updateField(optionalView, optional, optionalLabel);
        updateField(fyiView, fyi, fyiLabel);
        updateField(locationView, iCalendar.getLocation(), locationLabel);
        updateField(dateTimeView, iCalendar.getDateTime(), dateTimeLabel);

    }

    private String buildRule(Recurrence recurrence, Resources resources) {
        Frequency frequency = recurrence.getFrequency();
        switch (frequency) {
            case SECONDLY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_secondly);
                } else {
                    return resources.getString(R.string.ical_recurrence_secondly_interval, recurrence.getInterval());
                }
            case MINUTELY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_minutely);
                } else {
                    return resources.getString(R.string.ical_recurrence_minutely_interval, recurrence.getInterval());
                }
            case HOURLY:
                if (recurrence.getInterval() == 1) { return resources.getString(R.string.ical_recurrence_hourly);
                } else {
                    return resources.getString(R.string.ical_recurrence_hourly_interval, recurrence.getInterval());
                }
            case DAILY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_daily);
                } else {
                    return resources.getString(R.string.ical_recurrence_daily_interval, recurrence.getInterval());
                }
            case WEEKLY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_weekly);
                } else {
                    return resources.getString(R.string.ical_recurrence_weekly_interval, recurrence.getInterval());
                }
            case MONTHLY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_monthly);
                } else {
                    return resources.getString(R.string.ical_recurrence_monthly_interval, recurrence.getInterval());
                }
            case YEARLY:
                if (recurrence.getInterval() == 1) {
                    return resources.getString(R.string.ical_recurrence_yearly);
                } else {
                    return resources.getString(R.string.ical_recurrence_yearly_interval, recurrence.getInterval());
                }
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
