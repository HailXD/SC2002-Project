/**
 * Provides static utility methods for coordinating data loading, synchronization, and saving across different services.
 * Handles initial data synchronization logic after loading and provides convenience methods for saving all data or just user data.
 *
 * @author Jordon
 */
package Services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Controllers.AuthController;

import java.util.Date;
import java.util.Comparator;
import java.io.File;

import Enums.OfficerRegistrationStatus;
import Enums.ApplicationStatus;

import Models.Project;
import Models.BTOApplication;
import Models.OfficerRegistration;
import Models.User;
import Models.Applicant;
import Models.HDBOfficer;

public class DataService {
    private static final String DATA_DIR = "data";
    private static final String APPLICANT_LIST_FILE = DATA_DIR + File.separator + "ApplicantList.csv";
    private static final String OFFICER_LIST_FILE = DATA_DIR + File.separator + "OfficerList.csv";
    private static final String MANAGER_LIST_FILE = DATA_DIR + File.separator + "ManagerList.csv";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String[] APPLICANT_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };
    private static final String[] OFFICER_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };
    private static final String[] MANAGER_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };

    /**
     * Performs data synchronization checks and updates after initial loading.
     * - Synchronizes Applicant profile status (appliedProjectName,
     * applicationStatus, bookedFlatType) based on the latest relevant
     * BTOApplication record.
     * - Synchronizes Project approved officer lists with OfficerRegistration
     * records:
     * - Creates/updates APPROVED registrations if an officer is listed in a
     * project's CSV but lacks a corresponding registration.
     * - Issues warnings if an APPROVED registration exists but the officer is
     * missing from the project's CSV list.
     * - Note: Adjustment of project available units based on booked applications is
     * now handled within ApplicationService.loadApplications.
     * Saves officer registrations if any changes were made during synchronization.
     *
     * @param userService                The user service instance containing loaded
     *                                   user data.
     * @param projectService             The project service instance containing
     *                                   loaded project data.
     * @param applicationService         The application service instance containing
     *                                   loaded application data.
     * @param officerRegistrationService The officer registration service instance
     *                                   containing loaded registration data.
     */
    public static void synchronizeData(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IOfficerRegistrationService officerRegistrationService) {
        System.out.println("Synchronizing loaded data...");

        Map<String, User> users = userService.getAllUsers();
        List<Project> projects = projectService.getAllProjects();
        Map<String, BTOApplication> applications = applicationService.getAllApplications();
        Map<String, OfficerRegistration> officerRegistrations = officerRegistrationService.getAllRegistrations();

        boolean registrationsModified = false;

        users.values().stream()
                .filter(u -> u instanceof Applicant)
                .map(u -> (Applicant) u)
                .forEach(applicant -> {
                    BTOApplication relevantApp = applications.values().stream()
                            .filter(app -> app.getApplicantNric().equals(applicant.getNric()))
                            .max(Comparator.comparing((BTOApplication app) -> app.getStatus(),
                                    Comparator.comparingInt(s -> {
                                        switch (s) {
                                            case BOOKED:
                                                return 6;
                                            case SUCCESSFUL:
                                                return 5;
                                            case PENDING_WITHDRAWAL:
                                                return 4;
                                            case PENDING:
                                                return 3;
                                            case WITHDRAWN:
                                                return 2;
                                            case UNSUCCESSFUL:
                                                return 1;
                                            default:
                                                return 0;
                                        }
                                    })).thenComparing((BTOApplication app) -> app.getApplicationDate(),
                                            Comparator.reverseOrder()))
                            .orElse(null);

                    if (relevantApp != null) {
                        applicant.setAppliedProjectName(relevantApp.getProjectName());
                        applicant.setApplicationStatus(relevantApp.getStatus());
                        if (relevantApp.getStatus() == ApplicationStatus.BOOKED) {
                            applicant.setBookedFlatType(relevantApp.getFlatTypeApplied());
                        } else {
                            applicant.setBookedFlatType(null);
                        }
                    } else {
                        applicant.clearApplicationState();
                    }
                });

        for (Project project : projects) {
            List<String> approvedNricsFromProject = new ArrayList<>(project.getApprovedOfficerNrics());

            for (String officerNric : approvedNricsFromProject) {
                User user = users.get(officerNric);
                if (!(user instanceof HDBOfficer)) {
                    System.err.println("Data Sync Warning: NRIC " + officerNric + " in project '"
                            + project.getProjectName()
                            + "' approved list is not a valid HDB Officer. Consider removing from project CSV.");
                    continue;
                }

                String expectedRegId = officerNric + "_REG_" + project.getProjectName();
                OfficerRegistration existingReg = officerRegistrations.get(expectedRegId);

                if (existingReg == null || existingReg.getStatus() != OfficerRegistrationStatus.APPROVED) {
                    System.out.println("Info: Auto-creating/updating APPROVED registration for Officer " + officerNric
                            + " for Project '" + project.getProjectName() + "' based on project list.");

                    Date placeholderDate = project.getApplicationOpeningDate() != null
                            ? project.getApplicationOpeningDate()
                            : new Date(0);

                    OfficerRegistration syncReg = new OfficerRegistration(expectedRegId, officerNric,
                            project.getProjectName(), OfficerRegistrationStatus.APPROVED, placeholderDate);
                    officerRegistrations.put(syncReg.getRegistrationId(), syncReg);
                    registrationsModified = true;
                }
            }

            officerRegistrations.values().stream()
                    .filter(reg -> reg.getProjectName().equals(project.getProjectName())
                            && reg.getStatus() == OfficerRegistrationStatus.APPROVED)
                    .forEach(reg -> {
                        if (!project.getApprovedOfficerNrics().contains(reg.getOfficerNric())) {
                            System.err.println("Data Sync Warning: Approved registration " + reg.getRegistrationId()
                                    + " exists, but officer " + reg.getOfficerNric() + " is NOT in project '"
                                    + project.getProjectName()
                                    + "' approved list. Project CSV might be outdated or registration status incorrect.");
                        }
                    });
        }

        if (registrationsModified) {
            System.out.println("Saving updated officer registrations due to synchronization...");
            officerRegistrationService.saveOfficerRegistrations(officerRegistrations);
        }

        System.out.println("Data synchronization complete.");
    }

    /**
     * Saves all data managed by the provided services to their respective
     * persistent storage locations (CSV files).
     * This is a convenience method typically called during application shutdown.
     *
     * @param userService                The user service instance.
     * @param projectService             The project service instance.
     * @param applicationService         The application service instance.
     * @param enquiryService             The enquiry service instance.
     * @param officerRegistrationService The officer registration service instance.
     */
    public static void saveAllData(IUserService userService, IProjectService projectService,
            IApplicationService applicationService, IEnquiryService enquiryService,
            IOfficerRegistrationService officerRegistrationService) {
        System.out.println("Saving all data...");
        userService.saveUsers(userService.getAllUsers());
        projectService.saveProjects(projectService.getAllProjects());
        applicationService.saveApplications(applicationService.getAllApplications());
        enquiryService.saveEnquiries(enquiryService.getAllEnquiries());
        officerRegistrationService.saveOfficerRegistrations(officerRegistrationService.getAllRegistrations());
        System.out.println("All data saved.");
    }

    /**
     * Static method to save user data to respective CSV files (ApplicantList,
     * OfficerList, ManagerList).
     * This method duplicates the logic in {@link UserService#saveUsers(Map)} and is
     * kept primarily for compatibility
     * with {@link AuthController} which uses it directly. Ideally, AuthController
     * should use IUserService.
     *
     * @param users The map of users (NRIC to User object) to save.
     */
    public static void saveUsers(Map<String, User> users) {
        List<String[]> applicantData = new ArrayList<>();
        List<String[]> officerData = new ArrayList<>();
        List<String[]> managerData = new ArrayList<>();

        applicantData.add(APPLICANT_HEADER);
        officerData.add(OFFICER_HEADER);
        managerData.add(MANAGER_HEADER);

        users.values().forEach(user -> {
            String[] userData = {
                    user.getName(),
                    user.getNric(),
                    String.valueOf(user.getAge()),
                    user.getMaritalStatus().name(),
                    user.getPassword()
            };
            switch (user.getRole()) {
                case HDB_MANAGER:
                    managerData.add(userData);
                    break;
                case HDB_OFFICER:
                    officerData.add(userData);
                    break;
                case APPLICANT:
                    applicantData.add(userData);
                    break;
            }
        });

        CsvRW.writeCsv(APPLICANT_LIST_FILE, applicantData);
        CsvRW.writeCsv(OFFICER_LIST_FILE, officerData);
        CsvRW.writeCsv(MANAGER_LIST_FILE, managerData);
        System.out.println("Saved users (via static DataService method).");
    }
}
