package com.indiratrading.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class DataNormalizer {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    public static String normalizeIsin(String isin) {
        if (isin == null) return null;
        return isin.trim().toUpperCase();
    }

    public static String normalizeSymbol(String symbol) {
        if (symbol == null) return null;
        return symbol.trim().toUpperCase();
    }

    public static String normalizeClientId(String clientId) {
        if (clientId == null) return null;
        return clientId.trim().toUpperCase();
    }

    public static long parsePaise(Object value) {
        if (value == null) return 0;
        String str = value.toString().trim();

        boolean hasCurrencySymbol = str.contains("₹") || str.contains("Rs");

        // Handle "1,00,000" Indian format
        str = str.replace(",", "");

        // Handle "₹5000" or "Rs.5000"
        str = str.replace("₹", "").replace("Rs.", "").replace("Rs", "").trim();

        try {
            if (str.contains(".")) {
                // Has decimal point - treat as rupees, convert to paise
                BigDecimal rupees = new BigDecimal(str);
                return rupees.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
            }
            // No decimal point
            if (hasCurrencySymbol) {
                // Currency symbol present but no decimal - treat as rupees
                return Long.parseLong(str) * 100L;
            }
            // No currency symbol, no decimal - treat as paise (integer)
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot parse paise value: " + value, e);
        }
    }

    public static int parseQuantity(Object value) {
        if (value == null) return 0;
        String str = value.toString().trim().replace(",", "");
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot parse quantity: " + value, e);
        }
    }

    public static LocalDateTime parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();

        // Try multiple formats
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyy/MM/dd"
        };

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                if (format.equals("yyyy-MM-dd") || format.equals("dd/MM/yyyy") ||
                    format.equals("dd-MM-yyyy") || format.equals("MM/dd/yyyy") ||
                    format.equals("yyyy/MM/dd")) {
                    // Date-only format - parse as LocalDate then add default time
                    java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay();
                }
                if (format.contains("XXX") || format.contains("xxx")) {
                    return LocalDateTime.parse(dateStr, formatter);
                }
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new RuntimeException("Cannot parse date: " + dateStr);
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String quarantineRow(String reason, String rowId) {
        return String.format("ROW_ERROR: row=%s, reason=%s", rowId, reason);
    }
}
