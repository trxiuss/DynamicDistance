package com.dynamicdistance.util;

public class TimeUtil {

    public static Long parseDurationToMillis(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        input = input.trim().toLowerCase();

        try {
            long multiplier = 1000L;
            String numberPart = input;

            if (input.endsWith("ms")) {
                multiplier = 1L;
                numberPart = input.substring(0, input.length() - 2);
            } else if (input.endsWith("s")) {
                multiplier = 1000L;
                numberPart = input.substring(0, input.length() - 1);
            } else if (input.endsWith("m")) {
                multiplier = 60L * 1000L;
                numberPart = input.substring(0, input.length() - 1);
            } else if (input.endsWith("h")) {
                multiplier = 60L * 60L * 1000L;
                numberPart = input.substring(0, input.length() - 1);
            } else if (input.endsWith("d")) {
                multiplier = 24L * 60L * 60L * 1000L;
                numberPart = input.substring(0, input.length() - 1);
            }

            long value = Long.parseLong(numberPart);
            return value > 0 ? value * multiplier : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatRemainingTime(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "0s";
        }

        long seconds = remainingMillis / 1000L;
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }
}