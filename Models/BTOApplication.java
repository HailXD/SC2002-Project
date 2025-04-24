/**
 * Represents a single BTO (Build-To-Order) application submitted by an applicant for a specific project.
 *
 * @author Jun Yang
 */
package Models;

import Enums.ApplicationStatus;
import Enums.FlatType;

import java.util.Date;

public class BTOApplication {
    private final String applicationId;
    private final String applicantNric;
    private final String projectName;
    private FlatType flatTypeApplied;
    private ApplicationStatus status;
    private final Date applicationDate;
    private ApplicationStatus statusBeforeWithdrawal;

    /**
     * Constructs a new BTOApplication when an applicant first applies.
     * The application ID is automatically generated based on NRIC and project name.
     * Initial status is set to PENDING.
     *
     * @param applicantNric   The NRIC of the applicant. Cannot be null.
     * @param projectName     The name of the project being applied for. Cannot be
     *                        null.
     * @param flatTypeApplied The type of flat applied for. Cannot be null.
     * @param applicationDate The date the application was submitted. Cannot be
     *                        null.
     * @throws IllegalArgumentException if any parameter is null.
     */
    public BTOApplication(String applicantNric, String projectName, FlatType flatTypeApplied, Date applicationDate) {
        if (applicantNric == null || projectName == null || flatTypeApplied == null || applicationDate == null) {
            throw new IllegalArgumentException("BTOApplication fields cannot be null");
        }
        this.applicationId = applicantNric + "_" + projectName;
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.flatTypeApplied = flatTypeApplied;
        this.status = ApplicationStatus.PENDING;
        this.applicationDate = applicationDate;
        this.statusBeforeWithdrawal = null;
    }

    /**
     * Constructs a BTOApplication object when loading data from storage.
     * Allows setting all fields, including the application ID and status.
     * Includes warnings for potentially inconsistent data (e.g., BOOKED status with
     * null flat type).
     *
     * @param applicationId   The unique ID of the application. Cannot be null.
     * @param applicantNric   The NRIC of the applicant. Cannot be null.
     * @param projectName     The name of the project. Cannot be null.
     * @param flatTypeApplied The type of flat applied for. Can be null but issues
     *                        warning if status implies it shouldn't be.
     * @param status          The status of the application. Cannot be null.
     * @param applicationDate The date the application was submitted. Cannot be
     *                        null.
     * @throws IllegalArgumentException if required fields are null.
     */
    public BTOApplication(String applicationId, String applicantNric, String projectName, FlatType flatTypeApplied,
            ApplicationStatus status, Date applicationDate) {
        if (applicationId == null || applicantNric == null || projectName == null || status == null
                || applicationDate == null) {
            throw new IllegalArgumentException("Required BTOApplication fields cannot be null when loading");
        }
        if (flatTypeApplied == null && (status == ApplicationStatus.BOOKED || status == ApplicationStatus.SUCCESSFUL
                || status == ApplicationStatus.PENDING_WITHDRAWAL)) {
            System.err.println(
                    "Warning: Loading " + status + " application (" + applicationId + ") with null flatTypeApplied.");
        }

        this.applicationId = applicationId;
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.flatTypeApplied = flatTypeApplied;
        this.status = status;
        this.applicationDate = applicationDate;
        this.statusBeforeWithdrawal = null;
        if (status == ApplicationStatus.PENDING_WITHDRAWAL) {
            System.err.println("Warning: Application " + applicationId
                    + " loaded with PENDING_WITHDRAWAL status. Original status before withdrawal is unknown. Reverting will assume PENDING/SUCCESSFUL based on flat type.");
        }
    }

    /**
     * Get the application ID.
     * 
     * @return The application ID string.
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Get the NRIC of the applicant.
     * 
     * @return The applicant's NRIC string.
     */
    public String getApplicantNric() {
        return applicantNric;
    }

    /**
     * Get the name of the project applied for.
     * 
     * @return The project name string.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Get the type of flat applied for.
     * 
     * @return The flat type enum.
     */
    public FlatType getFlatTypeApplied() {
        return flatTypeApplied;
    }

    /**
     * Get the current status of the application.
     * 
     * @return The application status enum.
     */

    public ApplicationStatus getStatus() {
        return status;
    }

    /**
     * Get the date the application was submitted.
     * 
     * @return The application date.
     */
    public Date getApplicationDate() {
        return applicationDate;
    }

    /**
     * Get the status of the application before withdrawal.
     * 
     * @return The status before withdrawal.
     */
    public ApplicationStatus getStatusBeforeWithdrawal() {
        return statusBeforeWithdrawal;
    }

    /**
     * Sets the status of the application.
     * 
     * @param status The new application status. Cannot be null.
     * @throws IllegalArgumentException if status is null.
     */
    public void setStatus(ApplicationStatus status) {
        if (status != null) {
            if (status == ApplicationStatus.PENDING_WITHDRAWAL && this.status != ApplicationStatus.PENDING_WITHDRAWAL) {
                this.statusBeforeWithdrawal = this.status;
            }
            if (this.status == ApplicationStatus.PENDING_WITHDRAWAL && status != ApplicationStatus.PENDING_WITHDRAWAL) {
                this.statusBeforeWithdrawal = null;
            }
            this.status = status;
        }
    }

}
