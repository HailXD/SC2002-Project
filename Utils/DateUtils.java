/**
 * Utility class for common date-related operations.
 * Provides methods for getting the current date and formatting dates into strings.
 *
 * @author Jordon
 */
package Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Gets the current system date and time.
     * 
     * @return A new Date object representing the current moment.
     */
    public static Date getCurrentDate() {
        return new Date();
    }

    /**
     * Formats a Date object into a string using the "yyyy-MM-dd" pattern.
     * Uses strict formatting (setLenient(false)).
     *
     * @param date The Date object to format.
     * @return The formatted date string (e.g., "2023-10-27"), or an empty string if
     *         the input date is null.
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        DATE_FORMAT.setLenient(false);
        return DATE_FORMAT.format(date);
    }
}
