/**
 * Controller handling actions performed by an HDB Manager related to managing
 * HDB Officer registration requests for projects (Approving or Rejecting).
 * Inherits common functionality from BaseController.
 *
 * @author Jordon
 */
package Controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Enums.OfficerRegistrationStatus;
import Models.HDBManager;
import Models.HDBOfficer;
import Models.OfficerRegistration;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class OfficerRegistrationManagerController extends BaseController {
    /**
     * Constructs a new OfficerRegistrationManagerController.
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
    public OfficerRegistrationManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("OfficerRegistrationManagerController requires an HDBManager user.");
        }
    }

    /**
     * Guides the HDB Manager through approving or rejecting pending officer
     * registrations for a selected managed project.
     * - Displays a list of projects managed by the current manager for selection.
     * - Shows the officer slot usage for the selected project.
     * - Retrieves and displays PENDING officer registrations for the selected
     * project.
     * - Prompts the manager to select a pending registration to process.
     * - Validates that the registering user is still a valid HDB Officer.
     * - Prompts for Approve (A) or Reject (R) action.
     * - If Approve:
     * - Checks if there are remaining officer slots in the project.
     * - Checks if the officer is already handling another project with overlapping
     * dates.
     * - If checks pass, calls a helper method (`approveOfficerRegistration`) to
     * update the registration status to APPROVED, add the officer to the project's
     * approved list, and save both registration and project data.
     * - If Reject:
     * - Updates the registration status to REJECTED and saves the registration
     * data.
     * - Displays lists of currently approved officers and rejected registrations
     * for the project.
     */
    public void manageOfficerRegistrations() {
        System.out.println("\n--- Manage HDB Officer Registrations ---");
        List<Project> myProjects = projectService.getProjectsManagedBy(currentUser.getNric())
                .stream()
                .sorted(Comparator.comparing((Project p) -> p.getProjectName()))
                .collect(Collectors.toList());
        if (myProjects.isEmpty()) {
            System.out.println("You are not managing any projects for which to manage registrations.");
            return;
        }

        System.out.println("Select project to manage registrations for:");
        viewAndSelectProject(myProjects, "Select Project");
        Project selectedProject = selectProjectFromList(myProjects);
        if (selectedProject == null)
            return;

        System.out.println("\n--- Registrations for Project: " + selectedProject.getProjectName() + " ---");
        System.out.println("Officer Slots: " + selectedProject.getApprovedOfficerNrics().size() + " / "
                + selectedProject.getMaxOfficerSlots() + " (Remaining: " + selectedProject.getRemainingOfficerSlots()
                + ")");

        List<OfficerRegistration> projectRegistrations = officerRegistrationService
                .getRegistrationsByProject(selectedProject.getProjectName())
                .stream()
                .sorted(Comparator.comparing((OfficerRegistration oR) -> oR.getRegistrationDate()))
                .collect(Collectors.toList());

        List<OfficerRegistration> pendingRegistrations = projectRegistrations.stream()
                .filter(reg -> reg.getStatus() == OfficerRegistrationStatus.PENDING)
                .collect(Collectors.toList());

        System.out.println("\n--- Pending Registrations ---");
        if (pendingRegistrations.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (int i = 0; i < pendingRegistrations.size(); i++) {
                OfficerRegistration reg = pendingRegistrations.get(i);
                User officerUser = userService.findUserByNric(reg.getOfficerNric());
                System.out.printf("%d. NRIC: %s | Name: %-15s | Date: %s\n",
                        i + 1, reg.getOfficerNric(),
                        officerUser != null ? officerUser.getName() : "N/A",
                        DateUtils.formatDate(reg.getRegistrationDate()));
            }

            int choice = getIntInput("Enter number to Approve/Reject (or 0 to skip): ", 0, pendingRegistrations.size());

            if (choice >= 1) {
                OfficerRegistration regToProcess = pendingRegistrations.get(choice - 1);
                User officerUser = userService.findUserByNric(regToProcess.getOfficerNric());

                if (!(officerUser instanceof HDBOfficer)) {
                    System.out.println("Error: User " + regToProcess.getOfficerNric()
                            + " is no longer a valid HDB Officer. Rejecting registration.");
                    regToProcess.setStatus(OfficerRegistrationStatus.REJECTED);
                    officerRegistrationService
                            .saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
                    return;
                }
                HDBOfficer officer = (HDBOfficer) officerUser;

                System.out.print("Approve or Reject registration for Officer " + officer.getName() + "? (A/R): ");
                String action = scanner.nextLine().trim().toUpperCase();

                if (action.equals("A")) {
                    if (selectedProject.getRemainingOfficerSlots() <= 0) {
                        System.out.println("Cannot approve. No remaining officer slots for this project.");
                    } else if (isOfficerHandlingOverlappingProject(officer, selectedProject)) {
                        System.out.println(
                                "Cannot approve. Officer is already handling another project with overlapping dates.");
                    } else {
                        approveOfficerRegistration(regToProcess, selectedProject, officer);
                    }
                } else if (action.equals("R")) {
                    regToProcess.setStatus(OfficerRegistrationStatus.REJECTED);
                    System.out.println("Registration Rejected for Officer " + officer.getName() + ".");
                    officerRegistrationService
                            .saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
                } else {
                    System.out.println("Invalid action ('A' or 'R' expected). No change made.");
                }
            } else if (choice != 0) {
                System.out.println("Invalid choice.");
            }
        }

        System.out.println("\n--- Approved Officers for this Project ---");
        List<String> approvedNrics = selectedProject.getApprovedOfficerNrics();
        if (approvedNrics.isEmpty()) {
            System.out.println("(None)");
        } else {
            approvedNrics.forEach(nric -> {
                User user = userService.findUserByNric(nric);
                System.out.println(
                        "- NRIC: " + nric + (user != null ? " (Name: " + user.getName() + ")" : " (Name: N/A)"));
            });
        }

        System.out.println("\n--- Rejected Registrations for this Project ---");
        List<OfficerRegistration> rejected = projectRegistrations.stream()
                .filter(r -> r.getStatus() == OfficerRegistrationStatus.REJECTED)
                .collect(Collectors.toList());
        if (rejected.isEmpty()) {
            System.out.println("(None)");
        } else {
            rejected.forEach(reg -> {
                User user = userService.findUserByNric(reg.getOfficerNric());
                System.out.println("- NRIC: " + reg.getOfficerNric()
                        + (user != null ? " (Name: " + user.getName() + ")" : " (Name: N/A)")
                        + " | Rejected on/after: " + DateUtils.formatDate(reg.getRegistrationDate()));
            });
        }
    }

    private boolean isOfficerHandlingOverlappingProject(HDBOfficer officer, Project targetProject) {
        return officerRegistrationService.getRegistrationsByOfficer(officer.getNric())
                .stream()
                .filter(reg -> reg.getStatus() == OfficerRegistrationStatus.APPROVED)
                .filter(reg -> !reg.getProjectName().equals(targetProject.getProjectName()))
                .map(reg -> projectService.findProjectByName(reg.getProjectName()))
                .filter(otherProject -> otherProject != null)
                .anyMatch(otherProject -> checkDateOverlap(targetProject, otherProject));
    }

    /**
     * Approves the officer registration for the given project and officer.
     * @param registration The officer registration to approve.
     * @param project The project to which the officer is being added.
     * @param officer The officer being approved.
     */
    private void approveOfficerRegistration(OfficerRegistration registration, Project project, HDBOfficer officer) {
        if (project.getRemainingOfficerSlots() <= 0) {
            System.out.println("Error: No remaining officer slots. Approval cannot proceed.");
            return;
        }

        if (project.addApprovedOfficer(registration.getOfficerNric())) {
            registration.setStatus(OfficerRegistrationStatus.APPROVED);
            System.out.println("Registration Approved. Officer " + registration.getOfficerNric() + " ("
                    + officer.getName() + ") added to project '" + project.getProjectName() + "'.");

            officerRegistrationService.saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
            projectService.saveProjects(projectService.getAllProjects());
        } else {
            System.err.println("Error: Failed to add officer " + registration.getOfficerNric() + " to project '"
                    + project.getProjectName() + "' approved list (Slots full or already added?). Approval aborted.");
        }
    }
}
