/**
 * Controller handling actions performed by an HDB Officer related to managing the flat booking process
 * for applicants whose applications have been approved (status SUCCESSFUL).
 * Inherits common functionality from BaseController.
 *
 * @author Patrick
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
import Models.HDBOfficer;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class BookingOfficerController extends BaseController {
    /**
     * Constructs a new BookingOfficerController.
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
    public BookingOfficerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
    }

    /**
     * Guides the HDB Officer through the process of booking a flat for a selected
     * applicant.
     * - Checks if the officer is currently handling an active project.
     * - Retrieves and displays applicants for the handling project whose status is
     * SUCCESSFUL.
     * - Prompts the officer to select an applicant.
     * - Performs pre-booking checks:
     * - Verifies the application status is still SUCCESSFUL.
     * - Verifies the applicant profile doesn't already show them as BOOKED.
     * - Verifies the application has a valid flat type specified.
     * - Checks if units are still available for the applied flat type in the
     * project.
     * - Prompts for confirmation.
     * - If confirmed:
     * - Decrements the available unit count in the project's FlatTypeDetails.
     * - Updates the BTOApplication status to BOOKED.
     * - Updates the Applicant's profile status to BOOKED and sets the
     * bookedFlatType.
     * - Generates and displays a booking receipt.
     * - Saves the updated application, project (due to unit count change), and
     * potentially user data.
     */
    public void manageFlatBooking() {
        if (!(currentUser instanceof HDBOfficer))
            return;
        HDBOfficer officer = (HDBOfficer) currentUser;

        Project project = getOfficerHandlingProject(officer);

        if (project == null) {
            System.out.println("You need to be handling an active project to manage flat bookings.");
            return;
        }
        String handlingProjectName = project.getProjectName();

        System.out.println("\n--- Flat Booking Management for Project: " + handlingProjectName + " ---");

        List<BTOApplication> successfulApps = applicationService.getApplicationsByProject(handlingProjectName)
                .stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUCCESSFUL)
                .sorted(Comparator.comparing(app -> app.getApplicationDate()))
                .collect(Collectors.toList());

        if (successfulApps.isEmpty()) {
            System.out.println("No applicants with status SUCCESSFUL found for this project (ready for booking).");
            return;
        }

        System.out.println("Applicants with SUCCESSFUL status (ready for booking):");
        for (int i = 0; i < successfulApps.size(); i++) {
            BTOApplication app = successfulApps.get(i);
            User applicantUser = userService.findUserByNric(app.getApplicantNric());
            System.out.printf("%d. NRIC: %s | Name: %-15s | Applied Type: %-8s | App Date: %s\n",
                    i + 1,
                    app.getApplicantNric(),
                    applicantUser != null ? applicantUser.getName() : "N/A",
                    app.getFlatTypeApplied() != null ? app.getFlatTypeApplied().getDisplayName() : "N/A",
                    DateUtils.formatDate(app.getApplicationDate()));
        }

        int choice = getIntInput("Enter the number of the applicant to process booking for (or 0 to cancel): ", 0,
                successfulApps.size());

        if (choice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }

        BTOApplication applicationToBook = successfulApps.get(choice - 1);
        User applicantUser = userService.findUserByNric(applicationToBook.getApplicantNric());

        if (!(applicantUser instanceof Applicant)) {
            System.out.println(
                    "Error: Applicant data not found or invalid for NRIC " + applicationToBook.getApplicantNric()
                            + ". Cannot process booking.");
            return;
        }
        Applicant applicant = (Applicant) applicantUser;

        if (applicationToBook.getStatus() != ApplicationStatus.SUCCESSFUL) {
            System.out.println("Error: Applicant status is no longer SUCCESSFUL (Current: "
                    + applicationToBook.getStatus() + "). Cannot proceed with booking.");
            return;
        }
        if (applicant.hasBooked()) {
            System.out.println("Error: Applicant " + applicant.getNric()
                    + " has already booked a flat according to their profile. Booking cancelled.");
            return;
        }
        FlatType appliedFlatType = applicationToBook.getFlatTypeApplied();
        if (appliedFlatType == null) {
            System.out.println("Error: Application record does not have a valid flat type specified. Cannot book.");
            return;
        }
        Project currentProjectState = projectService.findProjectByName(handlingProjectName);
        if (currentProjectState == null) {
            System.out.println("Error: Could not retrieve current project state. Booking cancelled.");
            return;
        }
        FlatTypeDetails details = currentProjectState.getMutableFlatTypeDetails(appliedFlatType);
        if (details == null) {
            System.out.println("Error: Flat type details not found for " + appliedFlatType.getDisplayName()
                    + " in project " + currentProjectState.getProjectName() + ". Cannot book.");
            return;
        }
        if (details.getAvailableUnits() <= 0) {
            System.out.println("Error: No available units for the applied flat type ("
                    + appliedFlatType.getDisplayName() + ") at this moment. Booking cannot proceed.");
            return;
        }

        System.out.println("\n--- Confirm Booking ---");
        System.out.println("Applicant: " + applicant.getName() + " (" + applicant.getNric() + ")");
        System.out.println("Project: " + applicationToBook.getProjectName());
        System.out.println("Flat Type: " + appliedFlatType.getDisplayName());
        System.out.println("Available Units Before Booking: " + details.getAvailableUnits());
        System.out.printf("Selling Price: $%.2f\n", details.getSellingPrice());

        System.out.print("\nConfirm booking for this applicant? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            if (!details.decrementAvailableUnits()) {
                System.out.println(
                        "Error: Failed to decrement unit count (Units might have just become zero). Booking cancelled.");
                return;
            }

            applicationToBook.setStatus(ApplicationStatus.BOOKED);

            applicant.setApplicationStatus(ApplicationStatus.BOOKED);
            applicant.setBookedFlatType(appliedFlatType);

            System.out.println("Booking confirmed successfully!");
            System.out.println("Applicant status updated to BOOKED.");
            System.out.println(
                    "Remaining units for " + appliedFlatType.getDisplayName() + ": " + details.getAvailableUnits());

            generateBookingReceipt(applicant, applicationToBook, currentProjectState);

            applicationService.saveApplications(applicationService.getAllApplications());
            projectService.saveProjects(projectService.getAllProjects());

        } else {
            System.out.println("Booking cancelled.");
        }
    }

    /**
     * Generates and displays a booking receipt for the applicant.
     * Displays the applicant's details, project details, and booking status.
     * 
     * @param applicant   The applicant whose booking receipt is being generated.
     * @param application The BTOApplication associated with the booking.
     * @param project     The project associated with the booking.
     */

    private void generateBookingReceipt(Applicant applicant, BTOApplication application, Project project) {
        System.out.println("\n================ BTO Booking Receipt ================");
        System.out.println(" Receipt Generated: " + DateUtils.formatDate(DateUtils.getCurrentDate()) + " by Officer "
                + currentUser.getNric());
        System.out.println("-----------------------------------------------------");
        System.out.println(" Applicant Details:");
        System.out.println("   Name:          " + applicant.getName());
        System.out.println("   NRIC:          " + applicant.getNric());
        System.out.println("   Age:           " + applicant.getAge());
        System.out.println("   Marital Status:" + applicant.getMaritalStatus());
        System.out.println("-----------------------------------------------------");
        System.out.println(" Booking Details:");
        System.out.println("   Project Name:  " + project.getProjectName());
        System.out.println("   Neighborhood:  " + project.getNeighborhood());
        System.out.println("   Booked Flat:   "
                + (application.getFlatTypeApplied() != null ? application.getFlatTypeApplied().getDisplayName()
                        : "N/A"));
        FlatTypeDetails details = project.getFlatTypeDetails(application.getFlatTypeApplied());
        if (details != null) {
            System.out.printf("   Selling Price: $%.2f\n", details.getSellingPrice());
        } else {
            System.out.println("   Selling Price: N/A");
        }
        System.out.println("   Booking Status:" + application.getStatus());
        System.out.println("   Application ID:" + application.getApplicationId());
        System.out.println("-----------------------------------------------------");
        System.out.println(" Thank you for choosing HDB!");
        System.out.println("=====================================================");
    }
}
