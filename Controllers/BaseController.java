/**
 * Abstract base class for all controllers in the BTO application.
 * Provides common dependencies (services, current user, scanner, auth controller),
 * shared utility methods (finding data, checking date overlaps, input validation),
 * and common functionalities like project filtering and display.
 *
 * @author Kai Wang
 */
package Controllers;

import Enums.OfficerRegistrationStatus;
import Enums.FlatType;
import Enums.ApplicationStatus;
import Enums.MaritalStatus;
import Enums.UserRole;

import Models.User;
import Parsers.Dparse;
import Models.Project;
import Models.BTOApplication;
import Models.HDBManager;
import Models.HDBOfficer;
import Models.Applicant;
import Models.OfficerRegistration;
import Models.FlatTypeDetails;

import Services.IApplicationService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Comparator;
import java.util.stream.Collectors;

import Utils.DateUtils;

public abstract class BaseController {
    protected final IUserService userService;
    protected final IProjectService projectService;
    protected final IApplicationService applicationService;
    protected final IOfficerRegistrationService officerRegistrationService;
    protected final User currentUser;
    protected final Scanner scanner;
    protected final AuthController authController;

    protected String filterLocation = null;
    protected FlatType filterFlatType = null;

    /**
     * Constructs a new BaseController, initializing shared services and components.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param currentUser                The currently logged-in User object.
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks (like
     *                                   password change).
     */
    public BaseController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            User currentUser, Scanner scanner, AuthController authController) {
        this.userService = userService;
        this.projectService = projectService;
        this.applicationService = applicationService;
        this.officerRegistrationService = officerRegistrationService;
        this.currentUser = currentUser;
        this.scanner = scanner;
        this.authController = authController;
    }

    /**
     * Finds a project by its name using the injected project service.
     *
     * @param name The name of the project to find.
     * @return The Project object if found, null otherwise.
     */
    protected Project findProjectByName(String name) {
        return projectService.findProjectByName(name);
    }

    /**
     * Finds a BTO application based on the applicant's NRIC and project name using
     * the injected application service.
     *
     * @param nric        The applicant's NRIC.
     * @param projectName The project name.
     * @return The BTOApplication object if found, null otherwise.
     */
    protected BTOApplication findApplicationByApplicantAndProject(String nric, String projectName) {
        return applicationService.findApplicationByApplicantAndProject(nric, projectName);
    }

    /**
     * Determines which active project an HDB Officer is currently approved to
     * handle.
     * Checks the officer registration service for an APPROVED registration and
     * verifies
     * that the corresponding project's application period is still active.
     *
     * @param officer The HDBOfficer whose handling project is to be determined.
     * @return The Project object the officer is currently handling and which is
     *         active, or null if none.
     */
    public Project getOfficerHandlingProject(HDBOfficer officer) {
        if (officer == null)
            return null;
        Date today = DateUtils.getCurrentDate();
        OfficerRegistration approvedReg = officerRegistrationService
                .getApprovedRegistrationForOfficer(officer.getNric());

        if (approvedReg != null) {
            Project project = projectService.findProjectByName(approvedReg.getProjectName());
            if (project != null && project.isApplicationPeriodActive(today)) {
                return project;
            }
        }
        return null;
    }

    /**
     * Checks if the application periods of two projects overlap.
     * Handles null projects or dates gracefully (returns false).
     * Overlap occurs if p1's start is not after p2's end AND p1's end is not before
     * p2's start.
     *
     * @param p1 The first project.
     * @param p2 The second project.
     * @return true if the application periods overlap, false otherwise or if data
     *         is invalid.
     */
    protected boolean checkDateOverlap(Project p1, Project p2) {
        if (p1 == null || p2 == null || p1.getApplicationOpeningDate() == null || p1.getApplicationClosingDate() == null
                || p2.getApplicationOpeningDate() == null || p2.getApplicationClosingDate() == null) {
            return false;
        }
        boolean noOverlap = p1.getApplicationOpeningDate().after(p2.getApplicationClosingDate()) ||
                p1.getApplicationClosingDate().before(p2.getApplicationOpeningDate());
        return !noOverlap;
    }

    /**
     * Retrieves a list of projects based on various filtering criteria.
     * Applies internal controller filters (location, flat type) first.
     * Then applies boolean flags to control checks for visibility, application
     * period activity/expiry,
     * eligibility (based on current user), and unit availability.
     * Sorts the final list by project name.
     *
     * @param checkVisibility        If true, only includes projects visible to the
     *                               current user (see
     *                               {@link #isProjectVisibleToCurrentUser(Project)}).
     * @param checkEligibility       If true, only includes projects for which the
     *                               current user is eligible to apply for at least
     *                               one flat type (see
     *                               {@link #canApplyForFlatType(FlatType)}).
     *                               Ignored for Managers.
     * @param checkAvailability      If true, only includes projects where there is
     *                               at least one flat type the user is eligible for
     *                               AND which has available units (> 0). Ignored
     *                               for Managers. Requires checkEligibility to be
     *                               meaningful.
     * @param checkApplicationPeriod If true, only includes projects whose
     *                               application period is currently active.
     * @param checkNotExpired        If true, only includes projects whose
     *                               application period has not yet expired.
     * @return A sorted list of Project objects matching all applied filters.
     */
    protected List<Project> getFilteredProjects(boolean checkVisibility, boolean checkEligibility,
            boolean checkAvailability, boolean checkApplicationPeriod, boolean checkNotExpired) {
        Date currentDate = DateUtils.getCurrentDate();
        List<Project> allProjects = projectService.getAllProjects();

        return allProjects.stream()
                .filter(p -> filterLocation == null || p.getNeighborhood().equalsIgnoreCase(filterLocation))
                .filter(p -> filterFlatType == null || p.getFlatTypes().containsKey(filterFlatType))
                .filter(p -> !checkVisibility || isProjectVisibleToCurrentUser(p))
                .filter(p -> !checkApplicationPeriod || p.isApplicationPeriodActive(currentDate))
                .filter(p -> !checkNotExpired || !p.isApplicationPeriodExpired(currentDate))
                .filter(p -> {
                    if (!checkEligibility && !checkAvailability)
                        return true;
                    if (currentUser instanceof HDBManager)
                        return true;

                    boolean eligibleForAnyType = p.getFlatTypes().keySet().stream()
                            .anyMatch(flatType -> this.canApplyForFlatType(flatType));
                    if (checkEligibility && !eligibleForAnyType)
                        return false;

                    if (!checkAvailability)
                        return true;

                    boolean eligibleAndAvailableExists = p.getFlatTypes().entrySet().stream()
                            .anyMatch(entry -> {
                                FlatType type = entry.getKey();
                                FlatTypeDetails details = entry.getValue();
                                return canApplyForFlatType(type) && details.getAvailableUnits() > 0;
                            });
                    return eligibleAndAvailableExists;
                })
                .sorted(Comparator.comparing(project -> project.getProjectName()))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a specific project should be visible to the currently logged-in
     * user.
     * - Managers see all projects.
     * - Applicants see projects that are globally visible OR projects they have
     * applied to (and application is not failed/withdrawn).
     * - Officers see projects that are globally visible OR projects they are
     * approved to handle.
     *
     * @param project The project to check visibility for.
     * @return true if the project is visible to the current user, false otherwise.
     */
    protected boolean isProjectVisibleToCurrentUser(Project project) {
        if (currentUser instanceof HDBManager)
            return true;

        boolean appliedToThis = false;
        if (currentUser instanceof Applicant) {
            Applicant appUser = (Applicant) currentUser;
            appliedToThis = project.getProjectName().equals(appUser.getAppliedProjectName()) &&
                    appUser.getApplicationStatus() != null &&
                    appUser.getApplicationStatus() != ApplicationStatus.UNSUCCESSFUL &&
                    appUser.getApplicationStatus() != ApplicationStatus.WITHDRAWN;
        }

        boolean isHandlingOfficer = false;
        if (currentUser instanceof HDBOfficer) {
            isHandlingOfficer = officerRegistrationService.getRegistrationsByOfficer(currentUser.getNric())
                    .stream()
                    .anyMatch(reg -> reg.getProjectName().equals(project.getProjectName()) &&
                            reg.getStatus() == OfficerRegistrationStatus.APPROVED);

        }

        return project.isVisible() || appliedToThis || isHandlingOfficer;
    }

    /**
     * Checks if the currently logged-in user is eligible to apply for a specific
     * flat type based on simplified HDB rules.
     * - Managers cannot apply.
     * - Singles (>= 35) can apply for 2-Room.
     * - Married (>= 21) can apply for 2-Room or 3-Room.
     * (Other statuses like divorced/widowed are not explicitly handled here).
     *
     * @param type The FlatType to check eligibility for.
     * @return true if the current user meets the basic criteria for the flat type,
     *         false otherwise.
     */
    protected boolean canApplyForFlatType(FlatType type) {
        if (currentUser instanceof HDBManager)
            return false;

        if (currentUser.getMaritalStatus() == MaritalStatus.SINGLE) {
            return currentUser.getAge() >= 35 && type == FlatType.TWO_ROOM;
        } else if (currentUser.getMaritalStatus() == MaritalStatus.MARRIED) {
            return currentUser.getAge() >= 21 && (type == FlatType.TWO_ROOM || type == FlatType.THREE_ROOM);
        }
        return false;
    }

    /**
     * Prompts the user to enter or clear filters for project location
     * (neighborhood) and flat type.
     * Updates the internal `filterLocation` and `filterFlatType` fields of the
     * controller instance.
     * These filters are used by
     * {@link #getFilteredProjects(boolean, boolean, boolean, boolean, boolean)}.
     */
    public void applyFilters() {
        System.out.println("\n--- Apply/Clear Filters ---");
        System.out.print("Enter neighborhood to filter by (current: "
                + (filterLocation == null ? "Any" : filterLocation) + ", leave blank to clear): ");
        String loc = scanner.nextLine().trim();
        filterLocation = loc.isEmpty() ? null : loc;

        System.out.print("Enter flat type to filter by (TWO_ROOM, THREE_ROOM, current: "
                + (filterFlatType == null ? "Any" : filterFlatType) + ", leave blank to clear): ");
        String typeStr = scanner.nextLine().trim();
        if (typeStr.isEmpty()) {
            filterFlatType = null;
        } else {
            try {
                FlatType parsedType = FlatType.fromString(typeStr);
                if (parsedType != null) {
                    filterFlatType = parsedType;
                } else {
                    System.out.println("Invalid flat type entered. Filter not changed.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid flat type format. Filter not changed.");
            }
        }
        System.out.println(
                "Filters updated. Current filters: Location=" + (filterLocation == null ? "Any" : filterLocation)
                        + ", FlatType=" + (filterFlatType == null ? "Any" : filterFlatType));
    }

    /**
     * Displays a list of projects in a formatted table and prompts the user for
     * common actions (like selection).
     * Shows project details including name, neighborhood, dates, visibility, and
     * flat type information
     * (available/total units, price).
     * For Applicants/Officers, it indicates eligibility ("Ineligible", "No Units")
     * for each flat type.
     * For Managers/Officers, it shows manager NRIC and officer slot usage.
     *
     * @param projectList The list of Project objects to display.
     * @param prompt      A title string to display above the project list (e.g.,
     *                    "Select Project to Edit").
     */
    protected void viewAndSelectProject(List<Project> projectList, String prompt) {
        if (projectList.isEmpty()) {
            System.out.println("No projects match the current criteria.");
            return;
        }

        System.out.println("\n--- " + prompt + " ---");
        System.out.println("Current Filters: Location=" + (filterLocation == null ? "Any" : filterLocation)
                + ", FlatType=" + (filterFlatType == null ? "Any" : filterFlatType));
        System.out.println(
                "--------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-3s %-15s %-12s %-10s %-10s %-8s %-25s\n", "#", "Project Name", "Neighborhood", "Open",
                "Close", "Visible", "Flat Types (Available/Total, Price, Eligibility)");
        System.out.println(
                "--------------------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < projectList.size(); i++) {
            Project p = projectList.get(i);
            System.out.printf("%-3d %-15s %-12s %-10s %-10s %-8s ",
                    i + 1,
                    p.getProjectName(),
                    p.getNeighborhood(),
                    DateUtils.formatDate(p.getApplicationOpeningDate()),
                    DateUtils.formatDate(p.getApplicationClosingDate()),
                    p.isVisible() ? "On" : "Off");

            String flatDetails = p.getFlatTypes().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        FlatType type = entry.getKey();
                        FlatTypeDetails details = entry.getValue();
                        String eligibilityMark = "";
                        if (currentUser instanceof Applicant) {
                            if (!canApplyForFlatType(type)) {
                                eligibilityMark = " (Ineligible)";
                            } else if (details.getAvailableUnits() <= 0) {
                                eligibilityMark = " (No Units)";
                            }
                        }
                        return String.format("%s: %d/%d ($%.0f)%s",
                                type.getDisplayName(), details.getAvailableUnits(), details.getTotalUnits(),
                                details.getSellingPrice(), eligibilityMark);
                    })
                    .collect(Collectors.joining(", "));
            System.out.println(flatDetails);

            if (currentUser.getRole() != UserRole.APPLICANT) {
                int approvedCount = p.getApprovedOfficerNrics().size();
                System.out.printf("%-3s %-15s %-12s %-10s %-10s %-8s %-25s\n", "", "", "", "", "", "",
                        "Mgr: " + p.getManagerNric() + ", Officers: " + approvedCount + "/" + p.getMaxOfficerSlots());
            }
            if (i < projectList.size() - 1)
                System.out.println("---");

        }
        System.out.println(
                "--------------------------------------------------------------------------------------------------------------------");
    }

    /**
     * Prompts the user to select a project by number from a previously displayed
     * list.
     * Handles user input, validates the choice against the list size, and allows
     * cancellation (input 0).
     *
     * @param projectList The list of projects from which the user is selecting.
     * @return The selected Project object, or null if the user cancels, enters
     *         invalid input, or the list is empty/null.
     */
    protected Project selectProjectFromList(List<Project> projectList) {
        if (projectList == null || projectList.isEmpty())
            return null;

        System.out.print("Enter the number of the project (or 0 to cancel): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine());
            if (choice == 0) {
                System.out.println("Operation cancelled.");
                return null;
            }
            if (choice >= 1 && choice <= projectList.size()) {
                return projectList.get(choice - 1);
            } else {
                System.out.println("Invalid choice number.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return null;
        }
    }

    /**
     * Prompts the user for integer input within a specified range.
     * Reprompts until valid input is received.
     *
     * @param prompt The message to display to the user.
     * @param min    The minimum allowed integer value (inclusive).
     * @param max    The maximum allowed integer value (inclusive).
     * @return The validated integer input from the user.
     */
    protected int getIntInput(String prompt, int min, int max) {
        int value = -1;
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    break;
                } else {
                    System.out.println("Input must be between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
        return value;
    }

    /**
     * Prompts the user for double (floating-point) input within a specified range.
     * Reprompts until valid input is received.
     *
     * @param prompt The message to display to the user.
     * @param min    The minimum allowed double value (inclusive).
     * @param max    The maximum allowed double value (inclusive).
     * @return The validated double input from the user.
     */
    protected double getDoubleInput(String prompt, double min, double max) {
        double value = -1.0;
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                value = Double.parseDouble(input);
                if (value >= min && value <= max) {
                    break;
                } else {
                    System.out.println("Input must be between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return value;
    }

    /**
     * Prompts the user for date input in "yyyy-MM-dd" format.
     * Uses {@link Parsers.Dparse} for parsing and validation.
     * Reprompts until a valid date is entered or if blank input is disallowed.
     *
     * @param prompt     The message to display to the user.
     * @param allowBlank If true, allows the user to press Enter to skip input
     *                   (returns null). If false, requires valid date input.
     * @return The validated Date object, or null if blank input is allowed and
     *         entered.
     */
    protected Date getDateInput(String prompt, boolean allowBlank) {
        Date date = null;
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && allowBlank) {
                return null;
            }
            if (input.isEmpty() && !allowBlank) {
                System.out.println("Input cannot be empty.");
                continue;
            }
            date = Dparse.parseDate(input);
            if (date != null) {
                break;
            }
        }
        return date;
    }
}
