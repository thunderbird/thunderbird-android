package com.fsck.k9;


import java.util.Calendar;
import java.util.GregorianCalendar;


class QuietTimeChecker {
    private final Clock clock;
    private final String quietTimeStart;
    private final String quietTimeEnd;


    QuietTimeChecker(Clock clock, String quietTimeStart, String quietTimeEnd) {
        this.clock = clock;
        this.quietTimeStart = quietTimeStart;
        this.quietTimeEnd = quietTimeEnd;
    }

    public boolean isQuietTime() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(clock.getTime());

        Integer startHour = Integer.parseInt(quietTimeStart.split(":")[0]);
        Integer startMinute = Integer.parseInt(quietTimeStart.split(":")[1]);
        Integer endHour = Integer.parseInt(quietTimeEnd.split(":")[0]);
        Integer endMinute = Integer.parseInt(quietTimeEnd.split(":")[1]);

        Integer now = (gregorianCalendar.get(Calendar.HOUR) * 60) + gregorianCalendar.get(Calendar.MINUTE);
        Integer quietStarts = startHour * 60 + startMinute;
        Integer quietEnds =  endHour * 60 + endMinute;

        // If start and end times are the same, we're never quiet
        if (quietStarts.equals(quietEnds)) {
            return false;
        }


        // 21:00 - 05:00 means we want to be quiet if it's after 9 or before 5
        if (quietStarts > quietEnds) {
            // if it's 22:00 or 03:00 but not 8:00
            if (now >= quietStarts || now <= quietEnds) {
                return true;
            }
        }

        // 01:00 - 05:00
        else {

            // if it' 2:00 or 4:00 but not 8:00 or 0:00
            if (now >= quietStarts && now <= quietEnds) {
                return true;
            }
        }

        return false;
    }
}
