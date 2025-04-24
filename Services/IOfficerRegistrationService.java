/**
 * Interface defining the contract for HDB Officer project registration management services.
 * Specifies methods for loading, saving, finding, retrieving, adding, and removing registrations.
 *
 * @author Kishore Kumar
 */
package Services;

import java.util.List;
import java.util.Map;
import Models.OfficerRegistration;
import Models.Project;
import Models.User;
import Enums.OfficerRegistrationStatus;

public interface IOfficerRegistrationService {
    /**
     * Loads officer registration data from persistent storage (e.g., CSV file).
     * Requires user and project data for validation (e.g., checking officer NRIC,
     * project existence).
     * Populates the service's internal registration map.
     * 
     * @param users    A map of NRIC to User objects, used for validating officer
     *                 NRICs.
     * @param projects A list of all Project objects, used for validating project
     *                 names.
     * @return A map of RegistrationID to OfficerRegistration objects representing
     *         all loaded registrations.
     */
    Map<String, OfficerRegistration> loadOfficerRegistrations(Map<String, User> users, List<Project> projects);

    /**
     * Saves the provided map of officer registrations to persistent storage.
     * Overwrites the existing registration data file.
     * 
     * @param registrations A map of RegistrationID to OfficerRegistration objects
     *                      to be saved.
     */
    void saveOfficerRegistrations(Map<String, OfficerRegistration> registrations);

    /**
     * Finds an officer registration by its unique ID.
     * 
     * @param registrationId The ID of the registration to find.
     * @return The OfficerRegistration object if found, or null otherwise.
     */
    OfficerRegistration findRegistrationById(String registrationId);

    /**
     * Retrieves a list of all registrations (regardless of status) made by a
     * specific HDB Officer.
     * 
     * @param officerNric The NRIC of the officer.
     * @return A list of OfficerRegistration objects for the specified officer.
     *         Returns an empty list if NRIC is null or no registrations are found.
     */
    List<OfficerRegistration> getRegistrationsByOfficer(String officerNric);

    /**
     * Retrieves a list of all registrations (regardless of status) associated with
     * a specific project.
     * 
     * @param projectName The name of the project.
     * @return A list of OfficerRegistration objects for the specified project.
     *         Returns an empty list if projectName is null or no registrations are
     *         found.
     */
    List<OfficerRegistration> getRegistrationsByProject(String projectName);

    /**
     * Retrieves a list of all officer registrations that currently have a specific
     * status.
     * 
     * @param status The OfficerRegistrationStatus to filter by.
     * @return A list of OfficerRegistration objects with the specified status.
     *         Returns an empty list if status is null or no registrations match.
     */
    List<OfficerRegistration> getRegistrationsByStatus(OfficerRegistrationStatus status);

    /**
     * Finds the specific registration record indicating which project an officer is
     * currently approved to handle.
     * Assumes an officer can only be actively approved for one project at a time.
     * 
     * @param officerNric The NRIC of the officer.
     * @return The OfficerRegistration object with APPROVED status for the officer,
     *         or null if none is found.
     */
    OfficerRegistration getApprovedRegistrationForOfficer(String officerNric);

    /**
     * Adds a new officer registration to the service's internal map.
     * Should perform checks to prevent adding duplicates (based on registration
     * ID).
     * 
     * @param registration The OfficerRegistration object to add.
     */
    void addRegistration(OfficerRegistration registration);

    /**
     * Removes an officer registration from the service's internal map based on its
     * ID.
     * 
     * @param registrationId The ID of the registration to remove.
     * @return true if the registration was found and removed, false otherwise.
     */
    boolean removeRegistration(String registrationId);

    /**
     * Retrieves a map containing all officer registrations currently managed by the
     * service.
     * Implementations should consider returning a copy.
     * 
     * @return A map of RegistrationID to OfficerRegistration objects.
     */
    Map<String, OfficerRegistration> getAllRegistrations();
}
