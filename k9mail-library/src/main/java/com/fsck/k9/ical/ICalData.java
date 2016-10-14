package com.fsck.k9.ical;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.parameter.ParticipationLevel;
import biweekly.parameter.ParticipationStatus;
import biweekly.property.Attendee;
import biweekly.property.Method;
import biweekly.property.Organizer;
import biweekly.property.RecurrenceRule;

public class ICalData {
    private Method method;
    private String summary;
    private Organizer organizer;
    private String location;
    private long date;
    private Attendee[] optional;
    private Attendee[] required;
    private Attendee[] fyi;
    private Attendee[] accepted;
    private Attendee[] confirmed;
    private Attendee[] declined;
    private Attendee[] needsAction;
    private Attendee[] sent;
    private Attendee[] tentative;
    private Attendee[] delegated;
    private RecurrenceRule recurrenceRule;

    public ICalData(ICalendar iCal) {
        method = iCal.getMethod();

        //TODO: Handle more than one event
        if (iCal.getEvents().size() > 0) {
            updateContentsFromEvent(iCal.getEvents().get(0));
        } else {
            System.err.println("No events in iCal file");
        }
    }

    private void updateContentsFromEvent(VEvent event) {
        summary = event.getSummary().getValue();
        organizer = event.getOrganizer();
        location = event.getLocation().getValue();

        if(event.getDateStart() != null) {
            date = event.getDateStart().getValue().getTime();
        }

        List<Attendee> attendees = event.getAttendees();

        List<Attendee> requiredList = new ArrayList<>();
        List<Attendee> optionalList = new ArrayList<>();
        List<Attendee> fyiList = new ArrayList<>();

        List<Attendee> acceptedList = new ArrayList<>();
        List<Attendee> declinedList = new ArrayList<>();
        List<Attendee> confirmedList = new ArrayList<>();
        List<Attendee> inProcessList = new ArrayList<>();
        List<Attendee> completedList = new ArrayList<>();
        List<Attendee> delegatedList = new ArrayList<>();
        List<Attendee> sentList = new ArrayList<>();
        List<Attendee> needsActionList = new ArrayList<>();
        List<Attendee> tentativeList = new ArrayList<>();

        for(Attendee attendee: attendees) {
            if (attendee.getParticipationLevel() != null) {
                if (attendee.getParticipationLevel().equals(ParticipationLevel.REQUIRED)) {
                    requiredList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationLevel.OPTIONAL)) {
                    optionalList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationLevel.FYI)) {
                    fyiList.add(attendee);
                }
            }
            if (attendee.getParticipationStatus() != null) {
                if (attendee.getParticipationStatus().equals(ParticipationStatus.ACCEPTED)) {
                    acceptedList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.CONFIRMED)) {
                    confirmedList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.DECLINED)) {
                    declinedList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.IN_PROCESS)) {
                    inProcessList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.COMPLETED)) {
                    completedList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.DELEGATED)) {
                    delegatedList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.SENT)) {
                    sentList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.NEEDS_ACTION)) {
                    needsActionList.add(attendee);
                } else if (attendee.getParticipationLevel().equals(ParticipationStatus.TENTATIVE)) {
                    tentativeList.add(attendee);
                }
            }
        }

        required = requiredList.toArray(new Attendee[requiredList.size()]);
        optional = optionalList.toArray(new Attendee[optionalList.size()]);
        fyi = fyiList.toArray(new Attendee[fyiList.size()]);
        recurrenceRule = event.getRecurrenceRule();

        accepted = acceptedList.toArray(new Attendee[acceptedList.size()]);
        confirmed = confirmedList.toArray(new Attendee[confirmedList.size()]);
        declined = declinedList.toArray(new Attendee[declinedList.size()]);
        needsAction = needsActionList.toArray(new Attendee[needsActionList.size()]);
        sent = sentList.toArray(new Attendee[sentList.size()]);
        delegated = delegatedList.toArray(new Attendee[delegatedList.size()]);
        tentative = tentativeList.toArray(new Attendee[tentativeList.size()]);

    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public Attendee[] getRequired() {
        return required;
    }

    public Attendee[] getOptional() {
        return optional;
    }

    public Attendee[] getFyi() {
        return fyi;
    }

    public long getDate() {
        return date;
    }

    public String getDateTime() {
        return new SimpleDateFormat().format(new Date(date));
    }

    public String getLocation() {
        return location;
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public String getSummary() {
        return summary;
    }

    public Attendee[] getAccepted() {
        return accepted;
    }

    public Attendee[] getConfirmed() {
        return confirmed;
    }

    public Attendee[] getDeclined() {
        return declined;
    }

    public Attendee[] getNeedsAction() {
        return needsAction;
    }

    public Attendee[] getSent() {
        return sent;
    }

    public Attendee[] getTentative() {
        return tentative;
    }

    public Attendee[] getDelegated() {
        return delegated;
    }

    public Method getMethod() {
        return method;
    }
}
