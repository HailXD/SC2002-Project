/**
 * Service implementation for managing BTO Project data.
 * Handles loading project data from ProjectList.csv, validating against user data,
 * and saving projects back to the CSV file. Provides methods for finding, retrieving,
 * adding, and removing projects.
 *
 * @author Jordon
 */
package Services;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Enums.FlatType;
import Models.FlatTypeDetails;
import Models.HDBManager;
import Models.HDBOfficer;
import Models.Project;
import Models.User;
import Utils.DateUtils;
import Parsers.Dparse;
import Parsers.LSparse;

public class ProjectService implements IProjectService {
    private static final String DELIMITER = ",";
    private static final String DATA_DIR = "data";
    private static final String LIST_DELIMITER = ";";
    private static final String[] PROJECT_HEADER = {
            "Project Name", "Neighborhood", "Type 1", "Number of units for Type 1", "Selling price for Type 1",
            "Type 2", "Number of units for Type 2", "Selling price for Type 2",
            "Application opening date", "Application closing date", "Manager", "Officer Slot", "Officer", "Visibility"
    };
    private static final String PROJECT_FILE = DATA_DIR + File.separator + "ProjectList.csv";

    private List<Project> projects;

    /**
     * Constructs a new ProjectService. Initializes the internal project list.
     */
    public ProjectService() {
        this.projects = new ArrayList<>();
    }

