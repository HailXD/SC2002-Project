/**
 * Controller handling actions performed by an HDB Manager related to managing
 * BTO applications submitted for the projects they manage (Approving or Rejecting PENDING applications).
 * Inherits common functionality from BaseController.
 *
 * @author Jordon
 */
package Controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Enums.ApplicationStatus;
import Enums.FlatType;
import Models.Applicant;
import Models.BTOApplication;
import Models.FlatTypeDetails;
import Models.HDBManager;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class ApplicationManagerController extends BaseController {
    /**
     * Constructs a new ApplicationManagerController.
     * Ensures the current user is an HDBManager.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access.
     * @param currentUser                The currently logged-in User (must be
     *                                   HDBManager).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     * @throws IllegalArgumentException if the currentUser is not an HDBManager.
     */
    public ApplicationManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("ApplicationManagerController requires an HDBManager user.");
        }
    }

    /**
     * Guides the HDB Manager through approving or rejecting pending BTO
     * applications for a selected managed project.
     * - Displays a list of projects managed by the current manager for selection.
     * - Retrieves and displays PENDING applications for the selected project.
     * - Prompts the manager to select a pending application to process.
     * - Validates that the applicant data is available.
     * - Prompts for Approve (A) or Reject (R) action.
     * - If Approve:
     * - Checks if the application has a valid flat type specified.
     * - Checks if approving this application would exceed the total units available
     * for that flat type (considering already SUCCESSFUL or BOOKED applications).
     * - If checks pass, updates the application status and the applicant's profile
     * status to SUCCESSFUL, then saves the application data.
     * - If checks fail (e.g., no units left), informs the manager and does not
     * approve.
     * - If Reject:
     * - Updates the application status and the applicant's profile status to
     * UNSUCCESSFUL, clears booked flat type on profile, then saves the application
     * data.
     * - Displays a summary of applications with other statuses (Successful, Booked,
     * etc.) for the project.
     */
    public void manageApplications() {
        System.out.println("\n--- Manage BTO Applications ---");
        List<Project> myProjects = projectService.getProjectsManagedBy(currentUser.getNric())
                .stream()
                .sorted(Comparator.comparing(project -> project.getProjectName()))
                .collect(Collectors.toList());
        if (myProjects.isEmpty()) {
            System.out.println("You are not managing any projects for which to manage applications.");
            return;
        }

        System.out.println("Select project to manage applications for:");
        viewAndSelectProject(myProjects, "Select Project");
        Project selectedProject = selectProjectFromList(myProjects);
        if (selectedProject == null)
            return;

        System.out.println("\n--- Applications for Project: " + selectedProject.getProjectName() + " ---");

        List<BTOApplication> projectApplications = applicationService
                .getApplicationsByProject(selectedProject.getProjectName())
                .stream()
                .sorted(Comparator.comparing(app -> app.getApplicationDate()))
                .collect(Collectors.toList());

        if (projectApplications.isEmpty()) {
            System.out.println("No applications found for this project.");
            return;
        }

        List<BTOApplication> pendingApps = projectApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .collect(Collectors.toList());

        System.out.println("--- Pending Applications ---");
        if (pendingApps.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (int i = 0; i < pendingApps.size(); i++) {
                BTOApplication app = pendingApps.get(i);
                User applicantUser = userService.findUserByNric(app.getApplicantNric());
                System.out.printf("%d. NRIC: %s | Name: %-15s | Type: %-8s | Date: %s\n",
                        i + 1, app.getApplicantNric(),
                        applicantUser != null ? applicantUser.getName() : "N/A",
                        app.getFlatTypeApplied() != null ? app.getFlatTypeApplied().getDisplayName() : "N/A",
                        DateUtils.formatDate(app.getApplicationDate()));
            }

            int choice = getIntInput("Enter number to Approve/Reject (or 0 to skip): ", 0, pendingApps.size());

            if (choice >= 1) {
                BTOApplication appToProcess = pendingApps.get(choice - 1);
                User applicantUser = userService.findUserByNric(appToProcess.getApplicantNric());

                if (!(applicantUser instanceof Applicant)) {
                    System.out.println("Error: Applicant data not found or invalid for NRIC "
                            + appToProcess.getApplicantNric() + ". Rejecting application.");
                    appToProcess.setStatus(ApplicationStatus.UNSUCCESSFUL);
                    applicationService.saveApplications(applicationService.getAllApplications());
                    return;
                }
                Applicant applicant = (Applicant) applicantUser;

                System.out.print("Approve or Reject application for Applicant " + applicant.getName() + "? (A/R): ");
                String action = scanner.nextLine().trim().toUpperCase();

                if (action.equals("A")) {
                    FlatType appliedType = appToProcess.getFlatTypeApplied();
                    if (appliedType == null) {
                        System.out.println("Error: Application has no specified flat type. Cannot approve. Rejecting.");
                        appToProcess.setStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicant.setApplicationStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicationService.saveApplications(applicationService.getAllApplications());
                        return;
                    }

                    Project currentProjectState = projectService.findProjectByName(selectedProject.getProjectName());
                    if (currentProjectState == null) {
                        System.out.println("Error: Project details not found. Cannot approve. Rejecting.");
                        appToProcess.setStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicant.setApplicationStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicationService.saveApplications(applicationService.getAllApplications());
                        return;
                    }
                    FlatTypeDetails details = currentProjectState.getFlatTypeDetails(appliedType);

                    if (details == null) {
                        System.out.println("Error: Applied flat type (" + appliedType.getDisplayName()
                                + ") does not exist in this project configuration. Rejecting application.");
                        appToProcess.setStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicant.setApplicationStatus(ApplicationStatus.UNSUCCESSFUL);
                        applicationService.saveApplications(applicationService.getAllApplications());
                        return;
                    }

                    long alreadySuccessfulOrBookedCount = applicationService
                            .getApplicationsByProject(currentProjectState.getProjectName())
                            .stream()
                            .filter(a -> a.getFlatTypeApplied() == appliedType &&
                                    (a.getStatus() == ApplicationStatus.SUCCESSFUL
                                            || a.getStatus() == ApplicationStatus.BOOKED))
                            .count();

                    if (alreadySuccessfulOrBookedCount < details.getTotalUnits()) {
                        appToProcess.setStatus(ApplicationStatus.SUCCESSFUL);
                        applicant.setApplicationStatus(ApplicationStatus.SUCCESSFUL);
                        System.out.println(
                                "Application Approved (Status: SUCCESSFUL). Applicant can now book via Officer.");
                        applicationService.saveApplications(applicationService.getAllApplications());
                    } else {
                        System.out.println("Cannot approve. The number of successful/booked applications ("
                                + alreadySuccessfulOrBookedCount + ") already meets or exceeds the total supply ("
                                + details.getTotalUnits() + ") for " + appliedType.getDisplayName() + ".");
                        System.out.println(
                                "Consider rejecting this application or managing existing successful/booked ones.");
                    }

                } else if (action.equals("R")) {
                    appToProcess.setStatus(ApplicationStatus.UNSUCCESSFUL);
                    applicant.setApplicationStatus(ApplicationStatus.UNSUCCESSFUL);
                    applicant.setBookedFlatType(null);
                    System.out.println("Application Rejected (Status: UNSUCCESSFUL).");
                    applicationService.saveApplications(applicationService.getAllApplications());
                } else {
                    System.out.println("Invalid action ('A' or 'R' expected). No change made.");
                }
            } else if (choice != 0) {
                System.out.println("Invalid choice.");
            }
        }

        System.out.println("\n--- Other Application Statuses ---");
        List<BTOApplication> otherApps = projectApplications.stream()
                .filter(app -> app.getStatus() != ApplicationStatus.PENDING)
                .collect(Collectors.toList());

        if (otherApps.isEmpty()) {
            System.out.println("(None)");
        } else {
            otherApps.forEach(app -> {
                User applicant = userService.findUserByNric(app.getApplicantNric());
                System.out.printf("- NRIC: %s | Name: %-15s | Type: %-8s | Status: %s\n",
                        app.getApplicantNric(),
                        applicant != null ? applicant.getName() : "N/A",
                        app.getFlatTypeApplied() != null ? app.getFlatTypeApplied().getDisplayName() : "N/A",
                        app.getStatus());
            });
        }
    }
}
