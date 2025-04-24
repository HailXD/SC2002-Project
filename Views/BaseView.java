/**
 * Abstract base class for all view components in the BTO application.
 * Provides common elements like the Scanner, current User, associated BaseController,
 * and AuthController. Includes helper methods for getting menu choices, changing passwords,
 * applying filters via the controller, and pausing execution.
 *
 * @author Jun Yang
 */
package Views;

import java.util.Scanner;

import Models.User;

import Controllers.BaseController;
import Controllers.AuthController;

public abstract class BaseView {
    protected final Scanner scanner;
    protected final User currentUser;
    protected final BaseController controller;
    protected final AuthController authController;

    /**
     * Constructs a new BaseView.
     *
     * @param scanner        Scanner instance for reading user input.
     * @param currentUser    The currently logged-in User object.
     * @param controller     The BaseController associated with this view's role.
     * @param authController Controller for authentication tasks (like password
     *                       change).
     */
    public BaseView(Scanner scanner, User currentUser, BaseController controller, AuthController authController) {
        this.scanner = scanner;
        this.currentUser = currentUser;
        this.controller = controller;
        this.authController = authController;
    }

    /**
     * Abstract method that must be implemented by subclasses to display the
     * specific menu
     * options relevant to the user's role and handle user choices by calling
     * controller methods.
     */
    public abstract void displayMenu();

    /**
     * Prompts the user to enter a menu choice (integer) within a specified range.
     * Handles invalid input (non-numeric, out of range) and reprompts until a valid
     * choice is entered.
     *
     * @param min The minimum valid menu choice number (inclusive).
     * @param max The maximum valid menu choice number (inclusive).
     * @return The validated integer menu choice entered by the user.
     */
    protected int getMenuChoice(int min, int max) {
        int choice = -1;
        while (true) {
            System.out.print("Enter your choice: ");
            String input = scanner.nextLine();
            try {
                choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter a number between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return choice;
    }

    /**
     * Handles the user interface flow for changing the password.
     * Prompts for the current password, new password, and confirmation.
     * Performs basic validation (passwords match, not empty, not same as old).
     * Calls the
     * {@link Controllers.AuthController#changePassword(Models.User, String, String)}
     * method to perform the change.
     *
     * @return true if the password change was initiated successfully by the
     *         AuthController (usually indicates user should be logged out), false
     *         otherwise.
     */
    protected boolean changePassword() {
        System.out.println("\n--- Change Password ---");
        System.out.print("Enter current password: ");
        String oldPassword = scanner.nextLine();
        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();
        System.out.print("Confirm new password: ");
        String confirmPassword = scanner.nextLine();

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("New passwords do not match. Password not changed.");
            return false;
        }
        if (newPassword.isEmpty()) {
            System.out.println("Password cannot be empty. Password not changed.");
            return false;
        }
        if (newPassword.equals(oldPassword)) {
            System.out.println("New password cannot be the same as the old password. Password not changed.");
            return false;
        }

        boolean success = authController.changePassword(currentUser, oldPassword, newPassword);
        return success;
    }

    /**
     * Calls the `applyFilters` method on the associated BaseController instance,
     * allowing the user to set or clear project view filters (location, flat type).
     */
    protected void applyFilters() {
        if (controller != null) {
            controller.applyFilters();
        } else {
            System.out.println("Error: Controller not available for filtering.");
        }
    }

    /**
     * Pauses execution and waits for the user to press Enter before continuing.
     * Used to allow users to read output before the screen clears or the menu
     * redisplays.
     */
    protected void pause() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
