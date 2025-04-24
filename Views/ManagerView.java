/**
 * View component responsible for displaying the menu and handling input for the HDB Manager role.
 * Interacts with the {@link Controllers.ManagerController} to execute actions covering project management,
 * staff/application management, reporting, and enquiry oversight.
 *
 * @author Jun Yang
 */
package Views;

import java.util.Scanner;
import Models.User;
import Controllers.ManagerController;
import Controllers.AuthController;

public class ManagerView extends BaseView {
    private final ManagerController managerController;

    /**
     * Constructs a new ManagerView.
     *
     * @param scanner        Scanner instance for reading user input.
     * @param currentUser    The currently logged-in User (expected to be
     *                       HDBManager).
     * @param controller     The ManagerController associated with this view.
     * @param authController Controller for authentication tasks.
     */
    public ManagerView(Scanner scanner, User currentUser, ManagerController controller, AuthController authController) {
        super(scanner, currentUser, controller, authController);
        this.managerController = controller;
    }

    /**
     * Displays the main menu for the HDB Manager role.
     * Presents options grouped by function: Project Management, Staff and Application
     * Management,
     * Reporting and Enquiries, and Common Actions (filters, password change, logout).
     * Reads user input and calls the corresponding methods on the ManagerController
     * or BaseView helpers.
     * Loops until the user chooses to logout.
     */
    @Override
    public void displayMenu() {
        boolean logout = false;
        while (!logout) {
            System.out.println("\n=========== HDB Manager Menu ===========");
            System.out.println("Welcome, Manager " + currentUser.getName() + "!");
            System.out.println("--- Project Management ---");
            System.out.println(" 1. Create New BTO Project");
            System.out.println(" 2. Edit My Project Details");
            System.out.println(" 3. Delete My Project");
            System.out.println(" 4. Toggle Project Visibility");
            System.out.println(" 5. View All Projects");
            System.out.println(" 6. View My Managed Projects");
            System.out.println("--- Staff & Application Management ---");
            System.out.println(" 7. Manage Officer Registrations (Approve/Reject)");
            System.out.println(" 8. Manage BTO Applications (Approve/Reject)");
            System.out.println(" 9. Manage Withdrawal Requests (Approve/Reject)");
            System.out.println("--- Reporting & Enquiries ---");
            System.out.println("10. Generate Applicant Report (Booked Flats)");
            System.out.println("11. View Enquiries (All Projects)");
            System.out.println("12. View/Reply Enquiries (My Managed Projects)");
            System.out.println("--- Common Actions ---");
            System.out.println("13. Apply/Clear Project Filters (Affects Views 5, 6, 10, 12)");
            System.out.println("14. Change Password");
            System.out.println(" 0. Logout");
            System.out.println("========================================");

            int choice = getMenuChoice(0, 14);

            switch (choice) {
                case 1:
                    managerController.createProject();
                    break;
                case 2:
                    managerController.editProject();
                    break;
                case 3:
                    managerController.deleteProject();
                    break;
                case 4:
                    managerController.toggleProjectVisibility();
                    break;
                case 5:
                    managerController.viewAllProjects();
                    break;
                case 6:
                    managerController.viewMyProjects();
                    break;
                case 7:
                    managerController.manageOfficerRegistrations();
                    break;
                case 8:
                    managerController.manageApplications();
                    break;
                case 9:
                    managerController.manageWithdrawalRequests();
                    break;
                case 10:
                    managerController.generateApplicantReport();
                    break;
                case 11:
                    managerController.viewAllEnquiries();
                    break;
                case 12:
                    managerController.viewAndReplyToManagedEnquiries();
                    break;
                case 13:
                    applyFilters();
                    break;
                case 14:
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
