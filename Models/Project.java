/**
 * Represents a BTO (Build-To-Order) housing project.
 * Contains details such as name, neighborhood, flat types offered (with their details),
 * application period dates, managing HDB Manager, officer slot limits, approved officers,
 * and visibility status.
 *
 * @author Jun Yang
 */
package Models;

import Enums.FlatType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
    private String projectName;
    private String neighborhood;
    private Map<FlatType, FlatTypeDetails> flatTypes;
    private Date applicationOpeningDate;
    private Date applicationClosingDate;
    private final String managerNric;
    private int maxOfficerSlots;
    private final List<String> approvedOfficerNrics;
    private boolean visibility;

    /**
     * Constructs a new Project object with the specified parameters.
     *
     * @param projectName            The name of the project. Must not be null or
     *                               empty.
     * @param neighborhood           The neighborhood of the project. Must not be
     *                               null.
     * @param flatTypes              A map of flat types and their details. Must not
     *                               be null.
     * @param applicationOpeningDate The opening date for applications. Must not be
     *                               null.
     * @param applicationClosingDate The closing date for applications. Must not be
     *                               null and must be after opening date.
     * @param managerNric            The NRIC of the managing HDB Manager. Must not
     *                               be null.
     * @param maxOfficerSlots        The maximum number of officer slots. Must be
     *                               non-negative.
     * @param approvedOfficerNrics   A list of approved officer NRICs. Can be null
     *                               or empty.
     * @param visibility             The visibility status of the project.
     */
    public Project(String projectName, String neighborhood, Map<FlatType, FlatTypeDetails> flatTypes,
            Date applicationOpeningDate, Date applicationClosingDate, String managerNric,
            int maxOfficerSlots, List<String> approvedOfficerNrics, boolean visibility) {
        if (projectName == null || projectName.trim().isEmpty() || neighborhood == null ||
                flatTypes == null || applicationOpeningDate == null || applicationClosingDate == null ||
                managerNric == null || maxOfficerSlots < 0) {
            throw new IllegalArgumentException("Invalid project parameters");
        }
        if (applicationClosingDate.before(applicationOpeningDate)) {
            throw new IllegalArgumentException("Project closing date cannot be before opening date.");
        }
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.flatTypes = new HashMap<>(flatTypes);
        this.applicationOpeningDate = applicationOpeningDate;
        this.applicationClosingDate = applicationClosingDate;
        this.managerNric = managerNric;
        this.maxOfficerSlots = maxOfficerSlots;
        this.approvedOfficerNrics = new ArrayList<>(
                approvedOfficerNrics != null ? approvedOfficerNrics : Collections.emptyList());
        this.visibility = visibility;
    }

    /**
     * Gets the project name.
     *
     * @return The project name.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets the neighborhood of the project.
     *
     * @return The neighborhood.
     */
    public String getNeighborhood() {
        return neighborhood;
    }

    /**
     * Gets the map of flat types and their details.
     *
     * @return An unmodifiable map of flat types and their details.
     */
    public Map<FlatType, FlatTypeDetails> getFlatTypes() {
        return Collections.unmodifiableMap(flatTypes);
    }

    /**
     * Gets the application opening date.
     *
     * @return The application opening date.
     */
    public Date getApplicationOpeningDate() {
        return applicationOpeningDate;
    }

    /**
     * Gets the application closing date.
     *
     * @return The application closing date.
     */
    public Date getApplicationClosingDate() {
        return applicationClosingDate;
    }

    /**
     * Gets the NRIC of the managing HDB Manager.
     *
     * @return The manager's NRIC.
     */
    public String getManagerNric() {
        return managerNric;
    }

    /**
     * Gets the maximum number of officer slots.
     *
     * @return The maximum number of officer slots.
     */
    public int getMaxOfficerSlots() {
        return maxOfficerSlots;
    }

    /**
     * Gets the list of approved officer NRICs.
     *
     * @return An list of approved officer NRICs.
     */
    public List<String> getApprovedOfficerNrics() {
        return Collections.unmodifiableList(approvedOfficerNrics);
    }

    /**
     * Gets the visibility status of the project.
     *
     * @return The visibility status.
     */
    public boolean isVisible() {
        return visibility;
    }

    /**
     * Gets the number of remaining officer slots.
     *
     * @return The number of remaining officer slots.
     */
    public int getRemainingOfficerSlots() {
        return Math.max(0, maxOfficerSlots - approvedOfficerNrics.size());
    }

    /**
     * Sets the project name.
     * 
     * @param projectName
     */
    public void setProjectName(String projectName) {
        if (projectName != null && !projectName.trim().isEmpty()) {
            this.projectName = projectName.trim();
        }
    }

    /**
     * Sets the neighborhood of the project.
     * 
     * @param neighborhood
     */
    public void setNeighborhood(String neighborhood) {
        if (neighborhood != null) {
            this.neighborhood = neighborhood;
        }
    }

    /**
     * Sets the map of flat types and their details.
     * 
     * @param flatTypes
     */
    public void setFlatTypes(Map<FlatType, FlatTypeDetails> flatTypes) {
        if (flatTypes != null) {
            this.flatTypes = new HashMap<>(flatTypes);
        }
    }

    /**
     * Sets the application opening date.
     * 
     * @param applicationOpeningDate
     */
    public void setApplicationOpeningDate(Date applicationOpeningDate) {
        if (applicationOpeningDate != null) {
            this.applicationOpeningDate = applicationOpeningDate;
        }
    }

    /**
     * Sets the application closing date.
     * 
     * @param applicationClosingDate
     */
    public void setApplicationClosingDate(Date applicationClosingDate) {
        if (applicationClosingDate != null && (this.applicationOpeningDate == null
                || !applicationClosingDate.before(this.applicationOpeningDate))) {
            this.applicationClosingDate = applicationClosingDate;
        } else if (applicationClosingDate != null) {
            System.err.println("Warning: Closing date cannot be before opening date. Not updated.");
        }
    }

    /**
     * Sets the maximum number of officer slots.
     * 
     * @param maxOfficerSlots The maximum number of officer slots.
     */
    public void setMaxOfficerSlots(int maxOfficerSlots) {
        if (maxOfficerSlots >= this.approvedOfficerNrics.size() && maxOfficerSlots >= 0) {
            this.maxOfficerSlots = maxOfficerSlots;
        } else {
            System.err.println("Warning: Cannot set max officer slots (" + maxOfficerSlots
                    + ") below current approved count (" + this.approvedOfficerNrics.size() + "). Value not changed.");
        }
    }

    /**
     * Sets the visibility status of the project.
     * 
     * @param visibility The visibility status.
     */
    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    /**
     * Adds an approved officer NRIC to the project.
     * 
     * @param officerNric The NRIC of the officer to be added.
     */
    public boolean addApprovedOfficer(String officerNric) {
        if (officerNric != null && getRemainingOfficerSlots() > 0 && !approvedOfficerNrics.contains(officerNric)) {
            approvedOfficerNrics.add(officerNric);
            return true;
        }
        return false;
    }

    /**
     * Removes an approved officer NRIC from the project.
     * 
     * @param officerNric The NRIC of the officer to be removed.
     */
    public boolean removeApprovedOfficer(String officerNric) {
        boolean removed = approvedOfficerNrics.remove(officerNric);
        return removed;
    }

    /**
     * Checks if the application period is active based on the current date.
     *
     * @param currentDate The current date.
     * @return true if the application period is active, false otherwise.
     */
    public boolean isApplicationPeriodActive(Date currentDate) {
        if (currentDate == null || applicationOpeningDate == null || applicationClosingDate == null)
            return false;
        Calendar cal = Calendar.getInstance();
        cal.setTime(applicationClosingDate);
        cal.add(Calendar.DATE, 1);
        Date endOfDayClosing = cal.getTime();

        return !currentDate.before(applicationOpeningDate) && currentDate.before(endOfDayClosing);
    }

    /**
     * Checks if the project is active based on visibility and application period.
     *
     * @param currentDate The current date.
     * @return true if the project is active, false otherwise.
     */
    public boolean isActive(Date currentDate) {
        return this.isVisible() && this.isApplicationPeriodActive(currentDate);
    }

    /**
     * Checks if the application period has expired based on the current date.
     *
     * @param currentDate The current date.
     * @return true if the application period has expired, false otherwise.
     */
    public boolean isApplicationPeriodExpired(Date currentDate) {
        if (currentDate == null || applicationClosingDate == null)
            return false;
        Calendar cal = Calendar.getInstance();
        cal.setTime(applicationClosingDate);
        cal.add(Calendar.DATE, 1);
        Date endOfDayClosing = cal.getTime();
        return currentDate.after(endOfDayClosing);
    }

    /**
     * Gets the flat type details for a specific flat type.
     * 
     * @param type
     * @return The FlatTypeDetails object for the specified flat type, or null if
     *         not found.
     */
    public FlatTypeDetails getFlatTypeDetails(FlatType type) {
        return flatTypes.get(type);
    }

    /**
     * Gets the mutable flat type details for a specific flat type.
     * 
     * @param type
     * @return The FlatTypeDetails object for the specified flat type, or null if
     *         not found.
     */
    public FlatTypeDetails getMutableFlatTypeDetails(FlatType type) {
        return flatTypes.get(type);
    }
}
