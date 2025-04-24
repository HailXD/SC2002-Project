/**
 * Controller handling enquiry-related actions initiated by an Applicant (or an Officer acting as an Applicant),
 * such as submitting, viewing, editing, and deleting their own enquiries.
 * Requires an IEnquiryService.
 *
 * @author Kai Wang
 */
package Controllers;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import Models.Enquiry;
import Models.Project;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;
import Utils.DateUtils;

public class EnquiryApplicantController extends BaseController {

    private final IEnquiryService enquiryService;

    /**
     * Constructs a new EnquiryApplicantController.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access.
     * @param currentUser                The currently logged-in User (expected to
     *                                   be Applicant or HDBOfficer).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public EnquiryApplicantController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);
        this.enquiryService = enquiryService;
    }

    /**
     * Allows the applicant to submit a new enquiry about a BTO project.
     * - Optionally displays a list of viewable projects for selection.
     * - Allows the applicant to type the project name directly if not selected or
     * if the project isn't listed.
     * - Prompts for the enquiry text.
     * - Creates a new Enquiry object.
     * - Adds the enquiry using the enquiry service and saves it.
     */
    public void submitEnquiry() {
        System.out.println("\n--- Submit Enquiry ---");
        List<Project> viewableProjects = getFilteredProjects(true, false, false, false, false);
        Project selectedProject = null;
        String projectNameInput;

        if (!viewableProjects.isEmpty()) {
            viewAndSelectProject(viewableProjects, "Select Project to Enquire About (Optional)");
            selectedProject = selectProjectFromList(viewableProjects);
        }

        if (selectedProject != null) {
            projectNameInput = selectedProject.getProjectName();
            System.out.println("Enquiring about project: " + projectNameInput);
        } else {
            if (!viewableProjects.isEmpty()) {
                System.out.println("No project selected from list, or you chose to cancel selection.");
            }
            System.out.print("Enter the exact Project Name you want to enquire about (leave blank to cancel): ");
            projectNameInput = scanner.nextLine().trim();

            if (projectNameInput.isEmpty()) {
                System.out.println("Enquiry cancelled.");
                return;
            }
            if (projectService.findProjectByName(projectNameInput) == null) {
                System.out.println("Warning: Project '" + projectNameInput
                        + "' not found in current listings. Ensure the name is correct before submitting.");
            }
        }

        System.out.print("Enter your enquiry text (cannot be empty): ");
        String text = scanner.nextLine().trim();

        if (!text.isEmpty()) {
            Enquiry newEnquiry = new Enquiry(currentUser.getNric(), projectNameInput, text, DateUtils.getCurrentDate());
            enquiryService.addEnquiry(newEnquiry);
            System.out.println("Enquiry submitted successfully (ID: " + newEnquiry.getEnquiryId() + ").");
            enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
        } else {
            System.out.println("Enquiry text cannot be empty. Enquiry not submitted.");
        }
    }

    /**
     * Displays all enquiries submitted by the currently logged-in applicant.
     * Retrieves enquiries using the enquiry service, sorts them by date (most
     * recent first).
     * Displays enquiry ID, project name, date, enquiry text, and reply details (if
     * replied).
     */
    public void viewMyEnquiries() {
        System.out.println("\n--- Your Enquiries ---");
        List<Enquiry> myEnquiries = enquiryService.getEnquiriesByApplicant(currentUser.getNric())
                .stream()
                .sorted(Comparator.comparing((Enquiry e) -> e.getEnquiryDate()).reversed())
                .collect(Collectors.toList());

        if (myEnquiries.isEmpty()) {
            System.out.println("You have not submitted any enquiries.");
            return;
        }

        for (int i = 0; i < myEnquiries.size(); i++) {
            Enquiry e = myEnquiries.get(i);
            System.out.printf("%d. ID: %s | Project: %s | Date: %s\n",
                    i + 1, e.getEnquiryId(), e.getProjectName(), DateUtils.formatDate(e.getEnquiryDate()));
            System.out.println("   Enquiry: " + e.getEnquiryText());
            if (e.isReplied()) {
                System.out.printf("   Reply (by %s on %s): %s\n",
                        e.getRepliedByNric() != null ? e.getRepliedByNric() : "N/A",
                        e.getReplyDate() != null ? DateUtils.formatDate(e.getReplyDate()) : "N/A",
                        e.getReplyText());
            } else {
                System.out.println("   Reply: (Pending)");
            }
            System.out.println("----------------------------------------");
        }
    }

