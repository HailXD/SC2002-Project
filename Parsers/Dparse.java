/**
 * Utility class for parsing date strings into Date objects.
 * Uses a predefined format ("yyyy-MM-dd").
 *
 * @author Jordon
 */
package Parsers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dparse {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Parses a date string in "yyyy-MM-dd" format into a Date object.
     * Handles null, empty, or "null" strings by returning null.
     * Prints a warning to stderr if the format is invalid.
     * Uses strict parsing (setLenient(false)).
     *
     * @param dateString The date string to parse.
     * @return A Date object representing the parsed date, or null if parsing fails
     *         or input is invalid.
     */
    public static Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty() || dateString.trim().equalsIgnoreCase("null")) {
            return null;
        }
        try {
            DATE_FORMAT.setLenient(false);
            return DATE_FORMAT.parse(dateString.trim());
        } catch (ParseException e) {
            System.err.println("Warning: Invalid date format encountered: '" + dateString + "'. Expected yyyy-MM-dd.");
            return null;
        }
    }
}
