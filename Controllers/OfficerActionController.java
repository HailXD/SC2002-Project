/**
 * Controller handling specific actions initiated by an HDB Officer related to their role,
 * such as registering to handle projects and viewing their registration status or handling project details.
 * Inherits common functionality from BaseController.
 *
 * @author Kishore Kumar
 */
package Controllers;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Collections;

import Enums.OfficerRegistrationStatus;
import Models.HDBOfficer;
import Models.OfficerRegistration;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class OfficerActionController extends BaseController {
    /**
     * Constructs a new OfficerActionController.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param currentUser                The currently logged-in User (expected to
     *                                   be HDBOfficer).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public OfficerActionController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
    }

    /**
     * Allows an HDB Officer to register their interest in handling a specific BTO
     * project.
     * Performs several eligibility checks:
     * - Officer cannot have an active/booked/pending withdrawal BTO application.
     * - Finds projects available for registration based on:
     * - Project has remaining officer slots.
     * - Project application period is not expired.
     * - Officer does not already have any registration (Pending/Approved/Rejected)
     * for the project.
     * - Project dates do not overlap with any project the officer is currently
     * handling (Approved).
     * - Project dates do not overlap with any project the officer has a PENDING
     * registration for.
     * - Officer has never submitted a BTO application (any status) for this
     * project.
     * - Displays the list of available projects.
     * - Prompts the officer to select a project.
     * - Creates a new OfficerRegistration record with PENDING status.
     * - Saves the new registration.
     */
    public void registerForProject() {
        if (!(currentUser instanceof HDBOfficer)) {
            System.out.println("Error: Only HDB Officers can register to handle projects.");
            return;
        }
        HDBOfficer officer = (HDBOfficer) currentUser;
        Date currentDate = DateUtils.getCurrentDate();

        if (officer.hasActiveApplication() || officer.hasBooked()) {
            System.out.println(
                    "Error: Cannot register to handle a project while you have an active or booked BTO application ("
                            + officer.getAppliedProjectName() + ", Status: " + officer.getApplicationStatus() + ").");
            return;
        }
        if (officer.hasPendingWithdrawal()) {
            System.out.println(
                    "Error: Cannot register to handle a project while you have a pending withdrawal request for application '"
                            + officer.getAppliedProjectName() + "'.");
            return;
        }

        System.out.println("\n--- Register to Handle Project ---");

        Project currentlyHandlingProject = getOfficerHandlingProject(officer);

        List<Project> pendingRegistrationProjects = officerRegistrationService
                .getRegistrationsByOfficer(officer.getNric())
                .stream()
                .filter(reg -> reg.getStatus() == OfficerRegistrationStatus.PENDING)
                .map(reg -> projectService.findProjectByName(reg.getProjectName()))
                .filter(p -> p != null)
                .collect(Collectors.toList());

        List<Project> availableProjects = projectService.getAllProjects().stream()
                .filter(p -> p.getRemainingOfficerSlots() > 0)
                .filter(p -> !p.isApplicationPeriodExpired(currentDate))
                .filter(p -> officerRegistrationService.getRegistrationsByOfficer(officer.getNric())
                        .stream()
                        .noneMatch(reg -> reg.getProjectName().equals(p.getProjectName())))
                .filter(p -> currentlyHandlingProject == null || !checkDateOverlap(p, currentlyHandlingProject))
                .filter(p -> pendingRegistrationProjects.stream()
                        .noneMatch(pendingProject -> checkDateOverlap(p, pendingProject)))
                .filter(p -> applicationService.getApplicationsByApplicant(officer.getNric())
                        .stream()
                        .noneMatch(app -> app.getProjectName().equals(p.getProjectName())))
                .sorted(Comparator.comparing((Project p) -> p.getProjectName()))
                .collect(Collectors.toList());

        if (availableProjects.isEmpty()) {
            System.out
                    .println("No projects currently available for you to register for based on eligibility criteria:");
            System.out.println("- Project must have open officer slots and not be expired.");
            System.out.println(
                    "- You must not already have any registration (Pending/Approved/Rejected) for the project.");
            System.out.println("- You cannot have an active/booked BTO application or a pending withdrawal request.");
            System.out.println("- Project dates cannot overlap with a project you are already handling.");
            System.out.println("- Project dates cannot overlap with a project you have a PENDING registration for.");
            System.out.println("- You cannot register for a project you have previously applied for (any status).");
            return;
        }

        viewAndSelectProject(availableProjects, "Select Project to Register For");
        Project selectedProject = selectProjectFromList(availableProjects);

        if (selectedProject != null) {
            OfficerRegistration newRegistration = new OfficerRegistration(officer.getNric(),
                    selectedProject.getProjectName(), currentDate);
            officerRegistrationService.addRegistration(newRegistration);
            System.out.println("Registration request submitted for project '" + selectedProject.getProjectName()
                    + "'. Status: PENDING approval by Manager.");
            officerRegistrationService.saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
        }
    }

    /**
     * Displays the HDB Officer's current project handling status and registration
     * history.
     * - Shows the project currently being handled (if any, with APPROVED status and
     * active period).
     * - Lists all other registration requests (Pending, Rejected, or past Approved)
     * sorted by date.
     */
    public void viewRegistrationStatus() {
        if (!(currentUser instanceof HDBOfficer))
            return;
        HDBOfficer officer = (HDBOfficer) currentUser;
        System.out.println("\n--- Your HDB Officer Registration Status ---");

        Project handlingProject = getOfficerHandlingProject(officer);
        if (handlingProject != null) {
            System.out.println("You are currently APPROVED and HANDLING project: " + handlingProject.getProjectName());
            System.out.println(
                    "  (Application Period: " + DateUtils.formatDate(handlingProject.getApplicationOpeningDate())
                            + " to " + DateUtils.formatDate(handlingProject.getApplicationClosingDate()) + ")");
            System.out.println("----------------------------------------");
        }

        List<OfficerRegistration> myOtherRegistrations = officerRegistrationService
                .getRegistrationsByOfficer(officer.getNric())
                .stream()
                .filter(reg -> handlingProject == null
                        || !reg.getProjectName().equals(handlingProject.getProjectName()))
                .sorted(Comparator.comparing((OfficerRegistration oR) -> oR.getRegistrationDate()).reversed())
                .collect(Collectors.toList());

        if (myOtherRegistrations.isEmpty() && handlingProject == null) {
            System.out.println("You have no past or pending registration requests.");
        } else if (!myOtherRegistrations.isEmpty()) {
            System.out.println("Other Registration History/Requests:");
            for (OfficerRegistration reg : myOtherRegistrations) {
                System.out.printf("- Project: %-15s | Status: %-10s | Date: %s\n",
                        reg.getProjectName(), reg.getStatus(), DateUtils.formatDate(reg.getRegistrationDate()));
            }
        } else {
        }
    }

    /**
     * Displays the detailed information for the project the HDB Officer is
     * currently handling.
     * Uses the shared `viewAndSelectProject` display format from BaseController.
     * If the officer is not handling any active project, it informs them.
     */
    public void viewHandlingProjectDetails() {
        if (!(currentUser instanceof HDBOfficer))
            return;
        HDBOfficer officer = (HDBOfficer) currentUser;

        Project project = getOfficerHandlingProject(officer);

        if (project == null) {
            System.out.println(
                    "You are not currently handling any active project. Register for one first or check registration status.");
            return;
        }

        System.out.println("\n--- Details for Handling Project: " + project.getProjectName() + " ---");
        viewAndSelectProject(Collections.singletonList(project), "Project Details");
    }
}