    /**
     * Allows the applicant to edit the text of their own submitted enquiries,
     * provided they have not yet been replied to.
     * - Retrieves and displays editable enquiries (own, not replied).
     * - Prompts the user to select an enquiry to edit.
     * - Prompts for the new enquiry text.
     * - Updates the enquiry text using the Enquiry object's setter method.
     * - Saves the changes via the enquiry service.
     */
    public void editMyEnquiry() {
        System.out.println("\n--- Edit Enquiry ---");
        List<Enquiry> editableEnquiries = enquiryService.getEnquiriesByApplicant(currentUser.getNric())
                .stream()
                .filter(e -> !e.isReplied())
                .sorted(Comparator.comparing((Enquiry e) -> e.getEnquiryDate()).reversed())
                .collect(Collectors.toList());

        if (editableEnquiries.isEmpty()) {
            System.out.println("You have no enquiries that can be edited (must not be replied to yet).");
            return;
        }

        System.out.println("Select enquiry to edit:");
        for (int i = 0; i < editableEnquiries.size(); i++) {
            Enquiry e = editableEnquiries.get(i);
            String truncatedText = e.getEnquiryText().length() > 50 ? e.getEnquiryText().substring(0, 47) + "..."
                    : e.getEnquiryText();
            System.out.printf("%d. ID: %s | Project: %s | Text: %s\n",
                    i + 1, e.getEnquiryId(), e.getProjectName(), truncatedText);
        }

        int choice = getIntInput("Enter choice (or 0 to cancel): ", 0, editableEnquiries.size());

        if (choice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }

        Enquiry enquiryToEdit = editableEnquiries.get(choice - 1);
        System.out.println("Current text: " + enquiryToEdit.getEnquiryText());
        System.out.print("Enter new enquiry text (cannot be empty): ");
        String newText = scanner.nextLine().trim();

        if (enquiryToEdit.setEnquiryText(newText)) {
            System.out.println("Enquiry updated successfully.");
            enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
        } else {
            System.out.println("Enquiry not updated.");
        }
    }

    /**
     * Allows the applicant to delete their own submitted enquiries, provided they
     * have not yet been replied to.
     * - Retrieves and displays deletable enquiries (own, not replied).
     * - Prompts the user to select an enquiry to delete.
     * - Asks for confirmation.
     * - If confirmed, removes the enquiry using the enquiry service and saves the
     * changes.
     */
    public void deleteMyEnquiry() {
        System.out.println("\n--- Delete Enquiry ---");
        List<Enquiry> deletableEnquiries = enquiryService.getEnquiriesByApplicant(currentUser.getNric())
                .stream()
                .filter(e -> !e.isReplied())
                .sorted(Comparator.comparing((Enquiry e) -> e.getEnquiryDate()).reversed())
                .collect(Collectors.toList());

        if (deletableEnquiries.isEmpty()) {
            System.out.println("You have no enquiries that can be deleted (must not be replied to yet).");
            return;
        }

        System.out.println("Select enquiry to delete:");
        for (int i = 0; i < deletableEnquiries.size(); i++) {
            Enquiry e = deletableEnquiries.get(i);
            String truncatedText = e.getEnquiryText().length() > 50 ? e.getEnquiryText().substring(0, 47) + "..."
                    : e.getEnquiryText();
            System.out.printf("%d. ID: %s | Project: %s | Text: %s\n",
                    i + 1, e.getEnquiryId(), e.getProjectName(), truncatedText);
        }

        int choice = getIntInput("Enter choice (or 0 to cancel): ", 0, deletableEnquiries.size());

        if (choice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }

        Enquiry enquiryToDelete = deletableEnquiries.get(choice - 1);
        System.out.print(
                "Are you sure you want to permanently delete enquiry " + enquiryToDelete.getEnquiryId()
                        + "? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            if (enquiryService.removeEnquiry(enquiryToDelete.getEnquiryId())) {
                System.out.println("Enquiry deleted successfully.");
                enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
            } else {
                System.err.println("Error: Failed to remove enquiry from service layer.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }
}
