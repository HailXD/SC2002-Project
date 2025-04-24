/**
 * Interface defining the contract for enquiry data management services.
 * Specifies methods for loading, saving, finding, retrieving, adding, and removing enquiries.
 *
 * @author Kishore Kumar
 */
package Services;

import java.util.List;
import Models.Enquiry;

public interface IEnquiryService {
    /**
     * Loads enquiry data from persistent storage (e.g., CSV file).
     * Populates the service's internal enquiry list.
     * 
     * @return A list of loaded Enquiry objects.
     */
    List<Enquiry> loadEnquiries();

    /**
     * Saves the provided list of enquiries to persistent storage.
     * Overwrites the existing enquiry data file.
     * 
     * @param enquiries The list of Enquiry objects to be saved.
     */
    void saveEnquiries(List<Enquiry> enquiries);

    /**
     * Adds a new enquiry to the service's internal list.
     * Should perform checks to prevent adding duplicates (based on enquiry ID).
     * 
     * @param enquiry The Enquiry object to add.
     */
    void addEnquiry(Enquiry enquiry);

    /**
     * Removes an enquiry from the service's internal list based on its ID.
     * 
     * @param enquiryId The ID of the enquiry to remove.
     * @return true if the enquiry was found and removed, false otherwise.
     */
    boolean removeEnquiry(String enquiryId);

    /**
     * Finds an enquiry by its unique ID.
     * 
     * @param enquiryId The ID of the enquiry to find.
     * @return The Enquiry object if found, or null otherwise.
     */
    Enquiry findEnquiryById(String enquiryId);

    /**
     * Retrieves a list of all enquiries submitted by a specific applicant.
     * 
     * @param applicantNric The NRIC of the applicant.
     * @return A list of Enquiry objects submitted by the specified applicant.
     *         Returns an empty list if NRIC is null or no enquiries are found.
     */
    List<Enquiry> getEnquiriesByApplicant(String applicantNric);

    /**
     * Retrieves a list of all enquiries associated with a specific project.
     * 
     * @param projectName The name of the project.
     * @return A list of Enquiry objects for the specified project. Returns an empty
     *         list if projectName is null or no enquiries are found.
     */
    List<Enquiry> getEnquiriesByProject(String projectName);

    /**
     * Retrieves a list containing all enquiries currently managed by the service.
     * Implementations should consider returning a copy.
     * 
     * @return A list of all Enquiry objects.
     */
    List<Enquiry> getAllEnquiries();
}
