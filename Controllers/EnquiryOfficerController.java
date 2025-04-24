/**
 * Controller handling enquiry-related actions performed by an HDB Officer, specifically
 * viewing and replying to enquiries submitted for the project they are currently handling.
 * Requires an IEnquiryService. Inherits common functionality from BaseController.
 *
 * @author Jun Yang
 */
package Controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Models.Enquiry;
import Models.HDBOfficer;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class EnquiryOfficerController extends BaseController {

    private final IEnquiryService enquiryService;

    /**
     * Constructs a new EnquiryOfficerController.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access.
     * @param currentUser                The currently logged-in User (expected to
     *                                   be HDBOfficer).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public EnquiryOfficerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        this.enquiryService = enquiryService;
    }

    /**
     * Allows the HDB Officer to view and reply to enquiries for the project they
     * are currently handling.
     * - Checks if the officer is handling an active project.
     * - Retrieves all enquiries for that specific project using the enquiry
     * service.
     * - Separates enquiries into unreplied and replied lists.
     * - Displays unreplied enquiries and prompts the officer to select one to reply
     * to.
     * - If an enquiry is selected, prompts for the reply text.
     * - Sets the reply details on the Enquiry object using its `setReply` method.
     * - Saves the updated enquiry list via the enquiry service.
     * - Displays the list of already replied enquiries for the project.
     */
    public void viewAndReplyToEnquiries() {
        if (!(currentUser instanceof HDBOfficer))
            return;
        HDBOfficer officer = (HDBOfficer) currentUser;

        Project handlingProject = getOfficerHandlingProject(officer);

        if (handlingProject == null) {
            System.out.println("You need to be handling an active project to view and reply to its enquiries.");
            return;
        }
        String handlingProjectName = handlingProject.getProjectName();

        System.out.println("\n--- Enquiries for Project: " + handlingProjectName + " ---");
        List<Enquiry> projectEnquiries = enquiryService.getEnquiriesByProject(handlingProjectName)
                .stream()
                .sorted(Comparator.comparing((Enquiry e) -> e.getEnquiryDate()).reversed())
                .collect(Collectors.toList());

        if (projectEnquiries.isEmpty()) {
            System.out.println("No enquiries found for this project.");
            return;
        }

        List<Enquiry> unrepliedEnquiries = projectEnquiries.stream()
                .filter(e -> !e.isReplied())
                .collect(Collectors.toList());
        List<Enquiry> repliedEnquiries = projectEnquiries.stream()
                .filter((Enquiry e) -> e.isReplied())
                .collect(Collectors.toList());

        System.out.println("--- Unreplied Enquiries ---");
        if (unrepliedEnquiries.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (int i = 0; i < unrepliedEnquiries.size(); i++) {
                Enquiry e = unrepliedEnquiries.get(i);
                System.out.printf("%d. ID: %s | Applicant: %s | Date: %s\n",
                        i + 1, e.getEnquiryId(), e.getApplicantNric(), DateUtils.formatDate(e.getEnquiryDate()));
                System.out.println("   Enquiry: " + e.getEnquiryText());
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

        System.out.println("\n--- Replied Enquiries ---");
        if (repliedEnquiries.isEmpty()) {
            System.out.println("(None)");
        } else {
            for (Enquiry e : repliedEnquiries) {
                System.out.printf("ID: %s | Applicant: %s | Enquiry Date: %s\n",
                        e.getEnquiryId(), e.getApplicantNric(), DateUtils.formatDate(e.getEnquiryDate()));
                System.out.println("   Enquiry: " + e.getEnquiryText());
                System.out.printf("   Reply (by %s on %s): %s\n",
                        e.getRepliedByNric() != null ? e.getRepliedByNric() : "N/A",
                        e.getReplyDate() != null ? DateUtils.formatDate(e.getReplyDate()) : "N/A",
                        e.getReplyText());
                System.out.println("--------------------");
            }
        }
    }
}
