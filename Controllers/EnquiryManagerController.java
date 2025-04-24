/**
 * Controller handling actions performed by an HDB Manager related to viewing and managing enquiries.
 * Allows viewing all enquiries system-wide or viewing/replying to enquiries for managed projects.
 * Requires an IEnquiryService. Inherits common functionality from BaseController.
 *
 * @author Kai Wang
 */
package Controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Models.Enquiry;
import Models.HDBManager;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class EnquiryManagerController extends BaseController {

    private final IEnquiryService enquiryService;

    /**
     * Constructs a new EnquiryManagerController.
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
    public EnquiryManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        this.enquiryService = enquiryService;
        if (!(currentUser instanceof HDBManager)) {
            throw new IllegalArgumentException("EnquiryManagerController requires an HDBManager user.");
        }
    }

    /**
     * Displays all enquiries present in the system, regardless of project or
     * manager assignment.
     * Retrieves all enquiries from the enquiry service.
     * Sorts them by project name, then by enquiry date (newest first).
     * Prints details for each enquiry using a helper method.
     */
    public void viewAllEnquiries() {
        System.out.println("\n--- View Enquiries (ALL Projects) ---");
        List<Enquiry> allEnquiries = enquiryService.getAllEnquiries();

        if (allEnquiries.isEmpty()) {
            System.out.println("No enquiries found in the system.");
            return;
        }

        allEnquiries.stream()
                .sorted(Comparator.comparing((Enquiry e) -> e.getProjectName())
                        .thenComparing((Enquiry e) -> e.getEnquiryDate(), Comparator.reverseOrder()))
                .forEach(e -> {
                    printEnquiryDetails(e);
                    System.out.println("----------------------------------------");
                });
    }

    /**
     * Allows the HDB Manager to view and reply to enquiries specifically for the
     * projects they manage.
     * Optionally applies the manager's current view filters (location, flat type)
     * to the list of managed projects considered.
     * - Retrieves enquiries related to the filtered list of managed projects.
     * - Separates enquiries into unreplied and replied lists.
     * - Displays unreplied enquiries and prompts the manager to select one to reply
     * to.
     * - If an enquiry is selected, prompts for the reply text.
     * - Sets the reply details on the Enquiry object using its `setReply` method.
     * - Saves the updated enquiry list via the enquiry service.
     * - Displays the list of already replied enquiries for the managed projects.
     */
    public void viewAndReplyToManagedEnquiries() {
        System.out.println("\n--- View/Reply Enquiries (Managed Projects) ---");
        List<String> myManagedProjectNames = projectService.getProjectsManagedBy(currentUser.getNric())
                .stream()
                .filter(p -> filterLocation == null || p.getNeighborhood().equalsIgnoreCase(filterLocation))
                .filter(p -> filterFlatType == null || p.getFlatTypes().containsKey(filterFlatType))
                .map((Project p) -> p.getProjectName())
                .collect(Collectors.toList());

        if (myManagedProjectNames.isEmpty()) {
            String filterMsg = (filterLocation != null || filterFlatType != null) ? " matching the current filters."
                    : ".";
            System.out.println("You are not managing any projects" + filterMsg + " For which to view enquiries.");
            return;
        }

        List<Enquiry> managedEnquiries = enquiryService.getAllEnquiries().stream()
                .filter(e -> myManagedProjectNames.contains(e.getProjectName()))
                .sorted(Comparator.comparing((Enquiry e) -> e.getProjectName())
                        .thenComparing((Enquiry e) -> e.getEnquiryDate(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (managedEnquiries.isEmpty()) {
            String filterMsg = (filterLocation != null || filterFlatType != null) ? " (matching filters)." : ".";
            System.out.println("No enquiries found for the projects you manage" + filterMsg);
            return;
        }

        List<Enquiry> unrepliedEnquiries = managedEnquiries.stream()
                .filter(e -> !e.isReplied())
                .collect(Collectors.toList());
        List<Enquiry> repliedEnquiries = managedEnquiries.stream()
                .filter((Enquiry e) -> e.isReplied())
                .collect(Collectors.toList());

        System.out.println("--- Unreplied Enquiries (Managed Projects"
                + ((filterLocation != null || filterFlatType != null) ? " - Filtered" : "") + ") ---");
        if (unrepliedEnquiries.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (int i = 0; i < unrepliedEnquiries.size(); i++) {
                Enquiry e = unrepliedEnquiries.get(i);
                System.out.printf("%d. ", i + 1);
                printEnquiryDetails(e);
                System.out.println("---");
            }
            int choice = getIntInput("Enter the number of the enquiry to reply to (or 0 to skip): ", 0,
                    unrepliedEnquiries.size());

            if (choice >= 1) {
                Enquiry enquiryToReply = unrepliedEnquiries.get(choice - 1);
                System.out.print("Enter your reply: ");
                String replyText = scanner.nextLine().trim();
                if (enquiryToReply.setReply(replyText, currentUser.getNric(), DateUtils.getCurrentDate())) {
                    System.out.println("Reply submitted successfully.");
                    enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
                } else {
                    System.out.println("Reply not submitted.");
                }
            } else if (choice != 0) {
                System.out.println("Invalid choice.");
            }
        }

        System.out.println("\n--- Replied Enquiries (Managed Projects"
                + ((filterLocation != null || filterFlatType != null) ? " - Filtered" : "") + ") ---");
        if (repliedEnquiries.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (Enquiry e : repliedEnquiries) {
                printEnquiryDetails(e);
                System.out.println("----------------------------------------");
            }
        }
    }

    /**
     * Prints the details of a given enquiry.
     * Includes the enquiry ID, project name, applicant NRIC, applicant name,
     * enquiry date, and the enquiry text.
     * If the enquiry has been replied to, it also includes the reply details:
     * replier NRIC, replier role, reply date, and reply text.
     * 
     * @param e The Enquiry object to print details for.
     */

    private void printEnquiryDetails(Enquiry e) {
        User applicant = userService.findUserByNric(e.getApplicantNric());
        String applicantName = applicant != null ? applicant.getName() : "N/A";
        System.out.printf("ID: %s | Project: %-15s | Applicant: %s (%s) | Date: %s\n",
                e.getEnquiryId(),
                e.getProjectName(),
                e.getApplicantNric(),
                applicantName,
                DateUtils.formatDate(e.getEnquiryDate()));
        System.out.println("   Enquiry: " + e.getEnquiryText());
        if (e.isReplied()) {
            User replier = userService.findUserByNric(e.getRepliedByNric());
            String replierRole = replier != null ? " (" + replier.getRole().toString().replace("HDB_", "") + ")" : "";

            System.out.printf("   Reply (by %s%s on %s): %s\n",
                    e.getRepliedByNric() != null ? e.getRepliedByNric() : "N/A",
                    replierRole,
                    e.getReplyDate() != null ? DateUtils.formatDate(e.getReplyDate()) : "N/A",
                    e.getReplyText());
        } else {
            System.out.println("   Reply: (Pending)");
        }
    }
}
