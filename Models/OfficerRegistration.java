/**
 * Represents a registration request made by an HDB Officer to handle a specific BTO project.
 *
 * @author Jun Yang
 */
package Models;

import Enums.OfficerRegistrationStatus;
import java.util.Date;

public class OfficerRegistration {
    private final String registrationId;
    private final String officerNric;
    private final String projectName;
    private OfficerRegistrationStatus status;
    private final Date registrationDate;

    /**
     * Constructs a new OfficerRegistration when an officer first registers for a
     * project.
     * The registration ID is automatically generated based on NRIC and project
     * name.
     * Initial status is set to PENDING.
     *
     * @param officerNric      The NRIC of the officer. Cannot be null.
     * @param projectName      The name of the project being registered for. Cannot
     *                         be null.
     * @param registrationDate The date the registration was submitted. Cannot be
     *                         null.
     * @throws IllegalArgumentException if any parameter is null.
     */
    public OfficerRegistration(String officerNric, String projectName, Date registrationDate) {
        if (officerNric == null || projectName == null || registrationDate == null) {
            throw new IllegalArgumentException("OfficerRegistration fields cannot be null");
        }
        this.registrationId = officerNric + "_REG_" + projectName;
        this.officerNric = officerNric;
        this.projectName = projectName;
        this.status = OfficerRegistrationStatus.PENDING;
        this.registrationDate = registrationDate;
    }

    /**
     * Constructs a new OfficerRegistration when an officer first registers for a
     * project.
     * The registration ID is automatically generated based on NRIC and project
     * name.
     * Initial status is set to PENDING.
     *
     * @param officerNric      The NRIC of the HDB Officer registering. Cannot be
     *                         null.
     * @param projectName      The name of the project being registered for. Cannot
     *                         be null.
     * @param registrationDate The date the registration was submitted. Cannot be
     *                         null.
     * @throws IllegalArgumentException if any parameter is null.
     */
    public OfficerRegistration(String registrationId, String officerNric, String projectName,
            OfficerRegistrationStatus status, Date registrationDate) {
        if (registrationId == null || officerNric == null || projectName == null || status == null
                || registrationDate == null) {
            throw new IllegalArgumentException("Required OfficerRegistration fields cannot be null when loading");
        }
        this.registrationId = registrationId;
        this.officerNric = officerNric;
        this.projectName = projectName;
        this.status = status;
        this.registrationDate = registrationDate;
    }

    /**
     * Gets the registration ID.
     *
     * @return The registration ID string.
     */
    public String getRegistrationId() {
        return registrationId;
    }

    /**
     * Gets the NRIC of the officer.
     *
     * @return The officer's NRIC string.
     */
    public String getOfficerNric() {
        return officerNric;
    }

    /**
     * Gets the name of the project the officer is registering for.
     *
     * @return The project name string.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets the status of the registration.
     *
     * @return The registration status enum.
     */
    public OfficerRegistrationStatus getStatus() {
        return status;
    }

    /**
     * Gets the date the registration was submitted.
     *
     * @return The registration date.
     */
    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Sets the status of the registration.
     *
     * @param status The registration status enum. Cannot be null.
     */
    public void setStatus(OfficerRegistrationStatus status) {
        if (status != null) {
            this.status = status;
        }
    }
}
