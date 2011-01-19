/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.dom.datetime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTime {
    private final Date date;
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;
    private final int timeZone;

    public DateTime(String yearString, int month, int day, int hour, int minute, int second, int timeZone) {
        this.year = convertToYear(yearString);
        this.date = convertToDate(year, month, day, hour, minute, second, timeZone);
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.timeZone = timeZone;
    }

    private int convertToYear(String yearString) {
        int year = Integer.parseInt(yearString);
        switch (yearString.length()) {
            case 1:
            case 2:
                if (year >= 0 && year < 50)
                    return 2000 + year;
                else
                    return 1900 + year;
            case 3:
                return 1900 + year;
            default:
                return year;
        }
    }

    public static Date convertToDate(int year, int month, int day, int hour, int minute, int second, int timeZone) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        c.set(year, month - 1, day, hour, minute, second);
        c.set(Calendar.MILLISECOND, 0);

        if (timeZone != Integer.MIN_VALUE) {
            int minutes = ((timeZone / 100) * 60) + timeZone % 100;
            c.add(Calendar.MINUTE, -1 * minutes);
        }

        return c.getTime();
    }

    public Date getDate() {
        return date;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getTimeZone() {
        return timeZone;
    }

    public void print() {
        System.out.println(toString());
    }

    @Override
    public String toString() {
        return getYear() + " " + getMonth() + " " + getDay() + "; " + getHour() + " " + getMinute() + " " + getSecond() + " " + getTimeZone();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((date == null) ? 0 : date.hashCode());
        result = PRIME * result + day;
        result = PRIME * result + hour;
        result = PRIME * result + minute;
        result = PRIME * result + month;
        result = PRIME * result + second;
        result = PRIME * result + timeZone;
        result = PRIME * result + year;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DateTime other = (DateTime) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (day != other.day)
            return false;
        if (hour != other.hour)
            return false;
        if (minute != other.minute)
            return false;
        if (month != other.month)
            return false;
        if (second != other.second)
            return false;
        if (timeZone != other.timeZone)
            return false;
        if (year != other.year)
            return false;
        return true;
    }


}
