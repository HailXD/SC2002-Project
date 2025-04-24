/**
 * Main controller for the Applicant role.
 * It doesn't implement actions directly but delegates them to specialized sub-controllers:
 * - {@link ApplicantActionController} for core application actions.
 * - {@link EnquiryApplicantController} for enquiry-related actions.
 * Inherits common functionality and state (like filters) from {@link BaseController}.
 *
 * @author Jordon
 */
package Controllers;

import java.util.Scanner;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;

public class ApplicantController extends BaseController {

    private final ApplicantActionController actionController;
    private final EnquiryApplicantController enquiryController;

    /**
     * Constructs a new ApplicantController.
     * Instantiates and holds references to the specialized action and enquiry
     * controllers.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access (needed by
     *                                   EnquiryApplicantController).
     * @param currentUser                The currently logged-in User (expected to
     *                                   be Applicant).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public ApplicantController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);

        this.actionController = new ApplicantActionController(userService, projectService, applicationService,
                officerRegistrationService, currentUser, scanner, authController);
        this.enquiryController = new EnquiryApplicantController(userService, projectService, applicationService,
                officerRegistrationService, enquiryService, currentUser, scanner, authController);

    }

    /**
     * Delegates the action to view open projects to the ApplicantActionController.
     * 
     * @see ApplicantActionController#viewOpenProjects()
     */
    public void viewOpenProjects() {
        actionController.viewOpenProjects();
    }

    /**
     * Delegates the action to apply for a project to the ApplicantActionController.
     * 
     * @see ApplicantActionController#applyForProject()
     */
    public void applyForProject() {
        actionController.applyForProject();
    }

    /**
     * Delegates the action to view the applicant's application status to the
     * ApplicantActionController.
     * 
     * @see ApplicantActionController#viewMyApplication()
     */
    public void viewMyApplication() {
        actionController.viewMyApplication();
    }

    /**
     * Delegates the action to request application withdrawal to the
     * ApplicantActionController.
     * 
     * @see ApplicantActionController#requestWithdrawal()
     */
    public void requestWithdrawal() {
        actionController.requestWithdrawal();
    }

    /**
     * Delegates the action to submit an enquiry to the EnquiryApplicantController.
     * 
     * @see EnquiryApplicantController#submitEnquiry()
     */
    public void submitEnquiry() {
        enquiryController.submitEnquiry();
    }

    /**
     * Delegates the action to view the applicant's enquiries to the
     * EnquiryApplicantController.
     * 
     * @see EnquiryApplicantController#viewMyEnquiries()
     */
    public void viewMyEnquiries() {
        enquiryController.viewMyEnquiries();
    }

    /**
     * Delegates the action to edit an enquiry to the EnquiryApplicantController.
     * 
     * @see EnquiryApplicantController#editMyEnquiry()
     */
    public void editMyEnquiry() {
        enquiryController.editMyEnquiry();
    }

    /**
     * Delegates the action to delete an enquiry to the EnquiryApplicantController.
     * 
     * @see EnquiryApplicantController#deleteMyEnquiry()
     */
    public void deleteMyEnquiry() {
        enquiryController.deleteMyEnquiry();
    }

}
