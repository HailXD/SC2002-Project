/**
 * Controller responsible for handling user authentication (login) and password changes.
 * Interacts with the UserService to validate credentials and update user data.
 *
 * @author Kai Wang
 */
package Controllers;

import Models.User;
import Services.DataService;
import Services.IUserService;
import Utils.NricValidator;

public class AuthController {
    private final IUserService userService;

    /**
     * Constructs a new AuthController.
     *
     * @param userService The user service instance used for retrieving and
     *                    potentially saving user data.
     */
    public AuthController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * Attempts to log in a user based on provided NRIC and password.
     * Validates the NRIC format first.
     * Checks against user data retrieved from the UserService.
     *
     * @param nric     The NRIC entered by the user.
     * @param password The password entered by the user.
     * @return The User object if login is successful, null otherwise (prints error
     *         messages for invalid NRIC, user not found, or incorrect password).
     */
    public User login(String nric, String password) {
        if (!NricValidator.isValidNric(nric)) {
            System.out.println("Invalid NRIC format.");
            return null;
        }
        User user = userService.findUserByNric(nric);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        } else if (user != null) {
            System.out.println("Incorrect password.");
            return null;
        } else {
            System.out.println("User NRIC not found.");
            return null;
        }
    }

    /**
     * Attempts to change the password for the given user.
     * Verifies the old password before updating to the new one.
     * Ensures the new password is not empty and different from the old password.
     * Triggers a save operation for all users via the static DataService.saveUsers
     * method upon successful change.
     *
     * @param user        The User object whose password needs changing.
     * @param oldPassword The current password provided by the user for
     *                    verification.
     * @param newPassword The desired new password.
     * @return true if the password was successfully changed and saved, false
     *         otherwise (prints error messages for incorrect old password,
     *         non-matching new passwords, empty new password, or same password).
     */
    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (user == null)
            return false;

        if (user.getPassword().equals(oldPassword)) {
            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.equals(oldPassword)) {
                    System.out.println("New password cannot be the same as the old password. Password not changed.");
                    return false;
                }
                user.setPassword(newPassword);

                DataService.saveUsers(userService.getAllUsers());

                System.out.println("Password changed successfully. Please log in again.");
                return true;
            } else {
                System.out.println("New password cannot be empty.");
                return false;
            }
        } else {
            System.out.println("Incorrect old password.");
            return false;
        }
    }
}
