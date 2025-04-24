/**
 * Represents the types of flats available in BTO projects
 *
 * @author Kai Wang
 */
package Enums;

public enum FlatType {
    TWO_ROOM("2-Room"), THREE_ROOM("3-Room");

    private final String displayName;

    FlatType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the user-friendly display name for the flat type.
     * 
     * @return The display name (e.g., "2-Room").
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a string representation of a flat type (e.g., "2-Room") into the corresponding FlatType enum constant.
     *
     * @param text The string representation of the flat type.
     * @return The matching FlatType enum constant, or null if no match is found.
     */
    public static FlatType fromString(String text) {
        if (text != null) {
            for (FlatType b : FlatType.values()) {
                if (text.trim().equalsIgnoreCase(b.name()) || text.trim().equalsIgnoreCase(b.displayName)) {
                    return b;
                }
            }
            if ("2-Room".equalsIgnoreCase(text.trim()))
                return TWO_ROOM;
            if ("3-Room".equalsIgnoreCase(text.trim()))
                return THREE_ROOM;
        }
        return null;
    }
}