/**
 * Interface defining the contract for BTO application data management services.
 * Specifies methods for loading, saving, finding, retrieving, adding, and removing applications.
 *
 * @author Kishore Kumar
 */
package Services;

import java.util.List;
import java.util.Map;
import Models.BTOApplication;
import Models.Project;
import Enums.ApplicationStatus;

public interface IApplicationService {
    /**
     * Loads BTO application data from persistent storage (e.g., CSV file).
     * 
     * @param projects A list of all Project objects, used for unit count
     *                 adjustments.
     * @return A map of ApplicationID to BTOApplication objects representing all
     *         loaded applications.
     */
    Map<String, BTOApplication> loadApplications(List<Project> projects);

    /**
     * Saves the provided map of BTO applications to persistent storage.
     * Overwrites the existing application data file.
     * 
     * @param applications A map of ApplicationID to BTOApplication objects to be
     *                     saved.
     */
    void saveApplications(Map<String, BTOApplication> applications);

    /**
     * Finds a BTO application by its unique ID.
     * 
     * @param applicationId The ID of the application to find.
     * @return The BTOApplication object if found, or null otherwise.
     */
    BTOApplication findApplicationById(String applicationId);

    /**
     * Finds a specific BTO application based on the applicant's NRIC and the
     * project name.
     * Assumes a standard Application ID format (e.g., NRIC_ProjectName).
     * 
     * @param nric        The NRIC of the applicant.
     * @param projectName The name of the project.
     * @return The BTOApplication object if found, or null otherwise.
     */
    BTOApplication findApplicationByApplicantAndProject(String nric, String projectName);

    /**
     * Retrieves a list of all BTO applications submitted for a specific project.
     * 
     * @param projectName The name of the project.
     * @return A list of BTOApplication objects for the specified project. Returns
     *         an empty list if projectName is null or no applications are found.
     */
    List<BTOApplication> getApplicationsByProject(String projectName);

    /**
     * Retrieves a list of all BTO applications that currently have a specific
     * status.
     * 
     * @param status The ApplicationStatus to filter by.
     * @return A list of BTOApplication objects with the specified status. Returns
     *         an empty list if status is null or no applications match.
     */
    List<BTOApplication> getApplicationsByStatus(ApplicationStatus status);

    /**
     * Retrieves a list of all BTO applications submitted by a specific applicant.
     * 
     * @param nric The NRIC of the applicant.
     * @return A list of BTOApplication objects submitted by the specified
     *         applicant. Returns an empty list if nric is null or no applications
     *         are found.
     */
    List<BTOApplication> getApplicationsByApplicant(String nric);

    /**
     * Adds a new BTO application to the service's internal map.
     * Should perform checks to prevent adding duplicates (based on application ID).
     * 
     * @param application The BTOApplication object to add.
     */
    void addApplication(BTOApplication application);

    /**
     * Removes a BTO application from the service's internal map based on its ID.
     * 
     * @param applicationId The ID of the application to remove.
     * @return true if the application was found and removed, false otherwise.
     */
    boolean removeApplication(String applicationId);

    /**
     * Retrieves a map containing all BTO applications currently managed by the
     * service.
     * 
     * @return A map of ApplicationID to BTOApplication objects.
     */
    Map<String, BTOApplication> getAllApplications();
}
