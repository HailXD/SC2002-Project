
/**
 * Service implementation for managing User data (Applicants, Officers, Managers).
 * Handles loading user data from separate CSV files for each role and saving them back.
 * Provides methods for finding users by NRIC and retrieving all users.
 *
 * @author Jordon
 */
package Services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Enums.MaritalStatus;
import Models.Applicant;
import Models.HDBManager;
import Models.HDBOfficer;
import Models.User;

public class UserService implements IUserService {
    private static final String DATA_DIR = "data";
    private static final String APPLICANT_LIST_FILE = DATA_DIR + File.separator + "ApplicantList.csv";
    private static final String OFFICER_LIST_FILE = DATA_DIR + File.separator + "OfficerList.csv";
    private static final String MANAGER_LIST_FILE = DATA_DIR + File.separator + "ManagerList.csv";
    private static final String DELIMITER = ",";
    private static final String[] APPLICANT_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };
    private static final String[] OFFICER_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };
    private static final String[] MANAGER_HEADER = { "Name", "NRIC", "Age", "Marital Status", "Password" };

    private Map<String, User> users;

    /**
     * Constructs a new UserService. Initializes the internal user map.
     */
    public UserService() {
        this.users = new HashMap<>();
    }

    /**
     * Loads user data from ApplicantList.csv, OfficerList.csv, and ManagerList.csv.
     * Validates NRIC format and handles potential duplicates or role conflicts
     * (e.g., user in multiple files).
     * Populates the internal user map.
     *
     * @return The map containing all loaded users, keyed by NRIC.
     */
    @Override
    public Map<String, User> loadUsers() {
        this.users.clear();

        CsvRW.readCsv(APPLICANT_LIST_FILE, APPLICANT_HEADER.length).forEach(data -> {
            try {
                String nric = data[1].trim();
                if (!Utils.NricValidator.isValidNric(nric) || this.users.containsKey(nric)) {
                    if (this.users.containsKey(nric))
                        System.err.println("Duplicate NRIC found in ApplicantList: " + nric + ". Skipping duplicate.");
                    else
                        System.err.println("Invalid NRIC format in ApplicantList: " + nric + ". Skipping.");
                    return;
                }
                int age = Integer.parseInt(data[2].trim());
                MaritalStatus status = MaritalStatus.valueOf(data[3].trim().toUpperCase());
                Applicant applicant = new Applicant(nric, data[4].trim(), data[0].trim(), age, status);
                this.users.put(nric, applicant);
            } catch (Exception e) {
                System.err.println(
                        "Error parsing applicant data line: " + String.join(DELIMITER, data) + " - " + e.getMessage());
            }
        });

        CsvRW.readCsv(OFFICER_LIST_FILE, OFFICER_HEADER.length).forEach(data -> {
            try {
                String nric = data[1].trim();
                if (!Utils.NricValidator.isValidNric(nric)) {
                    System.err.println("Invalid NRIC format in OfficerList: " + nric + ". Skipping.");
                    return;
                }
                int age = Integer.parseInt(data[2].trim());
                MaritalStatus status = MaritalStatus.valueOf(data[3].trim().toUpperCase());
                HDBOfficer officer = new HDBOfficer(nric, data[4].trim(), data[0].trim(), age, status);
                if (this.users.containsKey(nric) && !(this.users.get(nric) instanceof HDBOfficer)) {
                    System.out.println(
                            "Info: User " + nric + " found in both Applicant and Officer lists. Using Officer role.");
                } else if (this.users.containsKey(nric)) {
                    System.err.println("Duplicate NRIC found in OfficerList: " + nric + ". Skipping duplicate.");
                    return;
                }
                this.users.put(nric, officer);
            } catch (Exception e) {
                System.err.println(
                        "Error parsing officer data line: " + String.join(DELIMITER, data) + " - " + e.getMessage());
            }
        });

        CsvRW.readCsv(MANAGER_LIST_FILE, MANAGER_HEADER.length).forEach(data -> {
            try {
                String nric = data[1].trim();
                if (!Utils.NricValidator.isValidNric(nric)) {
                    System.err.println("Invalid NRIC format in ManagerList: " + nric + ". Skipping.");
                    return;
                }
                int age = Integer.parseInt(data[2].trim());
                MaritalStatus status = MaritalStatus.valueOf(data[3].trim().toUpperCase());
                HDBManager manager = new HDBManager(nric, data[4].trim(), data[0].trim(), age, status);
                if (this.users.containsKey(nric) && !(this.users.get(nric) instanceof HDBManager)) {
                    System.out.println("Info: User " + nric + " found in other lists. Using Manager role.");
                } else if (this.users.containsKey(nric)) {
                    System.err.println("Duplicate NRIC found in ManagerList: " + nric + ". Skipping duplicate.");
                    return;
                }
                this.users.put(nric, manager);
            } catch (Exception e) {
                System.err.println(
                        "Error parsing manager data line: " + String.join(DELIMITER, data) + " - " + e.getMessage());
            }
        });

        System.out.println("Loaded " + this.users.size() + " unique users.");
        return this.users;
    }

    /**
     * Saves the provided map of users back to their respective CSV files
     * (ApplicantList, OfficerList, ManagerList).
     * Overwrites the existing files. Updates the internal user map to match the
     * saved state.
     *
     * @param usersToSave The map of users (NRIC to User object) to save.
     */
    @Override
    public void saveUsers(Map<String, User> usersToSave) {
        List<String[]> applicantData = new ArrayList<>();
        List<String[]> officerData = new ArrayList<>();
        List<String[]> managerData = new ArrayList<>();

        applicantData.add(APPLICANT_HEADER);
        officerData.add(OFFICER_HEADER);
        managerData.add(MANAGER_HEADER);

        usersToSave.values().forEach(user -> {
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
        System.out.println("Saved users.");
        if (usersToSave == this.users) {
            this.users = new HashMap<>(usersToSave);
        } else {
        }
    }

    /**
     * Finds a user by their NRIC from the internally managed map.
     *
     * @param nric The NRIC of the user to find.
     * @return The User object if found, or null otherwise.
     */
    @Override
    public User findUserByNric(String nric) {
        return this.users.get(nric);
    }

    /**
     * Retrieves a copy of the map containing all users currently managed by the
     * service.
     * Returning a copy prevents external modification of the internal state.
     *
     * @return A new HashMap containing all users (NRIC to User object).
     */
    @Override
    public Map<String, User> getAllUsers() {
        return new HashMap<>(this.users);
    }
}
