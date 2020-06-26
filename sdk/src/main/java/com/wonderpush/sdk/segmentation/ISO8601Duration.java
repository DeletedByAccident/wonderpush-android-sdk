package com.wonderpush.sdk.segmentation;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISO8601Duration {

    public final boolean positive;
    public final float years;
    public final float months;
    public final float weeks;
    public final float days;
    public final float hours;
    public final float minutes;
    public final float seconds;

    // "\\d+(?:(?:[.,])\\d*)?" matches a float number
    public static final Pattern PATTERN = Pattern.compile("^([+-])?P(\\d+(?:(?:[.,])\\d*)?Y)?(\\d+(?:(?:[.,])\\d*)?M)?(\\d+(?:(?:[.,])\\d*)?W)?(\\d+(?:(?:[.,])\\d*)?D)?(?:T(\\d+(?:(?:[.,])\\d*)?H)?(\\d+(?:(?:[.,])\\d*)?M)?(\\d+(?:(?:[.,])\\d*)?S)?)?$");

    public ISO8601Duration(boolean positive, float years, float months, float weeks, float days, float hours, float minutes, float seconds) {
        this.positive = positive;
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(positive ? '+' : '-');
        sb.append('P');
        sb.append(years);
        sb.append('Y');
        sb.append(months);
        sb.append('M');
        sb.append(weeks);
        sb.append('W');
        sb.append(days);
        sb.append('D');
        sb.append('T');
        sb.append(hours);
        sb.append('H');
        sb.append(minutes);
        sb.append('M');
        sb.append(seconds);
        sb.append('S');
        return sb.toString();
    }

    public static ISO8601Duration parse(String input) throws BadInputError {
        if (input == null) {
            throw new BadInputError("\"PT\" ISO 8601 duration expects a string");
        }
        Matcher matcher = PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new BadInputError("invalid \"PT\" ISO 8601 duration given");
        }
        boolean positive = !"-".equals(matcher.group(1));
        float years = getPart(matcher, 2);
        float months = getPart(matcher, 3);
        float weeks = getPart(matcher, 4);
        float days = getPart(matcher, 5);
        float hours = getPart(matcher, 6);
        float minutes = getPart(matcher, 7);
        float seconds = getPart(matcher, 8);
        return new ISO8601Duration(positive, years, months, weeks, days, hours, minutes, seconds);
    }

    private static float getPart(Matcher matcher, int group) {
        String text = matcher.group(group);
        if (text == null) {
            return 0;
        }
        // Remove unit
        text = text.substring(0, text.length() - 1);
        // Parse number
        try {
            return Float.parseFloat(text.replaceAll(",", "."));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public long applyTo(long date) {
        Calendar rtn = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        rtn.setTimeInMillis(date);
        float remainder = 0;
        int sign = this.positive ? 1 : -1;
        int yearsInt = (int) (this.years + remainder);
        rtn.add(Calendar.YEAR, sign * yearsInt);
        remainder = this.years + remainder - yearsInt;
        remainder *= 12;
        int monthsInt = (int) (this.months + remainder);
        rtn.add(Calendar.MONTH, sign * monthsInt);
        remainder = this.months + remainder - monthsInt;
        remainder *= rtn.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysInt = (int) (this.days + this.weeks * 7 + remainder);
        rtn.add(Calendar.DAY_OF_MONTH, sign * daysInt);
        remainder = this.days + this.weeks * 7 + remainder - daysInt;
        remainder *= 24;
        int hoursInt = (int) (this.hours + remainder);
        rtn.add(Calendar.HOUR_OF_DAY, sign * hoursInt);
        remainder = this.hours + remainder - hoursInt;
        remainder *= 60;
        int minutesInt = (int) (this.minutes + remainder);
        rtn.add(Calendar.MINUTE, sign * minutesInt);
        remainder = this.minutes + remainder - minutesInt;
        remainder *= 60;
        int secondsInt = (int) (this.seconds + remainder);
        rtn.add(Calendar.SECOND, sign * secondsInt);
        remainder = this.seconds + remainder - secondsInt;
        remainder *= 1000;
        int milliSecondsInt = (int) (0 + remainder);
        rtn.add(Calendar.MILLISECOND, sign * milliSecondsInt);
        return rtn.getTimeInMillis();
    }

}