    /**
     * Loads project data from ProjectList.csv.
     * Validates project names (uniqueness), flat types, dates, manager NRIC (must
     * be a valid HDBManager),
     * and officer NRICs (must be valid HDBOfficers). Adjusts officer lists based on
     * validity.
     * Populates the internal project list.
     *
     * @param users A map of NRIC to User objects, used for validating manager and
     *              officer NRICs.
     * @return A copy of the list containing all loaded and validated projects.
     */
    @Override
    public List<Project> loadProjects(Map<String, User> users) {
        this.projects.clear();
        Set<String> projectNames = new HashSet<>();

        CsvRW.readCsv(PROJECT_FILE, PROJECT_HEADER.length).forEach(data -> {
            try {
                String projectName = data[0].trim();
                if (projectName.isEmpty() || !projectNames.add(projectName.toLowerCase())) {
                    if (!projectName.isEmpty())
                        System.err.println("Skipping duplicate project name: " + projectName);
                    return;
                }

                String neighborhood = data[1].trim();
                Map<FlatType, FlatTypeDetails> flatTypes = new HashMap<>();

                if (data[2] != null && !data[2].trim().isEmpty()) {
                    FlatType type1 = FlatType.fromString(data[2].trim());
                    if (type1 != null) {
                        int units1 = Integer.parseInt(data[3].trim());
                        double price1 = Double.parseDouble(data[4].trim());
                        flatTypes.put(type1, new FlatTypeDetails(units1, units1, price1));
                    } else {
                        System.err.println("Warning: Unknown flat type '" + data[2] + "' in project '" + projectName
                                + "'. Skipping type.");
                    }
                }

                if (data[5] != null && !data[5].trim().isEmpty()) {
                    FlatType type2 = FlatType.fromString(data[5].trim());
                    if (type2 != null) {
                        int units2 = Integer.parseInt(data[6].trim());
                        double price2 = Double.parseDouble(data[7].trim());
                        flatTypes.put(type2, new FlatTypeDetails(units2, units2, price2));
                    } else {
                        System.err.println("Warning: Unknown flat type '" + data[5] + "' in project '" + projectName
                                + "'. Skipping type.");
                    }
                }

                Date openingDate = Dparse.parseDate(data[8].trim());
                Date closingDate = Dparse.parseDate(data[9].trim());
                String managerNric = data[10].trim();
                int officerSlots = Integer.parseInt(data[11].trim());

                List<String> officers = LSparse.parseListString(data[12]);

                boolean visibility = false;
                if (data.length > 13 && data[13] != null) {
                    String visibilityStr = data[13].trim();
                    if (visibilityStr.equals("1")) {
                        visibility = true;
                    } else if (!visibilityStr.equals("0") && !visibilityStr.isEmpty()) {
                        System.err.println("Warning: Invalid visibility value '" + visibilityStr + "' for project '"
                                + projectName + "'. Assuming false.");
                    }
                }

                if (!users.containsKey(managerNric) || !(users.get(managerNric) instanceof HDBManager)) {
                    System.err.println("Warning: Project '" + projectName + "' has invalid or non-manager NRIC: "
                            + managerNric + ". Skipping project.");
                    projectNames.remove(projectName.toLowerCase());
                    return;
                }
                if (openingDate == null || closingDate == null || closingDate.before(openingDate)) {
                    System.err.println("Warning: Project '" + projectName + "' has invalid application dates (Open: "
                            + data[8] + ", Close: " + data[9] + "). Skipping project.");
                    projectNames.remove(projectName.toLowerCase());
                    return;
                }
                List<String> validOfficers = new ArrayList<>();
                List<String> invalidOfficers = new ArrayList<>();
                for (String nric : officers) {
                    if (users.containsKey(nric) && users.get(nric) instanceof HDBOfficer) {
                        validOfficers.add(nric);
                    } else {
                        invalidOfficers.add(nric);
                    }
                }
                if (!invalidOfficers.isEmpty()) {
                    System.err.println("Warning: Project '" + projectName
                            + "' contains invalid or non-officer NRICs in its officer list: " + invalidOfficers
                            + ". Only valid officers retained.");
                }
                if (validOfficers.size() > officerSlots) {
                    System.err.println(
                            "Warning: Project '" + projectName + "' has more approved officers (" + validOfficers.size()
                                    + ") than slots (" + officerSlots + "). Check data. Using officer list as is.");
                }

                Project project = new Project(projectName, neighborhood, flatTypes, openingDate, closingDate,
                        managerNric, officerSlots, validOfficers, visibility);
                this.projects.add(project);

            } catch (NumberFormatException e) {
                System.err.println("Error parsing number in project data: " + String.join(DELIMITER, data) + " - "
                        + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing enum/data in project data: " + String.join(DELIMITER, data) + " - "
                        + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error parsing project data line: " + String.join(DELIMITER, data) + " - "
                        + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("Loaded " + this.projects.size() + " projects.");
        return new ArrayList<>(this.projects);
    }

    /**
     * Saves the provided list of projects to ProjectList.csv.
     * Formats project data, including flat type details (saving total units) and
     * officer lists, for CSV storage.
     * Overwrites the existing file. Updates the internal project list to match the
     * saved state.
     *
     * @param projectsToSave The list of Project objects to save.
     */
    @Override
    public void saveProjects(List<Project> projectsToSave) {
        List<String[]> dataList = new ArrayList<>();
        dataList.add(PROJECT_HEADER);

        projectsToSave.forEach(project -> {
            String[] data = new String[PROJECT_HEADER.length];
            data[0] = project.getProjectName();
            data[1] = project.getNeighborhood();

            FlatTypeDetails twoRoomDetails = project.getFlatTypeDetails(FlatType.TWO_ROOM);
            FlatTypeDetails threeRoomDetails = project.getFlatTypeDetails(FlatType.THREE_ROOM);

            if (twoRoomDetails != null) {
                data[2] = FlatType.TWO_ROOM.getDisplayName();
                data[3] = String.valueOf(twoRoomDetails.getTotalUnits());
                data[4] = String.valueOf(twoRoomDetails.getSellingPrice());
            } else {
                data[2] = "";
                data[3] = "0";
                data[4] = "0";
            }

            if (threeRoomDetails != null) {
                data[5] = FlatType.THREE_ROOM.getDisplayName();
                data[6] = String.valueOf(threeRoomDetails.getTotalUnits());
                data[7] = String.valueOf(threeRoomDetails.getSellingPrice());
            } else {
                data[5] = "";
                data[6] = "0";
                data[7] = "0";
            }

            data[8] = DateUtils.formatDate(project.getApplicationOpeningDate());
            data[9] = DateUtils.formatDate(project.getApplicationClosingDate());
            data[10] = project.getManagerNric();
            data[11] = String.valueOf(project.getMaxOfficerSlots());
            String officers = String.join(LIST_DELIMITER, project.getApprovedOfficerNrics());
            data[12] = officers;

            data[13] = project.isVisible() ? "1" : "0";

            dataList.add(data);
        });
        CsvRW.writeCsv(PROJECT_FILE, dataList);
        System.out.println("Saved projects.");
        this.projects = new ArrayList<>(projectsToSave);
    }

    /**
     * Finds a project by its name (case-insensitive) from the internally managed
     * list.
     *
     * @param name The name of the project to find.
     * @return The Project object if found, or null if the name is null, empty, or
     *         not found.
     */
    @Override
    public Project findProjectByName(String name) {
        if (name == null || name.trim().isEmpty())
            return null;
        return this.projects.stream()
                .filter(p -> p.getProjectName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a copy of the list containing all projects currently managed by the
     * service.
     * Returning a copy prevents external modification of the internal state.
     *
     * @return A new ArrayList containing all Project objects.
     */
    @Override
    public List<Project> getAllProjects() {
        return new ArrayList<>(this.projects);
    }

    /**
     * Retrieves a list of projects managed by a specific HDB Manager NRIC.
     * Filters the internal project list based on the managerNric field.
     *
     * @param managerNric The NRIC of the manager.
     * @return A list of projects managed by the specified manager. Returns an empty
     *         list if NRIC is null or no projects are found.
     */
    @Override
    public List<Project> getProjectsManagedBy(String managerNric) {
        if (managerNric == null)
            return new ArrayList<>();
        return this.projects.stream()
                .filter(p -> managerNric.equals(p.getManagerNric()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a new project to the internal list if a project with the same name
     * doesn't already exist.
     * Prints an error message if a duplicate name is detected.
     *
     * @param project The Project object to add.
     */
    @Override
    public void addProject(Project project) {
        if (project != null && findProjectByName(project.getProjectName()) == null) {
            this.projects.add(project);
        } else if (project != null) {
            System.err.println(
                    "Project with name '" + project.getProjectName() + "' already exists. Cannot add duplicate.");
        }
    }

    /**
     * Removes a project from the internal list based on its name.
     *
     * @param project The Project object to remove. If null, the method does
     *                nothing.
     * @return true if a project with the matching name was found and removed, false
     *         otherwise.
     */
    @Override
    public boolean removeProject(Project project) {
        if (project != null) {
            return this.projects.removeIf(p -> p.getProjectName().equals(project.getProjectName()));
        }
        return false;
    }
}
