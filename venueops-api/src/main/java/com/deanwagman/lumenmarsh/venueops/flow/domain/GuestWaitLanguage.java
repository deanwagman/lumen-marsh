package com.deanwagman.lumenmarsh.venueops.flow.domain;

import java.util.Locale;

public final class GuestWaitLanguage {

    private GuestWaitLanguage() {
    }

    public static String aboutMinutes(int minutes) {
        int rounded = (int) Math.round(Math.max(0, minutes) / 5.0) * 5;
        if (rounded <= 0) {
            return "About 5 minutes";
        }
        return "About " + rounded + " minutes";
    }

    public static String direction(QueueTrend trend) {
        if (trend == null) {
            return "LIKELY_STABLE";
        }
        return switch (trend) {
            case RISING -> "LIKELY_RISING";
            case FALLING -> "LIKELY_FALLING";
            case STABLE -> "LIKELY_STABLE";
        };
    }

    public static String guestTrendLabel(QueueTrend trend) {
        return switch (direction(trend)) {
            case "LIKELY_RISING" -> "Likely rising";
            case "LIKELY_FALLING" -> "Likely falling";
            default -> "Likely stable";
        };
    }

    public static String availability(boolean operating) {
        return operating ? "OPERATING" : "UNAVAILABLE";
    }

    public static String explainNext(String displayName, int postedWait, QueueTrend trend) {
        return displayName + " is a good next choice. Its current wait is "
                + postedWait + " minutes and is expected to remain "
                + guestTrendLabel(trend).toLowerCase(Locale.ROOT).replace("likely ", "") + ".";
    }
}
