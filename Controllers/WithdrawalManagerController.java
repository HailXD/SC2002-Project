/**
 * Controller handling actions performed by an HDB Manager related to managing
 * BTO application withdrawal requests (status PENDING_WITHDRAWAL) for projects they manage.
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


public class WithdrawalManagerController extends BaseController {
/**
 * Constructs a new WithdrawalManagerController.
 * Ensures the current user is an HDBManager.
 *
 * @param userService Service for user data access.
 * @param projectService Service for project data access.
 * @param applicationService Service for application data access.
 * @param officerRegistrationService Service for officer registration data access.
 * @param enquiryService Service for enquiry data access.
 * @param currentUser The currently logged-in User (must be HDBManager).
 * @param scanner Scanner instance for reading user input.
 * @param authController Controller for authentication tasks.
 * @throws IllegalArgumentException if the currentUser is not an HDBManager.
 */
    public WithdrawalManagerController(IUserService userService, IProjectService projectService,
                                   IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
                                   IEnquiryService enquiryService,
                                   User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner, authController);
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("WithdrawalManagerController requires an HDBManager user.");
        }
    }
/**
 * Guides the HDB Manager through approving or rejecting pending withdrawal requests for applications in their managed projects.
 * - Finds all applications with status PENDING_WITHDRAWAL for projects managed by the current manager.
 * - Displays these pending requests, showing applicant details and the inferred original status before withdrawal.
 * - Prompts the manager to select a request to process.
 * - Validates applicant and project data.
 * - Determines the original status (using stored value if available, otherwise inferring).
 * - Verifies the application is still in PENDING_WITHDRAWAL status.
 * - Prompts for Approve (A) or Reject (R) action.
 * - If Approve:
 *   - Determines the final status based on the original status (UNSUCCESSFUL if originally BOOKED/SUCCESSFUL, WITHDRAWN if originally PENDING).
 *   - If originally BOOKED, attempts to release the flat unit back to the project by incrementing available units in FlatTypeDetails.
 *   - Updates the application status to the final determined status.
 *   - Updates the applicant's profile status and clears their booked flat type.
 *   - Saves application data, and project data if a unit was released.
 * - If Reject:
 *   - Reverts the application status and the applicant's profile status back to the original status determined earlier.
 *   - Saves the application data.
 */
     public void manageWithdrawalRequests() {
        System.out.println("\n--- Manage Withdrawal Requests ---");
        List<String> myProjectNames = projectService.getProjectsManagedBy(currentUser.getNric())
                .stream()
                .map((Project p) -> p.getProjectName())
                .collect(Collectors.toList());

        if (myProjectNames.isEmpty()) {
            System.out.println("You are not managing any projects to have withdrawal requests.");
            return;
        }

        List<BTOApplication> pendingWithdrawals = applicationService.getAllApplications().values().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING_WITHDRAWAL)
                .filter(app -> myProjectNames.contains(app.getProjectName()))
                .sorted(Comparator.comparing((BTOApplication app) -> app.getApplicationDate()))
                .collect(Collectors.toList());

        if (pendingWithdrawals.isEmpty()) {
            System.out.println("No pending withdrawal requests found for the projects you manage.");
            return;
        }

        System.out.println("--- Pending Withdrawal Requests ---");
        for (int i = 0; i < pendingWithdrawals.size(); i++) {
            BTOApplication app = pendingWithdrawals.get(i);
            User applicantUser = userService.findUserByNric(app.getApplicantNric());

            ApplicationStatus statusBefore = app.getStatusBeforeWithdrawal();
            if (statusBefore == null) {
                 statusBefore = inferStatusBeforeWithdrawal(app, (applicantUser instanceof Applicant) ? (Applicant) applicantUser : null);
                 System.out.print(" (Inferred Original: " + statusBefore + ")");
            } else {
                System.out.print(" (Original: " + statusBefore + ")");
            }

            System.out.printf("\n%d. NRIC: %s | Name: %-15s | Project: %-15s | Type: %-8s | App Date: %s",
                    i + 1,
                    app.getApplicantNric(),
                    applicantUser != null ? applicantUser.getName() : "N/A",
                    app.getProjectName(),
                    app.getFlatTypeApplied() != null ? app.getFlatTypeApplied().getDisplayName() : "N/A",
                    DateUtils.formatDate(app.getApplicationDate()));
            System.out.println();
        }

        int choice = getIntInput("Enter number to Approve/Reject withdrawal (or 0 to skip): ", 0, pendingWithdrawals.size());

        if (choice >= 1) {
            BTOApplication appToProcess = pendingWithdrawals.get(choice - 1);
            User applicantUser = userService.findUserByNric(appToProcess.getApplicantNric());

            if (!(applicantUser instanceof Applicant)) {
                System.out.println("Error: Applicant data not found or invalid for NRIC "
                        + appToProcess.getApplicantNric() + ". Cannot process withdrawal.");
                return;
            }
            Applicant applicant = (Applicant) applicantUser;

            Project project = projectService.findProjectByName(appToProcess.getProjectName());
            if (project == null) {
                System.out.println("Error: Project data not found for application "
                        + appToProcess.getApplicationId() + ". Cannot process withdrawal.");
                return;
            }

            ApplicationStatus originalStatus = appToProcess.getStatusBeforeWithdrawal();
            if (originalStatus == null) {
                originalStatus = inferStatusBeforeWithdrawal(appToProcess, applicant);
                 System.out.println("Note: Original status inferred as " + originalStatus + " due to missing data.");
            }

             if (appToProcess.getStatus() != ApplicationStatus.PENDING_WITHDRAWAL) {
                  System.out.println("Error: Application status is no longer PENDING_WITHDRAWAL (Current: " + appToProcess.getStatus() + "). Cannot process.");
                  return;
             }


            System.out.print("Approve or Reject withdrawal request for Applicant " + applicant.getName() + "? (A/R): ");
            String action = scanner.nextLine().trim().toUpperCase();

            if (action.equals("A")) {
                ApplicationStatus finalStatus;
                boolean releasedUnit = false;

                if (originalStatus == ApplicationStatus.BOOKED) {
                    finalStatus = ApplicationStatus.UNSUCCESSFUL;
                    FlatType bookedType = appToProcess.getFlatTypeApplied();
                    if (bookedType != null) {
                        FlatTypeDetails details = project.getMutableFlatTypeDetails(bookedType);
                        if (details != null) {
                            if (details.incrementAvailableUnits()) {
                                releasedUnit = true;
                                System.out.println("Unit for " + bookedType.getDisplayName()
                                        + " released back to project " + project.getProjectName() + ". Available: " + details.getAvailableUnits());
                            } else {
                                System.err.println("Error: Could not increment available units for " + bookedType.getDisplayName() + " (already at max?). Check data consistency.");
                            }
                        } else {
                            System.err.println("Error: Could not find flat details for " + bookedType.getDisplayName() + " during withdrawal approval.");
                        }
                    } else {
                        System.err.println("Error: Cannot determine booked flat type to release unit during withdrawal approval.");
                    }
                } else if (originalStatus == ApplicationStatus.SUCCESSFUL) {
                    finalStatus = ApplicationStatus.UNSUCCESSFUL;
                } else {
                    finalStatus = ApplicationStatus.WITHDRAWN;
                }

                appToProcess.setStatus(finalStatus);

                applicant.setApplicationStatus(finalStatus);
                applicant.setBookedFlatType(null);

                System.out.println("Withdrawal request Approved. Application status set to " + finalStatus + ".");

                applicationService.saveApplications(applicationService.getAllApplications());
                if (releasedUnit) {
                    projectService.saveProjects(projectService.getAllProjects());
                }

            } else if (action.equals("R")) {
                appToProcess.setStatus(originalStatus);

                applicant.setApplicationStatus(originalStatus);

                System.out.println("Withdrawal request Rejected. Application status reverted to " + originalStatus + ".");
                applicationService.saveApplications(applicationService.getAllApplications());

            } else {
                System.out.println("Invalid action ('A' or 'R' expected). No change made.");
            }
        } else if (choice != 0) {
            System.out.println("Invalid choice.");
        }
    }

    /**
     * Infers the application status before withdrawal based on the current application and applicant data.
     *
     * @param app The BTOApplication object representing the application.
     * @param applicant The Applicant object representing the applicant.
     * @return The inferred ApplicationStatus before withdrawal.
     */
    /** 
     * @param app
     * @param applicant
     * @return
     */
    private ApplicationStatus inferStatusBeforeWithdrawal(BTOApplication app, Applicant applicant) {
         if (applicant != null && applicant.hasBooked() && applicant.getBookedFlatType() == app.getFlatTypeApplied()) {
             return ApplicationStatus.BOOKED;
         }
         else if (applicant != null && applicant.getApplicationStatus() == ApplicationStatus.SUCCESSFUL && applicant.getAppliedProjectName().equals(app.getProjectName())) {
              return ApplicationStatus.SUCCESSFUL;
         }
         else if (app.getFlatTypeApplied() != null) {
              return ApplicationStatus.SUCCESSFUL;
         }
         return ApplicationStatus.PENDING;
    }
}
