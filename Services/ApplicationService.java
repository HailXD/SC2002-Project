/**
 * Service implementation for managing BTO Application data.
 * Handles loading application data from applications.csv, adjusting project unit availability based on
 * booked applications, and saving applications back. Provides methods for finding, retrieving,
 * adding, and removing applications.
 *
 * @author Jordon
 */

package Services;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Enums.ApplicationStatus;
import Enums.FlatType;
import Models.BTOApplication;
import Models.FlatTypeDetails;
import Models.Project;
import Parsers.Dparse;
import Utils.DateUtils;

public class ApplicationService implements IApplicationService {
    private static final String DATA_DIR = "data";
    private static final String APPLICATION_FILE = DATA_DIR + File.separator + "applications.csv";
    private static final String[] APPLICATION_HEADER = { "ApplicationID", "ApplicantNRIC", "ProjectName",
            "FlatTypeApplied", "Status", "ApplicationDate" };
    private static final String DELIMITER = ",";

    private Map<String, BTOApplication> applications;

    /**
     * Constructs a new ApplicationService. Initializes the internal application
     * map.
     */
    public ApplicationService() {
        this.applications = new HashMap<>();
    }

    /**
     * Loads BTO application data from applications.csv.
     * Validates application IDs (uniqueness), dates, and enum values.
     * Tracks applications with 'BOOKED' status and adjusts the `availableUnits` in
     * the corresponding
     * Project's FlatTypeDetails objects provided in the `projects` list.
     * Populates the internal application map.
     *
     * @param projects A list of all Project objects, used for finding projects and
     *                 adjusting their unit counts.
     * @return A copy of the map containing all loaded applications (ApplicationID
     *         to BTOApplication object).
     */
    @Override
    public Map<String, BTOApplication> loadApplications(List<Project> projects) {
        this.applications.clear();
        Map<Project, Map<FlatType, Integer>> bookedCounts = new HashMap<>();

        CsvRW.readCsv(APPLICATION_FILE, APPLICATION_HEADER.length).forEach(data -> {
            try {
                String appId = data[0].trim();
                if (appId.isEmpty() || this.applications.containsKey(appId)) {
                    if (!appId.isEmpty())
                        System.err.println("Skipping duplicate application ID: " + appId);
                    return;
                }
                String applicantNric = data[1].trim();
                String projectName = data[2].trim();
                FlatType flatType = FlatType.fromString(data[3].trim());
                ApplicationStatus status = ApplicationStatus.valueOf(data[4].trim().toUpperCase());
                Date appDate = Dparse.parseDate(data[5].trim());

                if (appDate == null) {
                    System.err.println("Skipping application with invalid date: " + appId);
                    return;
                }
                if (flatType == null && (status == ApplicationStatus.BOOKED || status == ApplicationStatus.SUCCESSFUL
                        || status == ApplicationStatus.PENDING_WITHDRAWAL)) {
                    System.err.println("Warning: Application " + appId + " is " + status
                            + " but has invalid/missing flat type '" + data[3] + "'. Status might be inconsistent.");
                }

                BTOApplication application = new BTOApplication(appId, applicantNric, projectName, flatType, status,
                        appDate);
                this.applications.put(application.getApplicationId(), application);

                if (status == ApplicationStatus.BOOKED && flatType != null) {
                    Project project = projects.stream()
                            .filter(p -> p.getProjectName().equalsIgnoreCase(projectName))
                            .findFirst().orElse(null);
                    if (project != null) {
                        bookedCounts.computeIfAbsent(project, _ -> new HashMap<>())
                                .merge(flatType, 1, (a, b) -> a + b);
                    } else {
                        System.err.println("Warning: Booked application " + appId + " refers to non-existent project '"
                                + projectName + "'. Unit count cannot be adjusted.");
                    }
                }

            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing enum/data in application data: " + String.join(DELIMITER, data)
                        + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error parsing application data line: " + String.join(DELIMITER, data) + " - "
                        + e.getMessage());
            }
        });

        bookedCounts.forEach((project, typeCounts) -> {
            typeCounts.forEach((type, count) -> {
                FlatTypeDetails details = project.getMutableFlatTypeDetails(type);
                if (details != null) {
                    int initialAvailable = details.getTotalUnits();
                    int finalAvailable = Math.max(0, initialAvailable - count);
                    details.setAvailableUnits(finalAvailable);
                    if (count > details.getTotalUnits()) {
                        System.err.println("Error: More flats booked (" + count + ") than total units ("
                                + details.getTotalUnits() + ") for " + project.getProjectName() + "/"
                                + type.getDisplayName() + ". Available units set to 0.");
                    }
                } else {
                    System.err.println("Warning: Trying to adjust units for non-existent flat type "
                            + type.getDisplayName() + " in project " + project.getProjectName());
                }
            });
        });

