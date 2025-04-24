/**
 * Utility class for parsing strings containing lists of items separated by a delimiter.
 * Specifically designed to handle lists stored in CSV fields, potentially with quoting.
 *
 * @author Jordon
 */
package Parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LSparse {
    private static final String LIST_DELIMITER = ";";

    /**
     * Parses a string containing a list of items separated by a semicolon (';').
     * Handles cases where the entire list string might be enclosed in double quotes
     * (e.g., "\"item1;item2\"").
     * Trims whitespace from each resulting item and filters out empty strings.
     *
     * @param listString The string representation of the list (e.g., "item1; item2
     *                   ;item3" or "\"nric1;nric2\"").
     * @return A List of strings containing the parsed items, or an empty list if
     *         the input is null, empty, or contains no valid items.
     */
    public static List<String> parseListString(String listString) {
        if (listString == null || listString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String effectiveList = listString.trim();
        if (effectiveList.startsWith("\"") && effectiveList.endsWith("\"")) {
            effectiveList = effectiveList.substring(1, effectiveList.length() - 1).replace("\"\"", "\"");
        }
        return Arrays.stream(effectiveList.split(LIST_DELIMITER))
                .map(str -> str.trim())

                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
