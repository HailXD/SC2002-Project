/**
 * Interface defining the contract for BTO project data management services.
 * Specifies methods for loading, saving, finding, retrieving, adding, and removing projects.
 *
 * @author Kishore Kumar
 */
package Services;

import java.util.List;
import java.util.Map;
import Models.Project;
import Models.User;

public interface IProjectService {
    /**
     * Loads project data from persistent storage (e.g., CSV file).
     * Requires user data for validation (e.g., checking manager NRIC).
     * Populates the service's internal project list.
     * 
     * @param users A map of NRIC to User objects, used for validation.
     * @return A list of loaded Project objects.
     */
    List<Project> loadProjects(Map<String, User> users);

    /**
     * Saves the provided list of projects to persistent storage.
     * Overwrites the existing project data file.
     * 
     * @param projects The list of Project objects to be saved.
     */
    void saveProjects(List<Project> projects);

    /**
     * Finds a project by its unique name (case-insensitive).
     * 
     * @param name The name of the project to find.
     * @return The Project object if found, or null otherwise.
     */
    Project findProjectByName(String name);

    /**
     * Retrieves a list containing all projects currently managed by the service.
     * Implementations should consider returning a copy.
     * 
     * @return A list of all Project objects.
     */
    List<Project> getAllProjects();

    /**
     * Retrieves a list of projects managed by a specific HDB Manager.
     * 
     * @param managerNric The NRIC of the manager.
     * @return A list of projects managed by the specified manager. Returns an empty
     *         list if NRIC is null or no projects are found.
     */
    List<Project> getProjectsManagedBy(String managerNric);

    /**
     * Adds a new project to the service's internal list.
     * Should perform checks to prevent adding duplicates (based on project name).
     * 
     * @param project The Project object to add.
     */
    void addProject(Project project);

    /**
     * Removes a project
     * 
     * @param project The Project object to remove.
     */
    boolean removeProject(Project project);
}
