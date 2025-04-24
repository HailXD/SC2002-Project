/**
 * Main controller for the HDB Officer role.
 * It inherits from {@link ApplicantController} to provide standard applicant functionalities
 * (like applying for projects, managing own applications/enquiries).
 * It also delegates officer-specific actions to specialized sub-controllers:
 * - {@link OfficerActionController} for registration and status viewing.
 * - {@link EnquiryOfficerController} for handling enquiries related to the assigned project.
 * - {@link BookingOfficerController} for managing the flat booking process.
 *
 * @author Patrick
 */
package Controllers;

import java.util.Scanner;
import Models.User;
import Services.IApplicationService;
import Services.IEnquiryService;
import Services.IOfficerRegistrationService;
import Services.IProjectService;
import Services.IUserService;

public class OfficerController extends ApplicantController {

    private final OfficerActionController officerActionController;
    private final EnquiryOfficerController enquiryOfficerController;
    private final BookingOfficerController bookingOfficerController;

    /**
     * Constructs a new OfficerController.
     * Calls the superclass (ApplicantController) constructor to initialize base and
     * applicant-specific components.
     * Instantiates and holds references to the specialized officer action, enquiry,
     * and booking controllers.
     *
     * @param userService                Service for user data access.
     * @param projectService             Service for project data access.
     * @param applicationService         Service for application data access.
     * @param officerRegistrationService Service for officer registration data
     *                                   access.
     * @param enquiryService             Service for enquiry data access (needed by
     *                                   both Applicant and Officer parts).
     * @param currentUser                The currently logged-in User (expected to
     *                                   be HDBOfficer).
     * @param scanner                    Scanner instance for reading user input.
     * @param authController             Controller for authentication tasks.
     */
    public OfficerController(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService,
            IEnquiryService enquiryService,
            User currentUser, Scanner scanner, AuthController authController) {
        super(userService, projectService, applicationService, officerRegistrationService, enquiryService, currentUser,
                scanner, authController);

        this.officerActionController = new OfficerActionController(userService, projectService, applicationService,
                officerRegistrationService, currentUser, scanner, authController);
        this.enquiryOfficerController = new EnquiryOfficerController(userService, projectService, applicationService,
                officerRegistrationService, enquiryService, currentUser, scanner, authController);
        this.bookingOfficerController = new BookingOfficerController(userService, projectService, applicationService,
                officerRegistrationService, currentUser, scanner, authController);
    }

    /**
     * Delegates the action to register for a project to the
     * OfficerActionController.
     * 
     * @see OfficerActionController#registerForProject()
     */
    public void registerForProject() {
        officerActionController.registerForProject();
    }

    /**
     * Delegates the action to view registration status to the
     * OfficerActionController.
     * 
     * @see OfficerActionController#viewRegistrationStatus()
     */
    public void viewRegistrationStatus() {
        officerActionController.viewRegistrationStatus();
    }

    /**
     * Delegates the action to view details of the handling project to the
     * OfficerActionController.
     * 
     * @see OfficerActionController#viewHandlingProjectDetails()
     */
    public void viewHandlingProjectDetails() {
        officerActionController.viewHandlingProjectDetails();
    }

    /**
     * Delegates the action to view and reply to enquiries for the handling project
     * to the EnquiryOfficerController.
     * Note: This is the Officer-specific version, distinct from the applicant's
     * "viewMyEnquiries".
     * 
     * @see EnquiryOfficerController#viewAndReplyToEnquiries()
     */
    public void viewAndReplyToEnquiries() {
        enquiryOfficerController.viewAndReplyToEnquiries();
    }

    /**
     * Delegates the action to manage flat bookings to the BookingOfficerController.
     * 
     * @see BookingOfficerController#manageFlatBooking()
     */
    public void manageFlatBooking() {
        bookingOfficerController.manageFlatBooking();
    }

}
