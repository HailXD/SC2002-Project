
/**
 * Main application class for the BTO Management System.
 * Initializes services, loads data, handles the main login loop,
 * instantiates appropriate controllers and views based on user role,
 * and manages application shutdown.
 *
 * @author Patrick
 */

import java.util.Scanner;

import Models.User;

import Controllers.*;
import Services.*;

import Views.ApplicantView;
import Views.BaseView;
import Views.OfficerView;
import Views.ManagerView;

public class BTOApp {

    private IUserService userService;
    private IProjectService projectService;
    private IApplicationService applicationService;
    private IEnquiryService enquiryService;
    private IOfficerRegistrationService officerRegistrationService;

    private AuthController authController;
    private Scanner scanner;

    /**
     * Constructs the BTOApp instance.
     * Initializes the Scanner and instantiates all the necessary service
     * implementations
     * (UserService, ProjectService, ApplicationService, EnquiryService,
     * OfficerRegistrationService).
     */
    public BTOApp() {
        scanner = new Scanner(System.in);
        userService = new UserService();
        projectService = new ProjectService();
        applicationService = new ApplicationService();
        enquiryService = new EnquiryService();
        officerRegistrationService = new OfficerRegistrationService();
    }

    /**
     * Initializes the application by loading data from persistent storage (CSV
     * files)
     */
    public void initialize() {
        System.out.println("Initializing BTO Management System...");

        System.out.println("Loading users...");
        userService.loadUsers();

        System.out.println("Loading projects...");
        projectService.loadProjects(userService.getAllUsers());

        System.out.println("Loading applications...");
        applicationService.loadApplications(projectService.getAllProjects());

        System.out.println("Loading officer registrations...");
        officerRegistrationService.loadOfficerRegistrations(userService.getAllUsers(), projectService.getAllProjects());

        System.out.println("Loading enquiries...");
        enquiryService.loadEnquiries();

        System.out.println("Performing data synchronization...");
        DataService.synchronizeData(userService, projectService, applicationService, officerRegistrationService);

        authController = new AuthController(userService);

        System.out.println("Initialization complete. System ready.");
    }

    /**
     * Runs the main application loop.
     * Continuously prompts the user to log in via {@link #loginScreen()}.
     * If login is successful, it calls {@link #showRoleMenu(User)} to display the
     * appropriate menu.
     * After logout, it returns to the login screen or exits if the user chooses not
     * to retry after a failed login.
     * Includes a call to DataService.synchronizeData before each login attempt
     * (consider if this frequency is necessary).
     */
    public void run() {
        User currentUser = null;
        while (true) {
            DataService.synchronizeData(userService, projectService, applicationService, officerRegistrationService);

            currentUser = loginScreen();

            if (currentUser != null) {
                showRoleMenu(currentUser);
                currentUser = null;
                System.out.println("\nReturning to Login Screen...");
            } else {
                System.out.print("Login failed. Try again? (yes/no): ");
                String retry = scanner.nextLine().trim().toLowerCase();
                if (!retry.equals("yes")) {
                    break;
                }
            }
        }
    }

    /**
     * Handles the user interface for logging in.
     * Prompts the user for NRIC and password.
     * Calls the {@link Controllers.AuthController#login(String, String)} method to
     * authenticate the user.
     * Prints success or failure messages.
     *
     * @return The authenticated User object if login is successful, null otherwise.
     */
    private User loginScreen() {
        System.out.println("\n--- BTO Management System Login ---");
        System.out.print("Enter NRIC: ");
        String nric = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        User user = authController.login(nric, password);
        if (user != null) {
            System.out.println("Login successful! Welcome, " + user.getName() + " (" + user.getRole() + ")");
        }
        return user;
    }

    /**
     * Instantiates the appropriate main controller and view based on the logged-in
     * user's role.
     * Calls the `displayMenu()` method of the created view to show the
     * role-specific options.
     *
     * @param user The currently logged-in User object.
     */
    private void showRoleMenu(User user) {
        BaseView view;

        switch (user.getRole()) {
            case APPLICANT:
                ApplicantController appController = new ApplicantController(
                        userService, projectService, applicationService, officerRegistrationService, enquiryService,
                        user, scanner, authController);
                view = new ApplicantView(scanner, user, appController, authController);
                break;
            case HDB_OFFICER:
                OfficerController offController = new OfficerController(
                        userService, projectService, applicationService, officerRegistrationService, enquiryService,
                        user, scanner, authController);
                view = new OfficerView(scanner, user, offController, authController);
                break;
            case HDB_MANAGER:
                ManagerController manController = new ManagerController(
                        userService, projectService, applicationService, officerRegistrationService, enquiryService,
                        user, scanner, authController);
                view = new ManagerView(scanner, user, manController, authController);
                break;
            default:
                System.err.println("FATAL Error: Unknown user role encountered: " + user.getRole());
                return;
        }
        view.displayMenu();
    }

    /**
     * The main entry point of the BTO Management System application.
     * Creates an instance of BTOApp, initializes it (loads data), and runs the main
     * loop.
     * Includes basic error handling for unexpected exceptions during execution.
     * Ensures the scanner is closed upon termination.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        BTOApp app = new BTOApp();
        try {
            app.initialize();
            app.run();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (app.scanner != null) {
                app.scanner.close();
            }
        }
        System.out.println("Application terminated.");
        System.exit(1);
    }
}