        System.out.println("Loaded " + this.applications.size() + " applications.");
        return new HashMap<>(this.applications);
    }

    /**
     * Saves the provided map of BTO applications to applications.csv.
     * Formats application data for CSV storage.
     * Overwrites the existing file. Updates the internal application map to match
     * the saved state.
     *
     * @param applicationsToSave The map of applications (ApplicationID to
     *                           BTOApplication object) to save.
     */
    @Override
    public void saveApplications(Map<String, BTOApplication> applicationsToSave) {
        List<String[]> dataList = new ArrayList<>();
        dataList.add(APPLICATION_HEADER);
        applicationsToSave.values().stream()
                .sorted(Comparator.comparing((BTOApplication app) -> app.getApplicationId()))
                .forEach(app -> {
                    dataList.add(new String[] {
                            app.getApplicationId(),
                            app.getApplicantNric(),
                            app.getProjectName(),
                            app.getFlatTypeApplied() == null ? "" : app.getFlatTypeApplied().name(),
                            app.getStatus().name(),
                            DateUtils.formatDate(app.getApplicationDate())
                    });
                });
        CsvRW.writeCsv(APPLICATION_FILE, dataList);
        System.out.println("Saved applications.");
        this.applications = new HashMap<>(applicationsToSave);
    }

    /**
     * Finds a BTO application by its unique ID from the internally managed map.
     *
     * @param applicationId The ID of the application to find.
     * @return The BTOApplication object if found, or null otherwise.
     */
    @Override
    public BTOApplication findApplicationById(String applicationId) {
        return this.applications.get(applicationId);
    }

    /**
     * Finds a specific BTO application based on the applicant's NRIC and the
     * project name.
     * Constructs the expected application ID (NRIC_ProjectName) and uses
     * `findApplicationById`.
     *
     * @param nric        The NRIC of the applicant.
     * @param projectName The name of the project.
     * @return The BTOApplication object if found, or null if NRIC/projectName is
     *         null or the application doesn't exist.
     */
    @Override
    public BTOApplication findApplicationByApplicantAndProject(String nric, String projectName) {
        if (nric == null || projectName == null)
            return null;
        String appId = nric + "_" + projectName;
        return findApplicationById(appId);
    }

    /**
     * Retrieves a list of all BTO applications submitted for a specific project.
     * Filters the internal application map based on the projectName field.
     *
     * @param projectName The name of the project.
     * @return A list of BTOApplication objects for the specified project. Returns
     *         an empty list if projectName is null or no applications are found.
     */
    @Override
    public List<BTOApplication> getApplicationsByProject(String projectName) {
        if (projectName == null)
            return new ArrayList<>();
        return this.applications.values().stream()
                .filter(app -> projectName.equals(app.getProjectName()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all BTO applications that currently have a specific
     * status.
     * Filters the internal application map based on the status field.
     *
     * @param status The ApplicationStatus to filter by.
     * @return A list of BTOApplication objects with the specified status. Returns
     *         an empty list if status is null or no applications match.
     */
    @Override
    public List<BTOApplication> getApplicationsByStatus(ApplicationStatus status) {
        if (status == null)
            return new ArrayList<>();
        return this.applications.values().stream()
                .filter(app -> status.equals(app.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all BTO applications submitted by a specific applicant.
     * Filters the internal application map based on the applicantNric field.
     *
     * @param nric The NRIC of the applicant.
     * @return A list of BTOApplication objects submitted by the specified
     *         applicant. Returns an empty list if nric is null or no applications
     *         are found.
     */
    @Override
    public List<BTOApplication> getApplicationsByApplicant(String nric) {
        if (nric == null)
            return new ArrayList<>();
        return this.applications.values().stream()
                .filter(app -> nric.equals(app.getApplicantNric()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a new BTO application to the internal map if an application with the
     * same ID doesn't already exist.
     * Prints an error message if a duplicate ID is detected.
     *
     * @param application The BTOApplication object to add.
     */
    @Override
    public void addApplication(BTOApplication application) {
        if (application != null && !this.applications.containsKey(application.getApplicationId())) {
            this.applications.put(application.getApplicationId(), application);
        } else if (application != null) {
            System.err.println("Application with ID " + application.getApplicationId() + " already exists.");
        }
    }

    /**
     * Removes a BTO application from the internal map based on its ID.
     *
     * @param applicationId The ID of the application to remove. If null, the method
     *                      does nothing.
     * @return true if the application was found and removed, false otherwise.
     */
    @Override
    public boolean removeApplication(String applicationId) {
        if (applicationId != null) {
            return this.applications.remove(applicationId) != null;
        }
        return false;
    }

    /**
     * Retrieves a copy of the map containing all BTO applications currently managed
     * by the service.
     * Returning a copy prevents external modification of the internal state.
     *
     * @return A new HashMap containing all applications (ApplicationID to
     *         BTOApplication object).
     */
    @Override
    public Map<String, BTOApplication> getAllApplications() {
        return new HashMap<>(this.applications);
    }
}
