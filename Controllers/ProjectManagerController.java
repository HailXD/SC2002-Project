/**
 * Controller handling actions performed by an HDB Manager specifically related to
 * BTO project management (Create, Read, Update, Delete - CRUD) and visibility control.
 * Requires an IEnquiryService for cleanup during project deletion.
 * Inherits common functionality from BaseController.
 *
 * @author Kishore Kumar
 */
package Controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import Enums.ApplicationStatus;
import Enums.FlatType;
import Enums.OfficerRegistrationStatus;
import Models.BTOApplication;
import Models.Enquiry;
import Models.FlatTypeDetails;
import Models.HDBManager;
import Models.OfficerRegistration;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class ProjectManagerController extends BaseController {

    private final IEnquiryService enquiryService;

    /**
     * Constructor for ProjectManagerController
     *
     * @param userService                user service instance
     * @param projectService             project service instance
     * @param applicationService         application service instance
     * @param officerRegistrationService officer registration service instance
     * @param enquiryService             enquiry service instance (needed for
     *                                   deletion cleanup)
     * @param currentUser                currently logged-in user (must be an
     *                                   HDBManager)
     * @param scanner                    scanner for user input
     * @param authController             authentication controller
     */
    public ProjectManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        this.enquiryService = enquiryService;
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("ProjectManagerController requires an HDBManager user.");
        }
    }

    /**
     * Guides the HDB Manager through the process of creating a new BTO project.
     * - Prompts for project name (checks for uniqueness), neighborhood.
     * - Prompts for flat type details (total units, price for 2-Room and 3-Room).
     * Ensures at least one type is added.
     * - Prompts for application opening and closing dates (validates order and
     * checks for overlap with other projects managed by the same manager).
     * - Prompts for the maximum number of HDB officer slots (1-10).
     * - Creates a new Project object (initially visibility off, no approved
     * officers).
     * - Adds the project using the project service and saves it immediately.
     */
    public void createProject() {
        HDBManager manager = (HDBManager) currentUser;

        System.out.println("\n--- Create New BTO Project ---");

        String projectName;
        while (true) {
            System.out.print("Enter Project Name: ");
            projectName = scanner.nextLine().trim();
            if (projectName.isEmpty()) {
                System.out.println("Project name cannot be empty.");
            } else if (projectService.findProjectByName(projectName) != null) {
                System.out.println("Project name already exists. Please choose a unique name.");
            } else {
                break;
            }
        }

        System.out.print("Enter Neighborhood: ");
        String neighborhood = scanner.nextLine().trim();
        if (neighborhood.isEmpty()) {
            System.out.println("Neighborhood cannot be empty. Creation cancelled.");
            return;
        }

        Map<FlatType, FlatTypeDetails> flatTypes = new HashMap<>();
        System.out.println("--- Flat Type Details ---");
        int units2Room = getIntInput("Enter total number of 2-Room units (0 if none): ", 0, 9999);
        if (units2Room > 0) {
            double price2Room = getDoubleInput("Enter selling price for 2-Room units: $", 0, Double.MAX_VALUE);
            flatTypes.put(FlatType.TWO_ROOM, new FlatTypeDetails(units2Room, units2Room, price2Room));
        }
        int units3Room = getIntInput("Enter total number of 3-Room units (0 if none): ", 0, 9999);
        if (units3Room > 0) {
            double price3Room = getDoubleInput("Enter selling price for 3-Room units: $", 0, Double.MAX_VALUE);
            flatTypes.put(FlatType.THREE_ROOM, new FlatTypeDetails(units3Room, units3Room, price3Room));
        }

        if (flatTypes.isEmpty()) {
            System.out.println(
                    "Error: Project must have at least one type of flat (2-Room or 3-Room). Creation cancelled.");
            return;
        }

        Date openingDate;
        Date closingDate;
        while (true) {
            openingDate = getDateInput("Enter Application Opening Date (yyyy-MM-dd): ", false);
            closingDate = getDateInput("Enter Application Closing Date (yyyy-MM-dd): ", false);

            if (openingDate == null || closingDate == null) {
                System.out.println("Dates cannot be empty. Please re-enter.");
                continue;
            }
            if (closingDate.before(openingDate)) {
                System.out.println("Closing date cannot be before opening date. Please re-enter.");
                continue;
            }

            Project proposedProjectDates = new Project("__temp__", "__temp__", flatTypes, openingDate, closingDate,
                    manager.getNric(), 0, null, true);
            boolean overlapsWithActive = projectService.getProjectsManagedBy(manager.getNric())
                    .stream()
                    .anyMatch(existingProject -> checkDateOverlap(proposedProjectDates, existingProject));

            if (overlapsWithActive) {
                System.out.println("Error: The specified application period overlaps with another project you manage.");
                System.out.println(
                        "Please enter different dates or manage the visibility/dates of the existing project.");
            } else {
                break;
            }
        }

        int maxOfficers = getIntInput("Enter Maximum HDB Officer Slots (1-10): ", 1, 10);

        Project newProject = new Project(projectName, neighborhood, flatTypes, openingDate, closingDate,
                manager.getNric(), maxOfficers, new ArrayList<>(), false);

        projectService.addProject(newProject);
        System.out.println("Project '" + projectName + "' created successfully. Visibility is currently OFF.");
        projectService.saveProjects(projectService.getAllProjects());
    }

    /**
     * Allows the HDB Manager to edit details of projects they manage.
     * - Displays a list of managed projects for selection.
     * - Allows editing of:
     * - Neighborhood.
     * - Selling prices for existing flat types (cannot change types or total units
     * easily).
     * - Application opening and closing dates (validates order and checks for
     * overlap with *other* managed projects).
     * - Maximum officer slots (ensures new max is not less than current approved
     * count and within 1-10 range).
     * - Updates the Project object directly or via setters.
     * - Saves all projects after the edit attempt.
     */
    public void editProject() {
        System.out.println("\n--- Edit BTO Project ---");
        List<Project> myProjects = getManagedProjects(false);
        if (myProjects.isEmpty()) {
            return;
        }

        viewAndSelectProject(myProjects, "Select Project to Edit");
        Project projectToEdit = selectProjectFromList(myProjects);
        if (projectToEdit == null)
            return;

        System.out
                .println("Editing Project: " + projectToEdit.getProjectName() + " (Leave blank to keep current value)");

        System.out.print("Enter new Neighborhood [" + projectToEdit.getNeighborhood() + "]: ");
        String newNeighborhood = scanner.nextLine().trim();
        if (!newNeighborhood.isEmpty()) {
            projectToEdit.setNeighborhood(newNeighborhood);
        }

        Map<FlatType, FlatTypeDetails> existingFlatTypesView = projectToEdit.getFlatTypes();

        for (FlatType type : FlatType.values()) {
            if (existingFlatTypesView.containsKey(type)) {
                FlatTypeDetails currentDetails = projectToEdit.getMutableFlatTypeDetails(type);
                if (currentDetails != null) {
                    System.out.println("--- Edit " + type.getDisplayName() + " ---");
                    System.out
                            .print("Enter new selling price [" + String.format("%.2f", currentDetails.getSellingPrice())
                                    + "] (leave blank to keep): $");
                    String priceInput = scanner.nextLine().trim();
                    if (!priceInput.isEmpty()) {
                        try {
                            double newPrice = Double.parseDouble(priceInput);
                            if (newPrice >= 0) {
                                currentDetails.setSellingPrice(newPrice);
                            } else {
                                System.out.println("Price cannot be negative. Keeping original price.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid price format. Keeping original price.");
                        }
                    }
                }
            }
        }

        Date originalOpening = projectToEdit.getApplicationOpeningDate();
        Date originalClosing = projectToEdit.getApplicationClosingDate();
        Date newOpeningDate = getDateInput("Enter new Opening Date (yyyy-MM-dd) ["
                + DateUtils.formatDate(originalOpening) + "] (leave blank to keep): ", true);
        Date newClosingDate = getDateInput("Enter new Closing Date (yyyy-MM-dd) ["
                + DateUtils.formatDate(originalClosing) + "] (leave blank to keep): ", true);

        Date finalOpening = (newOpeningDate != null) ? newOpeningDate : originalOpening;
        Date finalClosing = (newClosingDate != null) ? newClosingDate : originalClosing;

        boolean datesChanged = (newOpeningDate != null || newClosingDate != null);
        boolean datesValid = true;

        if (finalClosing.before(finalOpening)) {
            System.out.println("Error: Closing date cannot be before opening date. Dates not updated.");
            datesValid = false;
        }

        if (datesChanged && datesValid) {
            Project proposedProjectDates = new Project(
                    projectToEdit.getProjectName(), projectToEdit.getNeighborhood(), projectToEdit.getFlatTypes(),
                    finalOpening, finalClosing, projectToEdit.getManagerNric(), projectToEdit.getMaxOfficerSlots(),
                    projectToEdit.getApprovedOfficerNrics(), projectToEdit.isVisible());

            boolean overlapsWithOther = projectService.getProjectsManagedBy(currentUser.getNric())
                    .stream()
                    .filter(p -> !p.getProjectName().equals(projectToEdit.getProjectName()))
                    .anyMatch(otherProject -> checkDateOverlap(proposedProjectDates, otherProject));

            if (overlapsWithOther) {
                System.out.println(
                        "Error: The new application period overlaps with another project you manage. Dates not updated.");
                datesValid = false;
            }
        }

        if (datesValid) {
            if (newOpeningDate != null)
                projectToEdit.setApplicationOpeningDate(newOpeningDate);
            if (newClosingDate != null)
                projectToEdit.setApplicationClosingDate(newClosingDate);
            if (datesChanged)
                System.out.println("Application dates updated.");
        }

        int currentMaxSlots = projectToEdit.getMaxOfficerSlots();
        int currentApprovedCount = projectToEdit.getApprovedOfficerNrics().size();
        int minSlots = currentApprovedCount;
        System.out.print("Enter new Max Officer Slots [" + currentMaxSlots + "] (min " + minSlots
                + ", max 10, leave blank to keep): ");
        String slotsInput = scanner.nextLine().trim();
        if (!slotsInput.isEmpty()) {
            try {
                int newMaxSlots = Integer.parseInt(slotsInput);
                projectToEdit.setMaxOfficerSlots(newMaxSlots);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Max slots not changed.");
            }
        }

        System.out.println("Project details update attempt complete.");
        projectService.saveProjects(projectService.getAllProjects());
    }

    /**
     * Allows the HDB Manager to delete a project they manage.
     * - Displays a list of managed projects for selection.
     * - Performs checks to prevent deletion if the project has active associations:
     * - Active BTO applications (Pending, Successful, Booked, PendingWithdrawal).
     * - Active Officer registrations (Pending, Approved).
     * - If no active associations exist, prompts for confirmation.
     * - If confirmed:
     * - Removes the project using the project service.
     * - Removes all associated BTO applications using the application service.
     * - Removes all associated officer registrations using the officer registration
     * service.
     * - Removes all associated enquiries using the enquiry service.
     * - Saves changes after each removal step.
     */
    public void deleteProject() {
        System.out.println("\n--- Delete BTO Project ---");
        List<Project> myProjects = getManagedProjects(false);
        if (myProjects.isEmpty())
            return;

        viewAndSelectProject(myProjects, "Select Project to Delete");
        Project projectToDelete = selectProjectFromList(myProjects);
        if (projectToDelete == null)
            return;

        boolean hasActiveApplications = applicationService.getApplicationsByProject(projectToDelete.getProjectName())
                .stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.PENDING ||
                        app.getStatus() == ApplicationStatus.SUCCESSFUL ||
                        app.getStatus() == ApplicationStatus.BOOKED ||
                        app.getStatus() == ApplicationStatus.PENDING_WITHDRAWAL);

        boolean hasActiveRegistrations = officerRegistrationService
                .getRegistrationsByProject(projectToDelete.getProjectName())
                .stream()
                .anyMatch(reg -> reg.getStatus() == OfficerRegistrationStatus.PENDING ||
                        reg.getStatus() == OfficerRegistrationStatus.APPROVED);

        if (hasActiveApplications || hasActiveRegistrations) {
            System.out.println("Error: Cannot delete project '" + projectToDelete.getProjectName() + "'.");
            if (hasActiveApplications)
                System.out.println("- It has active BTO applications (Pending/Successful/Booked/PendingWithdrawal).");
            if (hasActiveRegistrations)
                System.out.println("- It has active Officer registrations (Pending/Approved).");
            System.out.println(
                    "Resolve these associations (e.g., reject applications/registrations, wait for booking/withdrawal) before deleting.");
            return;
        }

        System.out.print("Are you sure you want to permanently delete project '" + projectToDelete.getProjectName()
                + "'? This will also remove associated historical applications, registrations, and enquiries. (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            String deletedProjectName = projectToDelete.getProjectName();

            if (projectService.removeProject(projectToDelete)) {
                System.out.println("Project '" + deletedProjectName + "' deleted successfully.");
                projectService.saveProjects(projectService.getAllProjects());

                List<String> appIdsToRemove = applicationService.getApplicationsByProject(deletedProjectName)
                        .stream()
                        .map((BTOApplication app) -> app.getApplicationId())
                        .collect(Collectors.toList());
                int removedAppCount = 0;
                for (String appId : appIdsToRemove) {
                    if (applicationService.removeApplication(appId)) {
                        removedAppCount++;
                    }
                }
                if (removedAppCount > 0) {
                    System.out.println("Removed " + removedAppCount + " associated applications.");
                    applicationService.saveApplications(applicationService.getAllApplications());
                }

                List<String> regIdsToRemove = officerRegistrationService.getRegistrationsByProject(deletedProjectName)
                        .stream()
                        .map((OfficerRegistration oR) -> oR.getRegistrationId())
                        .collect(Collectors.toList());
                int removedRegCount = 0;
                for (String regId : regIdsToRemove) {
                    if (officerRegistrationService.removeRegistration(regId)) {
                        removedRegCount++;
                    }
                }
                if (removedRegCount > 0) {
                    System.out.println("Removed " + removedRegCount + " associated officer registrations.");
                    officerRegistrationService
                            .saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
                }

                List<String> enqIdsToRemove = enquiryService.getEnquiriesByProject(deletedProjectName)
                        .stream()
                        .map((Enquiry e) -> e.getEnquiryId())
                        .collect(Collectors.toList());
                int removedEnqCount = 0;
                for (String enqId : enqIdsToRemove) {
                    if (enquiryService.removeEnquiry(enqId)) {
                        removedEnqCount++;
                    }
                }
                if (removedEnqCount > 0) {
                    System.out.println("Removed " + removedEnqCount + " associated enquiries.");
                    enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
                }

            } else {
                System.err.println("Error: Failed to remove project from service layer.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    /**
     * Allows the HDB Manager to toggle the visibility status (On/Off) of a project
     * they manage.
     * - Displays a list of managed projects for selection.
     * - Toggles the `visibility` flag on the selected Project object.
     * - Saves all projects after the change.
     */
    public void toggleProjectVisibility() {
        System.out.println("\n--- Toggle Project Visibility ---");
        List<Project> myProjects = getManagedProjects(false);
        if (myProjects.isEmpty())
            return;

        viewAndSelectProject(myProjects, "Select Project to Toggle Visibility");
        Project projectToToggle = selectProjectFromList(myProjects);

        if (projectToToggle != null) {
            boolean currentVisibility = projectToToggle.isVisible();
            projectToToggle.setVisibility(!currentVisibility);
            System.out.println("Project '" + projectToToggle.getProjectName() + "' visibility toggled to "
                    + (projectToToggle.isVisible() ? "ON" : "OFF") + ".");
            projectService.saveProjects(projectService.getAllProjects());
        }
    }

    /**
     * Displays all BTO projects in the system from a manager's perspective.
     * This view ignores visibility, eligibility, availability, and application
     * period checks,
     * showing all projects regardless of their status or the manager's filters.
     * Uses the standard project display format.
     */
    public void viewAllProjects() {
        System.out.println("\n--- View All Projects (Manager View) ---");
        List<Project> displayProjects = getFilteredProjects(false, false, false, false, false);
        viewAndSelectProject(displayProjects, "All BTO Projects (Manager View)");
    }

    /**
     * Displays only the projects managed by the currently logged-in HDB Manager.
     * Applies the manager's currently set view filters (location, flat type).
     * Uses the standard project display format.
     */
    public void viewMyProjects() {
        System.out.println("\n--- View My Managed Projects ---");
        List<Project> myProjects = getManagedProjects(true);
        viewAndSelectProject(myProjects, "Projects Managed By You");
    }

    /**
     * Get the list of projects managed by the current user.
     * * @param applyUserFilters whether to apply user filters (location, flat
     * type).
     * 
     * @return List of projects managed by the current user, filtered and sorted.
     */
    private List<Project> getManagedProjects(boolean applyUserFilters) {
        List<Project> managed = projectService.getProjectsManagedBy(currentUser.getNric());

        if (applyUserFilters) {
            managed = managed.stream()
                    .filter(p -> filterLocation == null || p.getNeighborhood().equalsIgnoreCase(filterLocation))
                    .filter(p -> filterFlatType == null || p.getFlatTypes().containsKey(filterFlatType))
                    .collect(Collectors.toList());
        }

        managed.sort(Comparator.comparing((Project p) -> p.getProjectName()));

        if (managed.isEmpty()) {
            String filterMsg = (applyUserFilters && (filterLocation != null || filterFlatType != null))
                    ? " matching the current filters."
                    : ".";
            System.out.println("You are not managing any projects" + filterMsg);
        }
        return managed;
    }
}