/**
 * View component responsible for displaying the menu and handling input for the HDB Officer role.
 * Interacts with the {@link Controllers.OfficerController} to execute actions.
 * Includes both officer-specific actions and inherited applicant actions.
 *
 * @author Jun Yang
 */
package Views;

import Controllers.OfficerController;
import Controllers.AuthController;
import Models.User;
import Models.Project;
import Models.HDBOfficer;

import java.util.Scanner;

public class OfficerView extends BaseView {
    private final OfficerController officerController;
    
    /**
     * Constructs a new OfficerView.
     *
     * @param scanner        Scanner instance for reading user input.
     * @param currentUser    The currently logged-in User (expected to be
     *                       HDBOfficer).
     * @param controller     The OfficerController associated with this view.
     * @param authController Controller for authentication tasks.
     */
    public OfficerView(Scanner scanner, User currentUser, OfficerController controller, AuthController authController) {
        super(scanner, currentUser, controller, authController);
        this.officerController = controller;
    }

    /**
     * Displays the main menu for the HDB Officer role.
     * Shows the currently handled project (if any).
     * Presents options for officer actions (registration, viewing status/details,
     * managing enquiries/bookings),
     * inherited applicant actions (viewing/applying for projects, managing own
     * applications/enquiries),
     * common actions (filters, password change), and logging out.
     * Reads user input and calls the corresponding methods on the OfficerController
     * or BaseView helpers.
     * Loops until the user chooses to logout.
     */
    @Override
    public void displayMenu() {
        boolean logout = false;
        while (!logout) {
            System.out.println("\n=========== HDB Officer Menu ===========");
            System.out.println("Welcome, Officer " + currentUser.getName() + "!");
            Project handlingProject = officerController.getOfficerHandlingProject((HDBOfficer) currentUser);
            if (handlingProject != null) {
                System.out.println("--> Currently Handling Project: " + handlingProject.getProjectName() + " <--");
            } else {
                System.out.println("--> Not currently handling any project <--");
            }
            System.out.println("--- Officer Actions ---");
            System.out.println(" 1. Register to Handle Project");
            System.out.println(" 2. View My Registration Status");
            System.out.println(" 3. View Handling Project Details");
            System.out.println(" 4. View/Reply Enquiries (Handling Project)");
            System.out.println(" 5. Manage Flat Booking (Process Successful Applicants)");
            System.out.println("--- Applicant Actions (Inherited) ---");
            System.out.println(" 6. View Available BTO Projects");
            System.out.println(" 7. Apply for BTO Project");
            System.out.println(" 8. View My Application Status");
            System.out.println(" 9. Request Application Withdrawal");
            System.out.println("10. Submit Enquiry");
            System.out.println("11. View My Enquiries");
            System.out.println("12. Edit My Enquiry");
            System.out.println("13. Delete My Enquiry");
            System.out.println("--- Common Actions ---");
            System.out.println("14. Apply/Clear Project Filters");
            System.out.println("15. Change Password");
            System.out.println(" 0. Logout");
            System.out.println("========================================");

            int choice = getMenuChoice(0, 15);

            switch (choice) {
                case 1:
                    officerController.registerForProject();
                    break;
                case 2:
                    officerController.viewRegistrationStatus();
                    break;
                case 3:
                    officerController.viewHandlingProjectDetails();
                    break;
                case 4:
                    officerController.viewAndReplyToEnquiries();
                    break;
                case 5:
                    officerController.manageFlatBooking();
                    break;
                case 6:
                    officerController.viewOpenProjects();
                    break;
                case 7:
                    officerController.applyForProject();
                    break;
                case 8:
                    officerController.viewMyApplication();
                    break;
                case 9:
                    officerController.requestWithdrawal();
                    break;
                case 10:
                    officerController.submitEnquiry();
                    break;
                case 11:
                    officerController.viewMyEnquiries();
                    break;
                case 12:
                    officerController.editMyEnquiry();
                    break;
                case 13:
                    officerController.deleteMyEnquiry();
                    break;
                case 14:
                    applyFilters();
                    break;
                case 15:
                    if (changePassword()) {
                        logout = true;
                    }
                    break;
                case 0:
                    logout = true;
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice.");
                    break;
            }
            if (!logout && choice != 0) {
                pause();
            }
        }
    }
}
