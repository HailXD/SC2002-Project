/**
 * Controller handling the generation of reports requested by an HDB Manager.
 * Currently implements a report for applicants with booked flats, allowing various filters.
 * Inherits common functionality from BaseController.
 *
 * @author Jun Yang
 */
package Controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Enums.ApplicationStatus;
import Enums.FlatType;
import Enums.MaritalStatus;
import Models.BTOApplication;
import Models.HDBManager;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;

public class ReportManagerController extends BaseController {
    /**
     * Constructs a new ReportManagerController.
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
    public ReportManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("ReportManagerController requires an HDBManager user.");
        }
    }

    /**
     * Generates and displays a report listing applicants who have booked flats,
     * based on manager-defined filters.
     * - Prompts the manager to filter by:
     * - Project Scope: All managed projects (respecting view filters) or a specific
     * managed project.
     * - Flat Type: Specific type or all.
     * - Marital Status: Specific status or all.
     * - Age Range: Minimum and/or maximum age, or none.
     * - Retrieves all applications with BOOKED status.
     * - Filters these applications based on the selected project(s), flat type, and
     * applicant demographics (marital status, age) by fetching user details.
     * - Sorts the results (e.g., by project name, then applicant NRIC).
     * - Displays the filtered list in a formatted table, showing applicant NRIC,
     * name, age, marital status, project name, and booked flat type.
     * - Shows the total count of matching applicants.
     */
    public void generateApplicantReport() {
        System.out.println("\n--- Generate Applicant Report (Booked Flats) ---");

        System.out.println("Filter by project:");
        System.out.println("1. All Projects Managed By You (Respecting View Filters)");
        System.out.println("2. A Specific Project Managed By You");
        System.out.println("0. Cancel");
        int projectFilterChoice = getIntInput("Enter choice: ", 0, 2);

        List<Project> projectsToReportOn = new ArrayList<>();
        if (projectFilterChoice == 0)
            return;

        if (projectFilterChoice == 1) {
            projectsToReportOn = getManagedProjectsApplyingFilters();
            if (projectsToReportOn.isEmpty()) {
                return;
            }
            System.out.println("Reporting on all projects you manage"
                    + (filterLocation != null || filterFlatType != null ? " (matching current view filters)." : "."));
        } else {
            List<Project> allMyProjects = projectService.getProjectsManagedBy(currentUser.getNric());
            if (allMyProjects.isEmpty()) {
                System.out.println("You are not managing any projects.");
                return;
            }
            viewAndSelectProject(allMyProjects.stream().sorted(Comparator.comparing((Project p) -> p.getProjectName()))
                    .collect(Collectors.toList()), "Select Specific Project to Report On");
            Project specificProject = selectProjectFromList(allMyProjects);
            if (specificProject == null)
                return;
            projectsToReportOn.add(specificProject);
            System.out.println("Reporting specifically for project: " + specificProject.getProjectName());
        }

        final List<String> finalProjectNames = projectsToReportOn.stream()
                .map((Project p) -> p.getProjectName())
                .collect(Collectors.toList());

        System.out.print("Filter report by Flat Type (TWO_ROOM, THREE_ROOM, or leave blank for all): ");
        String typeStr = scanner.nextLine().trim();
        FlatType filterReportFlatType = null;
        if (!typeStr.isEmpty()) {
            filterReportFlatType = FlatType.fromString(typeStr);
            if (filterReportFlatType != null) {
                System.out.println("Filtering report for flat type: " + filterReportFlatType.getDisplayName());
            } else {
                System.out.println("Invalid flat type entered. Reporting for all types.");
            }
        }

        System.out.print("Filter report by Marital Status (SINGLE, MARRIED, or leave blank for all): ");
        String maritalStr = scanner.nextLine().trim().toUpperCase();
        MaritalStatus filterMaritalStatus = null;
        if (!maritalStr.isEmpty()) {
            try {
                filterMaritalStatus = MaritalStatus.valueOf(maritalStr);
                System.out.println("Filtering report for marital status: " + filterMaritalStatus);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid marital status. Reporting for all statuses.");
            }
        }

        int minAge = getIntInput("Filter report by Minimum Age (e.g., 21, or 0 for no minimum): ", 0, 120);
        int maxAge = getIntInput("Filter report by Maximum Age (e.g., 65, or 0 for no maximum): ", 0, 120);
        if (minAge > 0 || maxAge > 0) {
            System.out.println("Filtering report for age range: "
                    + (minAge > 0 ? minAge : "Any") + " to "
                    + (maxAge > 0 ? maxAge : "Any"));
        }
        if (minAge > 0 && maxAge > 0 && maxAge < minAge) {
            System.out.println("Warning: Maximum age cannot be less than minimum age. Ignoring age filters.");
            minAge = 0;
            maxAge = 0;
        }

        final FlatType finalFilterReportFlatType = filterReportFlatType;
        final MaritalStatus finalFilterMaritalStatus = filterMaritalStatus;
        final int finalMinAge = minAge;
        final int finalMaxAge = maxAge;

        List<BTOApplication> bookedApplications = applicationService.getAllApplications().values().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.BOOKED)
                .filter(app -> finalProjectNames.contains(app.getProjectName()))
                .filter(app -> finalFilterReportFlatType == null
                        || app.getFlatTypeApplied() == finalFilterReportFlatType)
                .filter(app -> {
                    User user = userService.findUserByNric(app.getApplicantNric());
                    if (user == null)
                        return false;

                    if (finalFilterMaritalStatus != null && user.getMaritalStatus() != finalFilterMaritalStatus)
                        return false;
                    if (finalMinAge > 0 && user.getAge() < finalMinAge)
                        return false;
                    if (finalMaxAge > 0 && user.getAge() > finalMaxAge)
                        return false;

                    return true;
                })
                .sorted(Comparator.comparing((BTOApplication app) -> app.getProjectName())
                        .thenComparing(app -> app.getApplicantNric()))
                .collect(Collectors.toList());

