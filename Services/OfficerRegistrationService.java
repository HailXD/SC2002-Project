/**
 * Service implementation for managing HDB Officer project registration data.
 * Handles loading registration data from officer_registrations.csv, validating against user and project data,
 * and saving registrations back. Provides methods for finding, retrieving, adding, and removing registrations.
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

import Enums.OfficerRegistrationStatus;
import Models.HDBOfficer;
import Models.OfficerRegistration;
import Models.Project;
import Models.User;
import Parsers.Dparse;
import Utils.DateUtils;

public class OfficerRegistrationService implements IOfficerRegistrationService {
    private static final String DATA_DIR = "data";
    private static final String DELIMITER = ",";
    private static final String OFFICER_REGISTRATION_FILE = DATA_DIR + File.separator + "officer_registrations.csv";
    private static final String[] OFFICER_REGISTRATION_HEADER = { "RegistrationID", "OfficerNRIC", "ProjectName",
            "Status", "RegistrationDate" };

    private Map<String, OfficerRegistration> registrations;

    /**
     * Constructs a new OfficerRegistrationService. Initializes the internal
     * registration map.
     */
    public OfficerRegistrationService() {
        this.registrations = new HashMap<>();
    }

    /**
     * Loads officer registration data from officer_registrations.csv.
     * Validates registration IDs (uniqueness), dates, enum values, officer NRICs
     * (must exist in `users` and be an HDBOfficer),
     * and project names (must exist in `projects`).
     * Populates the internal registration map.
     *
     * @param users    A map of NRIC to User objects, used for validating officer
     *                 NRICs.
     * @param projects A list of all Project objects, used for validating project
     *                 names.
     * @return A copy of the map containing all loaded and validated registrations
     *         (RegistrationID to OfficerRegistration object).
     */
    @Override
    public Map<String, OfficerRegistration> loadOfficerRegistrations(Map<String, User> users, List<Project> projects) {
        this.registrations.clear();

        CsvRW.readCsv(OFFICER_REGISTRATION_FILE, OFFICER_REGISTRATION_HEADER.length).forEach(data -> {
            try {
                String regId = data[0].trim();
                if (regId.isEmpty() || this.registrations.containsKey(regId)) {
                    if (!regId.isEmpty())
                        System.err.println("Skipping duplicate registration ID: " + regId);
                    return;
                }
                String officerNric = data[1].trim();
                String projectName = data[2].trim();
                OfficerRegistrationStatus status = OfficerRegistrationStatus.valueOf(data[3].trim().toUpperCase());
                Date regDate = Dparse.parseDate(data[4].trim());

                if (regDate == null) {
                    System.err.println("Skipping registration with invalid date: " + regId);
                    return;
                }

                if (!users.containsKey(officerNric) || !(users.get(officerNric) instanceof HDBOfficer)) {
                    System.err.println("Warning: Registration " + regId + " refers to invalid or non-officer NRIC: "
                            + officerNric + ". Skipping registration.");
                    return;
                }
                boolean projectExists = projects.stream()
                        .anyMatch(p -> p.getProjectName().equalsIgnoreCase(projectName));
                if (!projectExists) {
                    System.err.println("Warning: Registration " + regId + " refers to non-existent project: "
                            + projectName + ". Skipping registration.");
                    return;
                }

                OfficerRegistration registration = new OfficerRegistration(regId, officerNric, projectName, status,
                        regDate);
                this.registrations.put(registration.getRegistrationId(), registration);
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing enum/data in officer registration data: "
                        + String.join(DELIMITER, data) + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error parsing officer registration data line: " + String.join(DELIMITER, data)
                        + " - " + e.getMessage());
            }
        });
        System.out.println("Loaded " + this.registrations.size() + " officer registrations.");
        return new HashMap<>(this.registrations);
    }

    /**
     * Saves the provided map of officer registrations to officer_registrations.csv.
     * Formats registration data for CSV storage.
     * Overwrites the existing file. Updates the internal registration map to match
     * the saved state.
     *
     * @param registrationsToSave The map of registrations (RegistrationID to
     *                            OfficerRegistration object) to save.
     */
    @Override
    public void saveOfficerRegistrations(Map<String, OfficerRegistration> registrationsToSave) {
        List<String[]> dataList = new ArrayList<>();
        dataList.add(OFFICER_REGISTRATION_HEADER);
        registrationsToSave.values().stream()
                .sorted(Comparator.comparing((OfficerRegistration oR) -> oR.getRegistrationId()))
                .forEach(reg -> {
                    dataList.add(new String[] {
                            reg.getRegistrationId(),
                            reg.getOfficerNric(),
                            reg.getProjectName(),
                            reg.getStatus().name(),
                            DateUtils.formatDate(reg.getRegistrationDate())
                    });
                });
        CsvRW.writeCsv(OFFICER_REGISTRATION_FILE, dataList);
        System.out.println("Saved officer registrations.");
        this.registrations = new HashMap<>(registrationsToSave);
    }

    /**
     * Finds an officer registration by its unique ID from the internally managed
     * map.
     *
     * @param registrationId The ID of the registration to find.
     * @return The OfficerRegistration object if found, or null otherwise.
     */
    @Override
    public OfficerRegistration findRegistrationById(String registrationId) {
        return this.registrations.get(registrationId);
    }

    /**
     * Retrieves a list of all registrations (regardless of status) made by a
     * specific HDB Officer.
     * Filters the internal registration map based on the officerNric field.
     *
     * @param officerNric The NRIC of the officer.
     * @return A list of OfficerRegistration objects for the specified officer.
     *         Returns an empty list if NRIC is null or no registrations are found.
     */
    @Override
    public List<OfficerRegistration> getRegistrationsByOfficer(String officerNric) {
        if (officerNric == null)
            return new ArrayList<>();
        return this.registrations.values().stream()
                .filter(reg -> officerNric.equals(reg.getOfficerNric()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all registrations (regardless of status) associated with
     * a specific project.
     * Filters the internal registration map based on the projectName field.
     *
     * @param projectName The name of the project.
     * @return A list of OfficerRegistration objects for the specified project.
     *         Returns an empty list if projectName is null or no registrations are
     *         found.
     */
    @Override
    public List<OfficerRegistration> getRegistrationsByProject(String projectName) {
        if (projectName == null)
            return new ArrayList<>();
        return this.registrations.values().stream()
                .filter(reg -> projectName.equals(reg.getProjectName()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all officer registrations that currently have a specific
     * status.
     * Filters the internal registration map based on the status field.
     *
     * @param status The OfficerRegistrationStatus to filter by.
     * @return A list of OfficerRegistration objects with the specified status.
     *         Returns an empty list if status is null or no registrations match.
     */
    @Override
    public List<OfficerRegistration> getRegistrationsByStatus(OfficerRegistrationStatus status) {
        if (status == null)
            return new ArrayList<>();
        return this.registrations.values().stream()
                .filter(reg -> status.equals(reg.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Finds the specific registration record indicating which project an officer is
     * currently approved to handle.
     * Filters for APPROVED status and returns the first match found. Assumes an
     * officer handles at most one project at a time.
     *
     * @param officerNric The NRIC of the officer.
     * @return The OfficerRegistration object with APPROVED status for the officer,
     *         or null if none is found or NRIC is null.
     */
    @Override
    public OfficerRegistration getApprovedRegistrationForOfficer(String officerNric) {
        if (officerNric == null)
            return null;
        return this.registrations.values().stream()
                .filter(reg -> officerNric.equals(reg.getOfficerNric())
                        && reg.getStatus() == OfficerRegistrationStatus.APPROVED)
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a new officer registration to the internal map if a registration with
     * the same ID doesn't already exist.
     * Prints an error message if a duplicate ID is detected.
     *
     * @param registration The OfficerRegistration object to add.
     */
    @Override
    public void addRegistration(OfficerRegistration registration) {
        if (registration != null && !this.registrations.containsKey(registration.getRegistrationId())) {
            this.registrations.put(registration.getRegistrationId(), registration);
        } else if (registration != null) {
            System.err.println("Registration with ID " + registration.getRegistrationId() + " already exists.");
        }
    }

    /**
     * Removes an officer registration from the internal map based on its ID.
     *
     * @param registrationId The ID of the registration to remove. If null, the
     *                       method does nothing.
     * @return true if the registration was found and removed, false otherwise.
     */
    @Override
    public boolean removeRegistration(String registrationId) {
        if (registrationId != null) {
            return this.registrations.remove(registrationId) != null;
        }
        return false;
    }

    /**
     * Retrieves a copy of the map containing all officer registrations currently
     * managed by the service.
     * Returning a copy prevents external modification of the internal state.
     *
     * @return A new HashMap containing all registrations (RegistrationID to
     *         OfficerRegistration object).
     */
    @Override
    public Map<String, OfficerRegistration> getAllRegistrations() {
        return new HashMap<>(this.registrations);
    }
}
