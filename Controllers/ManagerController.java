/**
 * Main controller for the HDB Manager role.
 * It doesn't implement actions directly but delegates them to specialized sub-controllers
 * responsible for specific areas of management (Projects, Officer Registrations, Applications,
 * Withdrawals, Reports, Enquiries).
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

public class ManagerController extends BaseController {

    private final ProjectManagerController projectManagerController;
    private final OfficerRegistrationManagerController officerRegManagerController;
    private final ApplicationManagerController applicationManagerController;
    private final WithdrawalManagerController withdrawalManagerController;
    private final ReportManagerController reportManagerController;
    private final EnquiryManagerController enquiryManagerController;

    /**
     * Constructs a new ManagerController.
     * Instantiates and holds references to all the specialized manager
     * sub-controllers,
     * passing necessary dependencies (services, user, scanner, auth controller) to
     * them.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access (needed by
     *                                   multiple sub-controllers).
     * @param currentUser                The currently logged-in User (expected to
     *                                   be HDBManager).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public ManagerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, currentUser, scanner,
                authController);

        this.projectManagerController = new ProjectManagerController(userService, projectService, applicationService,
                officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.officerRegManagerController = new OfficerRegistrationManagerController(userService, projectService,
                applicationService, officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.applicationManagerController = new ApplicationManagerController(userService, projectService,
                applicationService, officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.withdrawalManagerController = new WithdrawalManagerController(userService, projectService,
                applicationService, officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.reportManagerController = new ReportManagerController(userService, projectService, applicationService,
                officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.enquiryManagerController = new EnquiryManagerController(userService, projectService, applicationService,
                officerRegistrationService, enquiryService, currentUser, scanner, authController);

    }

    /**
     * Delegates the action to create a project to the ProjectManagerController.
     * 
     * @see ProjectManagerController#createProject()
     */
    public void createProject() {
        projectManagerController.createProject();
    }

    /**
     * Delegates the action to edit a project to the ProjectManagerController.
     * 
     * @see ProjectManagerController#editProject()
     */
    public void editProject() {
        projectManagerController.editProject();
    }

    /**
     * Delegates the action to delete a project to the ProjectManagerController.
     * 
     * @see ProjectManagerController#deleteProject()
     */
    public void deleteProject() {
        projectManagerController.deleteProject();
    }

    /**
     * Delegates the action to toggle project visibility to the
     * ProjectManagerController.
     * 
     * @see ProjectManagerController#toggleProjectVisibility()
     */
    public void toggleProjectVisibility() {
        projectManagerController.toggleProjectVisibility();
    }

    /**
     * Delegates the action to view all projects (manager oversight) to the
     * ProjectManagerController.
     * 
     * @see ProjectManagerController#viewAllProjects()
     */
    public void viewAllProjects() {
        projectManagerController.viewAllProjects();
    }

    /**
     * Delegates the action to view managed projects to the
     * ProjectManagerController.
     * 
     * @see ProjectManagerController#viewMyProjects()
     */
    public void viewMyProjects() {
        projectManagerController.viewMyProjects();
    }

    /**
     * Delegates the action to manage officer registrations to the
     * OfficerRegistrationManagerController.
     * 
     * @see OfficerRegistrationManagerController#manageOfficerRegistrations()
     */
    public void manageOfficerRegistrations() {
        officerRegManagerController.manageOfficerRegistrations();
    }

    /**
     * Delegates the action to manage BTO applications to the
     * ApplicationManagerController.
     * 
     * @see ApplicationManagerController#manageApplications()
     */
    public void manageApplications() {
        applicationManagerController.manageApplications();
    }

    /**
     * Delegates the action to manage withdrawal requests to the
     * WithdrawalManagerController.
     * 
     * @see WithdrawalManagerController#manageWithdrawalRequests()
     */
    public void manageWithdrawalRequests() {
        withdrawalManagerController.manageWithdrawalRequests();
    }

    /**
     * Delegates the action to generate the applicant report to the
     * ReportManagerController.
     * 
     * @see ReportManagerController#generateApplicantReport()
     */
    public void generateApplicantReport() {
        reportManagerController.generateApplicantReport();
    }

    /**
     * Delegates the action to view all enquiries to the EnquiryManagerController.
     * 
     * @see EnquiryManagerController#viewAllEnquiries()
     */
    public void viewAllEnquiries() {
        enquiryManagerController.viewAllEnquiries();
    }

    /**
     * Delegates the action to view and reply to managed enquiries to the
     * EnquiryManagerController.
     * 
     * @see EnquiryManagerController#viewAndReplyToManagedEnquiries()
     */
    public void viewAndReplyToManagedEnquiries() {
        enquiryManagerController.viewAndReplyToManagedEnquiries();
    }

}