        System.out.println("\n--- Report: Applicants with Flat Bookings ---");
        System.out.println("Filters Applied: Projects=" + String.join(", ", finalProjectNames)
                + ", FlatType="
                + (finalFilterReportFlatType == null ? "Any" : finalFilterReportFlatType.getDisplayName())
                + ", MaritalStatus=" + (finalFilterMaritalStatus == null ? "Any" : finalFilterMaritalStatus)
                + ", Age=" + (finalMinAge > 0 ? finalMinAge : "Any") + "-" + (finalMaxAge > 0 ? finalMaxAge : "Any"));
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("%-15s | %-15s | %-5s | %-10s | %-15s | %-8s\n",
                "Applicant NRIC", "Name", "Age", "Marital", "Project Name", "FlatType");
        System.out.println("---------------------------------------------------------------------------------");

        if (bookedApplications.isEmpty()) {
            System.out.println("No matching booked applications found for the specified filters.");
        } else {
            bookedApplications.forEach(app -> {
                User user = userService.findUserByNric(app.getApplicantNric());
                System.out.printf("%-15s | %-15s | %-5s | %-10s | %-15s | %-8s\n",
                        app.getApplicantNric(),
                        user != null ? user.getName() : "N/A",
                        user != null ? String.valueOf(user.getAge()) : "N/A",
                        user != null ? user.getMaritalStatus().name() : "N/A",
                        app.getProjectName(),
                        app.getFlatTypeApplied() != null ? app.getFlatTypeApplied().getDisplayName() : "N/A");
            });
        }
        System.out.println("---------------------------------------------------------------------------------");
        System.out.println("Total matching booked applicants: " + bookedApplications.size());
        System.out.println("--- End of Report ---");
    }

    /**
     * Retrieves the list of projects managed by the current user, applying any
     * active filters.
     * 
     * @return A list of projects managed by the current user, filtered by location
     *         and flat type.
     */
    private List<Project> getManagedProjectsApplyingFilters() {
        return projectService.getProjectsManagedBy(currentUser.getNric())
                .stream()
                .filter(p -> filterLocation == null || p.getNeighborhood().equalsIgnoreCase(filterLocation))
                .filter(p -> filterFlatType == null || p.getFlatTypes().containsKey(filterFlatType))
                .sorted(Comparator.comparing((Project p) -> p.getProjectName()))
                .collect(Collectors.toList());
    }
}
