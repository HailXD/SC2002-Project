/**
 * Interface defining the contract for user data management services.
 * Specifies methods for loading, saving, finding, and retrieving user data.
 *
 * @author Kishore Kumar
 */
package Services;

import java.util.Map;
import Models.User;

public interface IUserService {
    /**
     * Loads user data from the specified data files and populates the internal user
     * map.
     * Clears existing users before loading new data.
     *
     * @return A map of user NRIC to User object.
     */
    Map<String, User> loadUsers();

    /**
     * Saves the current user data to the specified data files.
     * This method should be called to persist any changes made to the user data.
     */
    void saveUsers(Map<String, User> users);

    /**
     * Finds a user by their NRIC.
     *
     * @param nric The NRIC of the user to find.
     * @return The User object if found, null otherwise.
     */
    User findUserByNric(String nric);

    /**
     * Retrieves a map containing all users currently managed by the service.
     * 
     * @return A map of NRIC to User objects.
     */
    Map<String, User> getAllUsers();
}
