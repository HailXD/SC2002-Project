/**
 * Controller handling specific actions initiated by an Applicant (or an Officer acting as an Applicant),
 * such as viewing open projects, applying for a project, viewing their application status,
 * and requesting withdrawal. Inherits common functionality from BaseController.
 *
 * @author Kai Wang
 */
package Controllers;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Comparator;

import Enums.ApplicationStatus;
import Enums.FlatType;
import Enums.OfficerRegistrationStatus;
import Models.Applicant;
import Models.BTOApplication;
import Models.HDBOfficer;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class ApplicantActionController extends BaseController {
    /**
     * Constructs a new ApplicantActionController.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param currentUser                The currently logged-in User (expected to
     *                                   be Applicant or HDBOfficer).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public ApplicantActionController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
    }

    /**
     * Displays a list of BTO projects that are currently open for application and
     * meet the
     * current user's eligibility criteria, unit availability, and any applied view
     * filters.
     * Uses
     * {@link BaseController#getFilteredProjects(boolean, boolean, boolean, boolean, boolean)}
     * with appropriate flags.
     */
    public void viewOpenProjects() {
        System.out.println("\n--- Viewing Available BTO Projects ---");
        List<Project> availableProjects = getFilteredProjects(true, true, true, true, true);
        viewAndSelectProject(availableProjects, "Available BTO Projects");
    }

    /**
     * Guides the applicant through the process of applying for a BTO project.
     * - Checks if the applicant already has an active/booked/pending withdrawal
     * application.
     * - Displays eligible projects (open, visible, eligible, available units).
     * - Prompts the applicant to select a project.
     * - Performs checks specific to HDB Officers applying (cannot apply for
     * handling project or project with pending registration).
     * - Prompts the applicant to select an eligible and available flat type within
     * the chosen project.
     * - Creates a new BTOApplication record with PENDING status.
     * - Updates the Applicant's profile (applicationStatus, appliedProjectName).
     * - Saves the new application.
     */
    public void applyForProject() {
        if (!(currentUser instanceof Applicant)) {
            System.out.println("Error: Only applicants can apply for projects.");
            return;
        }
        Applicant applicant = (Applicant) currentUser;

        if (applicant.hasBooked()) {
            System.out.println("You have already booked a flat for project '" + applicant.getAppliedProjectName()
                    + "'. You cannot apply again.");
            return;
        }
        if (applicant.hasActiveApplication()) {
            System.out.println("You have an active application for project '" + applicant.getAppliedProjectName()
                    + "' with status: " + applicant.getApplicationStatus());
            System.out.println("You must withdraw (and have it approved) or be unsuccessful before applying again.");
            return;
        }
        if (applicant.hasPendingWithdrawal()) {
            System.out.println("You have a withdrawal request pending manager approval for project '"
                    + applicant.getAppliedProjectName() + "'.");
            System.out.println("You cannot apply for a new project until the withdrawal is processed.");
            return;
        }

        System.out.println("\n--- Apply for BTO Project ---");
        List<Project> eligibleProjects = getFilteredProjects(true, true, true, true, true);

        if (eligibleProjects.isEmpty()) {
            System.out.println(
                    "There are currently no open projects you are eligible to apply for based on filters, eligibility, and unit availability.");
            return;
        }

        viewAndSelectProject(eligibleProjects, "Select Project to Apply For");
        Project selectedProject = selectProjectFromList(eligibleProjects);
        if (selectedProject == null)
            return;

        if (currentUser instanceof HDBOfficer) {
            HDBOfficer officer = (HDBOfficer) currentUser;
            Project handlingProject = getOfficerHandlingProject(officer);
            if (handlingProject != null && selectedProject.equals(handlingProject)) {
                System.out.println("Error: You cannot apply for a project you are currently handling as an Officer.");
                return;
            }
            boolean hasPendingRegistration = officerRegistrationService.getRegistrationsByOfficer(officer.getNric())
                    .stream()
                    .anyMatch(reg -> reg.getProjectName().equals(selectedProject.getProjectName()) &&
                            reg.getStatus() == OfficerRegistrationStatus.PENDING);
            if (hasPendingRegistration) {
                System.out.println("Error: You cannot apply for a project you have a pending registration for.");
                return;
            }
        }

        FlatType selectedFlatType = selectEligibleFlatType(selectedProject);
        if (selectedFlatType == null)
            return;

        BTOApplication newApplication = new BTOApplication(currentUser.getNric(), selectedProject.getProjectName(),
                selectedFlatType, DateUtils.getCurrentDate());
        applicationService.addApplication(newApplication);

        applicant.setAppliedProjectName(selectedProject.getProjectName());
        applicant.setApplicationStatus(ApplicationStatus.PENDING);
        applicant.setBookedFlatType(null);

        System.out.println("Application submitted successfully for project '" + selectedProject.getProjectName() + "' ("
                + selectedFlatType.getDisplayName() + "). Status: PENDING.");

        applicationService.saveApplications(applicationService.getAllApplications());
    }

    private FlatType selectEligibleFlatType(Project project) {
        List<FlatType> eligibleAndAvailableTypes = project.getFlatTypes().entrySet().stream()
                .filter(entry -> canApplyForFlatType(entry.getKey()) && entry.getValue().getAvailableUnits() > 0)
                .map(entry -> entry.getKey())
                .sorted()
                .collect(Collectors.toList());

        if (eligibleAndAvailableTypes.isEmpty()) {
            System.out.println(
                    "There are no flat types available in this project that you are eligible for and have units remaining.");
            return null;
        }

        if (eligibleAndAvailableTypes.size() == 1) {
            FlatType onlyOption = eligibleAndAvailableTypes.get(0);
            System.out.println("You will be applying for the only eligible and available type: "
                    + onlyOption.getDisplayName() + ".");
            return onlyOption;
        } else {
            System.out.println("Select the flat type you want to apply for:");
            for (int i = 0; i < eligibleAndAvailableTypes.size(); i++) {
                System.out.println((i + 1) + ". " + eligibleAndAvailableTypes.get(i).getDisplayName());
            }
            int typeChoice = getIntInput("Enter choice (or 0 to cancel): ", 0, eligibleAndAvailableTypes.size());

            if (typeChoice == 0) {
                System.out.println("Application cancelled.");
                return null;
            }
            return eligibleAndAvailableTypes.get(typeChoice - 1);
        }
    }

    /**
     * Displays the details of the current or most recent BTO application associated
     * with the logged-in applicant.
     * Retrieves application details from the applicant's profile and the
     * application service.
     * If no current application is linked to the profile but historical records
     * exist, it shows the latest historical one.
     * Displays project name, neighborhood, applied flat type, status, booked type
     * (if applicable), and application date.
     */
    public void viewMyApplication() {
        if (!(currentUser instanceof Applicant))
            return;
        Applicant applicant = (Applicant) currentUser;

        String projectName = applicant.getAppliedProjectName();
        ApplicationStatus status = applicant.getApplicationStatus();

        if (projectName == null || status == null) {
            System.out.println("You do not have any current or past BTO application records synced to your profile.");
            List<BTOApplication> historicalApps = applicationService.getApplicationsByApplicant(applicant.getNric());
            if (!historicalApps.isEmpty()) {
                System.out.println("However, historical application records exist. The latest was:");
                BTOApplication latestApp = historicalApps.stream()
                        .max(Comparator.comparing(app -> app.getApplicationDate()))
                        .orElse(null);
                if (latestApp != null) {
                    Project project = projectService.findProjectByName(latestApp.getProjectName());
                    System.out.println("Project Name: " + latestApp.getProjectName());
                    System.out.println("Neighborhood: "
                            + (project != null ? project.getNeighborhood() : "(Project details not found)"));
                    System.out.println("Flat Type Applied For: "
                            + (latestApp.getFlatTypeApplied() != null ? latestApp.getFlatTypeApplied().getDisplayName()
                                    : "N/A"));
                    System.out.println("Application Status: " + latestApp.getStatus());
                    System.out.println("Application Date: " + DateUtils.formatDate(latestApp.getApplicationDate()));
                }
            }
            return;
        }

        BTOApplication application = applicationService.findApplicationByApplicantAndProject(applicant.getNric(),
                projectName);
        if (application == null) {
            System.out.println(
                    "Error: Your profile indicates an application for '" + projectName
                            + "', but the detailed record could not be found. Please contact support.");
            return;
        }

        Project project = projectService.findProjectByName(projectName);

        System.out.println("\n--- Your BTO Application ---");
        System.out.println("Project Name: " + projectName);
        System.out.println(
                "Neighborhood: " + (project != null ? project.getNeighborhood() : "(Project details not found)"));
        System.out.println("Flat Type Applied For: "
                + (application.getFlatTypeApplied() != null ? application.getFlatTypeApplied().getDisplayName()
                        : "N/A"));
        System.out.println("Application Status: " + status);
        if (status == ApplicationStatus.BOOKED && applicant.getBookedFlatType() != null) {
            System.out.println("Booked Flat Type: " + applicant.getBookedFlatType().getDisplayName());
        }
        System.out.println("Application Date: " + DateUtils.formatDate(application.getApplicationDate()));
    }

    /**
     * Allows an applicant to request withdrawal of their current BTO application.
     * - Checks if the applicant has an application eligible for withdrawal
     * (PENDING, SUCCESSFUL, or BOOKED status).
     * - Retrieves the application record.
     * - Prompts for confirmation.
     * - If confirmed, updates the application status and the applicant's profile
     * status to PENDING_WITHDRAWAL.
     * - Saves the updated application. Manager approval is required to finalize the
     * withdrawal.
     */
    public void requestWithdrawal() {
        if (!(currentUser instanceof Applicant))
            return;
        Applicant applicant = (Applicant) currentUser;

        String currentProject = applicant.getAppliedProjectName();
        ApplicationStatus currentStatus = applicant.getApplicationStatus();

        if (currentProject == null || currentStatus == null) {
            System.out.println("You do not have an application to withdraw.");
            return;
        }

        if (currentStatus != ApplicationStatus.PENDING &&
                currentStatus != ApplicationStatus.SUCCESSFUL &&
                currentStatus != ApplicationStatus.BOOKED) {
            System.out
                    .println("Your application status (" + currentStatus + ") is not eligible for withdrawal request.");
            System.out.println("You can only request withdrawal if your status is PENDING, SUCCESSFUL, or BOOKED.");
            return;
        }

        BTOApplication application = applicationService.findApplicationByApplicantAndProject(applicant.getNric(),
                currentProject);
        if (application == null) {
            System.out.println(
                    "Error: Could not find the application record for project '" + currentProject
                            + "' to request withdrawal. Please contact support.");
            return;
        }
        if (application.getStatus() != currentStatus) {
            System.out.println("Error: Application status mismatch between profile (" + currentStatus + ") and record ("
                    + application.getStatus() + "). Please re-login or contact support.");
            return;
        }

        System.out.println("\n--- Request Application Withdrawal ---");
        System.out.println("Project: " + application.getProjectName());
        System.out.println("Current Status: " + currentStatus);
        System.out.print(
                "Are you sure you want to request withdrawal for this application? Manager approval is required. (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            application.setStatus(ApplicationStatus.PENDING_WITHDRAWAL);

            applicant.setApplicationStatus(ApplicationStatus.PENDING_WITHDRAWAL);

            System.out.println("Withdrawal request submitted successfully.");
            System.out.println("Your application status is now PENDING_WITHDRAWAL and requires Manager approval.");

            applicationService.saveApplications(applicationService.getAllApplications());

        } else {
            System.out.println("Withdrawal request cancelled.");
        }
    }
}
