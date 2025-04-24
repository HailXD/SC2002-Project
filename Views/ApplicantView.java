/**
 * View component responsible for displaying the menu and handling input for the Applicant role.
 * Interacts with the {@link Controllers.ApplicantController} to execute actions.
 *
 * @author Jun Yang
 */

package Views;

import java.util.Scanner;
import Models.User;

import Controllers.ApplicantController;
import Controllers.AuthController;

public class ApplicantView extends BaseView {
    private final ApplicantController applicantController;
    
    /**
     * Constructs a new ApplicantView.
     *
     * @param scanner        Scanner instance for reading user input.
     * @param currentUser    The currently logged-in User (expected to be
     *                       Applicant).
     * @param controller     The ApplicantController associated with this view.
     * @param authController Controller for authentication tasks.
     */
    public ApplicantView(Scanner scanner, User currentUser, ApplicantController controller,
            AuthController authController) {
        super(scanner, currentUser, controller, authController);
        this.applicantController = controller;
    }

    /**
     * Displays the main menu for the Applicant role.
     * Presents options for viewing/applying for projects, managing applications and
     * enquiries,
     * applying filters, changing password, and logging out.
     * Reads user input and calls the corresponding methods on the
     * ApplicantController or BaseView helpers.
     * Loops until the user chooses to logout.
     */
    @Override
    public void displayMenu() {
        boolean logout = false;
        while (!logout) {
            System.out.println("\n=========== Applicant Menu ===========");
            System.out.println("Welcome, " + currentUser.getName() + "!");
            System.out.println("1. View Available BTO Projects");
            System.out.println("2. Apply for BTO Project");
            System.out.println("3. View My Application Status");
            System.out.println("4. Request Application Withdrawal");
            System.out.println("5. Submit Enquiry");
            System.out.println("6. View My Enquiries");
            System.out.println("7. Edit My Enquiry");
            System.out.println("8. Delete My Enquiry");
            System.out.println("9. Apply/Clear Project Filters");
            System.out.println("10. Change Password");
            System.out.println("0. Logout");
            System.out.println("======================================");

            int choice = getMenuChoice(0, 10);

            switch (choice) {
                case 1:
                    applicantController.viewOpenProjects();
                    break;
                case 2:
                    applicantController.applyForProject();
                    break;
                case 3:
                    applicantController.viewMyApplication();
                    break;
                case 4:
                    applicantController.requestWithdrawal();
                    break;
                case 5:
                    applicantController.submitEnquiry();
                    break;
                case 6:
                    applicantController.viewMyEnquiries();
                    break;
                case 7:
                    applicantController.editMyEnquiry();
                    break;
                case 8:
                    applicantController.deleteMyEnquiry();
                    break;
                case 9:
                    applyFilters();
                    break;
                case 10:
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
