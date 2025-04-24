/**
 * Utility class for validating the format of Singapore NRIC (National Registration Identity Card) numbers.
 * Uses a regular expression to check the pattern.
 *
 * @author Jordon
 */
package Utils;

import java.util.regex.Pattern;

public class NricValidator {
    private static final Pattern NRIC_PATTERN = Pattern.compile("^[STFG]\\d{7}[A-Z]$");

    /**
     * Validates if the given string matches the standard NRIC format.
     * The pattern checks for a starting character (S, T, F, G), followed by 7
     * digits, and ending with an uppercase letter.
     *
     * @param nric The NRIC string to validate.
     * @return true if the string matches the NRIC pattern, false otherwise
     *         (including if the input is null).
     */
    public static boolean isValidNric(String nric) {
        return nric == null ? false : NRIC_PATTERN.matcher(nric).matches();
    }
}
