/**
 * Represents an Applicant user in the BTO system
 * Extends the base User class and adds attributes related to BTO applications
 * such as the project applied for, application status, and booked flat type
 *
 * @author Jun Yang
 */
package Models;

import Enums.MaritalStatus;
import Enums.UserRole;
import Enums.ApplicationStatus;
import Enums.FlatType;

public class Applicant extends User {
    private String appliedProjectName;
    private ApplicationStatus applicationStatus;
    private FlatType bookedFlatType;

    /**
     * Constructs a new Applicant object
     *
     * @param nric          The applicant's NRIC. Must not be null
     * @param password      The applicant's password. Must not be null
     * @param name          The applicant's name. Must not be null
     * @param age           The applicant's age
     * @param maritalStatus The applicant's marital status. Must not be null
     * @throws IllegalArgumentException if any required field is null
     */
    public Applicant(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        super(nric, password, name, age, maritalStatus);
        this.applicationStatus = null;
        this.appliedProjectName = null;
        this.bookedFlatType = null;
    }

    /**
     * Gets the user's role
     * 
     * @return The user role enum
     */
    @Override
    public UserRole getRole() {
        return UserRole.APPLICANT;
    }

    /**
     * Gets the name of the project the applicant has applied for
     * 
     * @return The project name string
     */
    public String getAppliedProjectName() {
        return appliedProjectName;
    }

    /**
     * Sets the name of the project the applicant has applied for
     * 
     * @param appliedProjectName The project name string
     */
    public void setAppliedProjectName(String appliedProjectName) {
        this.appliedProjectName = appliedProjectName;
    }

    /**
     * Gets the application status of the applicant
     * 
     * @return The application status enum
     */
    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    /**
     * Sets the application status of the applicant
     * 
     * @param applicationStatus The application status enum
     */
    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    /**
     * Gets the booked flat type of the applicant
     * 
     * @return The booked flat type enum
     */
    public FlatType getBookedFlatType() {
        return bookedFlatType;
    }

    /**
     * Sets the booked flat type of the applicant
     * 
     * @param bookedFlatType The booked flat type enum
     */
    public void setBookedFlatType(FlatType bookedFlatType) {
        this.bookedFlatType = bookedFlatType;
    }

    /**
     * Checks if the applicant has an active application
     * 
     * @return true if the application status is PENDING or SUCCESSFUL, false
     *         otherwise
     */
    public boolean hasActiveApplication() {
        return this.applicationStatus == ApplicationStatus.PENDING ||
                this.applicationStatus == ApplicationStatus.SUCCESSFUL;
    }

    /**
     * Check if the applicant has a pending withdrawal
     *
     * @return true if the application status is PENDING_WITHDRAWAL, false otherwise
     */
    public boolean hasPendingWithdrawal() {
        return this.applicationStatus == ApplicationStatus.PENDING_WITHDRAWAL;
    }

    /**
     * Checks if the applicant has booked a flat
     * 
     * @return true if the application status is BOOKED and booked flat type is not
     *         null, false otherwise
     */
    public boolean hasBooked() {
        return this.applicationStatus == ApplicationStatus.BOOKED && this.bookedFlatType != null;
    }

    /**
     * Clears the application state of the applicant
     * Resets the applied project name, application status, and booked flat type to
     * null
     */
    public void clearApplicationState() {
        this.appliedProjectName = null;
        this.applicationStatus = null;
        this.bookedFlatType = null;
    }
}