/**
 * Represents the details associated with a specific flat type within a BTO project,
 * including the total number of units, the number currently available, and the selling price.
 *
 * @author Jun Yang
 */

package Models;

public class FlatTypeDetails {
    private final int totalUnits;
    private int availableUnits;
    private double sellingPrice;

    /**
     * Constructor for FlatTypeDetails.
     *
     * @param totalUnits     The total number of units available for this flat type
     * @param availableUnits The number of units currently available for sale
     * @param sellingPrice   The selling price of the flat type
     * @throws IllegalArgumentException if any of the parameters are invalid
     */
    public FlatTypeDetails(int totalUnits, int availableUnits, double sellingPrice) {
        if (totalUnits < 0 || availableUnits < 0 || availableUnits > totalUnits || sellingPrice < 0) {
            throw new IllegalArgumentException("Invalid FlatTypeDetails parameters: total=" + totalUnits
                    + ", available=" + availableUnits + ", price=" + sellingPrice);
        }
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
        this.sellingPrice = sellingPrice;
    }

    /**
     * Gets the total number of units for this flat type.
     * 
     * @return The total unit count.
     */
    public int getTotalUnits() {
        return totalUnits;
    }

    /**
     * Gets the number of available units for this flat type.
     * 
     * @return The available unit count.
     */

    public int getAvailableUnits() {
        return availableUnits;
    }

    /**
     * Gets the selling price of this flat type.
     * 
     * @return The selling price.
     */

    public double getSellingPrice() {
        return sellingPrice;
    }

    /**
     * Sets the selling price of this flat type.
     * 
     * @param sellingPrice The new selling price.
     */
    public void setSellingPrice(double sellingPrice) {
        if (sellingPrice >= 0) {
            this.sellingPrice = sellingPrice;
        } else {
            System.err.println("Warning: Attempted to set negative selling price.");
        }
    }

    /**
     * Decrements the number of available units by one.
     * 
     * @return true if the decrement was successful, false if no units were
     *         available to decrement.
     */
    public boolean decrementAvailableUnits() {
        if (this.availableUnits > 0) {
            this.availableUnits--;
            return true;
        } else {
            System.err.println("Warning: Attempted to decrement zero available units.");
            return false;
        }
    }

    /**
     * Increments the number of available units by one.
     * 
     * @return true if the increment was successful, false if it would exceed total
     *         units.
     */
    public boolean incrementAvailableUnits() {
        if (this.availableUnits < this.totalUnits) {
            this.availableUnits++;
            return true;
        } else {
            System.err.println("Warning: Attempted to increment available units beyond total units.");
            return false;
        }
    }

    /**
     * Sets the number of available units for this flat type.
     * 
     * @param availableUnits The new number of available units.
     */
    public void setAvailableUnits(int availableUnits) {
        if (availableUnits >= 0 && availableUnits <= this.totalUnits) {
            this.availableUnits = availableUnits;
        } else {
            System.err.println("Error: Invalid available units (" + availableUnits + ") set for flat type with total "
                    + this.totalUnits + ". Clamping to valid range [0, " + this.totalUnits + "].");
            this.availableUnits = Math.max(0, Math.min(availableUnits, this.totalUnits));
        }
    }
}