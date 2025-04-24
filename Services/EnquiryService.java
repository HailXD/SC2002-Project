/**
 * Service implementation for managing Enquiry data.
 * Handles loading enquiry data from enquiries.csv, managing unique ID generation,
 * and saving enquiries back. Provides methods for finding, retrieving, adding, and removing enquiries.
 *
 * @author Jordon
 */
package Services;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Models.Enquiry;
import Parsers.Dparse;
import Utils.DateUtils;

public class EnquiryService implements IEnquiryService {
    private static final String DATA_DIR = "data";
    private static final String DELIMITER = ",";
    private static final String ENQUIRY_FILE = DATA_DIR + File.separator + "enquiries.csv";
    private static final String[] ENQUIRY_HEADER = { "EnquiryID", "ApplicantNRIC", "ProjectName", "EnquiryText",
            "ReplyText", "RepliedByNRIC", "EnquiryDate", "ReplyDate" };

    private List<Enquiry> enquiries;

    /**
     * Constructs a new EnquiryService. Initializes the internal enquiry list.
     */
    public EnquiryService() {
        this.enquiries = new ArrayList<>();
    }

    /**
     * Loads enquiry data from enquiries.csv.
     * Validates enquiry IDs (uniqueness), dates, and required fields (text, date).
     * Updates the static `nextId` in the Enquiry class based on loaded IDs to
     * ensure future generated IDs are unique.
     * Populates the internal enquiry list.
     *
     * @return A copy of the list containing all loaded enquiries.
     */
    @Override
    public List<Enquiry> loadEnquiries() {
        this.enquiries.clear();
        Set<String> enquiryIds = new HashSet<>();

        CsvRW.readCsv(ENQUIRY_FILE, ENQUIRY_HEADER.length).forEach(data -> {
            try {
                String id = data[0].trim();
                if (id.isEmpty() || !enquiryIds.add(id)) {
                    if (!id.isEmpty())
                        System.err.println("Skipping duplicate enquiry ID: " + id);
                    return;
                }
                String applicantNric = data[1].trim();
                String projectName = data[2].trim();
                String text = data[3].trim();
                String reply = data[4].trim();
                String repliedBy = data[5].trim();
                Date enqDate = Dparse.parseDate(data[6].trim());
                Date replyDate = Dparse.parseDate(data[7].trim());

                if (text.isEmpty() || enqDate == null) {
                    System.err.println("Skipping enquiry with missing text or invalid date: " + id);
                    enquiryIds.remove(id);
                    return;
                }
                if ((reply != null && !reply.isEmpty())
                        && (repliedBy == null || repliedBy.isEmpty() || replyDate == null)) {
                    System.err.println("Warning: Enquiry " + id
                            + " seems replied but missing replier NRIC or reply date. Loading reply as is.");
                }

                Enquiry enquiry = new Enquiry(id, applicantNric, projectName, text, reply, repliedBy, enqDate,
                        replyDate);
                this.enquiries.add(enquiry);
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing enum/data in enquiry data: " + String.join(DELIMITER, data) + " - "
                        + e.getMessage());
            } catch (Exception e) {
                System.err.println(
                        "Error parsing enquiry data line: " + String.join(DELIMITER, data) + " - " + e.getMessage());
            }
        });
        Enquiry.finalizeNextIdInitialization();
        System.out.println("Loaded " + this.enquiries.size() + " enquiries.");
        return new ArrayList<>(this.enquiries);
    }

    /**
     * Saves the provided list of enquiries to enquiries.csv.
     * Formats enquiry data, including handling null reply fields, for CSV storage.
     * Overwrites the existing file. Updates the internal enquiry list to match the
     * saved state.
     *
     * @param enquiriesToSave The list of Enquiry objects to save.
     */
    @Override
    public void saveEnquiries(List<Enquiry> enquiriesToSave) {
        List<String[]> dataList = new ArrayList<>();
        dataList.add(ENQUIRY_HEADER);
        enquiriesToSave.stream()
                .sorted(Comparator.comparing((Enquiry e) -> e.getEnquiryId()))
                .forEach(enq -> {
                    dataList.add(new String[] {
                            enq.getEnquiryId(),
                            enq.getApplicantNric(),
                            enq.getProjectName(),
                            enq.getEnquiryText(),
                            enq.getReplyText() == null ? "" : enq.getReplyText(),
                            enq.getRepliedByNric() == null ? "" : enq.getRepliedByNric(),
                            DateUtils.formatDate(enq.getEnquiryDate()),
                            enq.getReplyDate() == null ? "" : DateUtils.formatDate(enq.getReplyDate())
                    });
                });
        CsvRW.writeCsv(ENQUIRY_FILE, dataList);
        System.out.println("Saved enquiries.");
        this.enquiries = new ArrayList<>(enquiriesToSave);
    }

    /**
     * Adds a new enquiry to the internal list if an enquiry with the same ID
     * doesn't already exist.
     * Prints an error message if a duplicate ID is detected.
     *
     * @param enquiry The Enquiry object to add.
     */
    @Override
    public void addEnquiry(Enquiry enquiry) {
        if (enquiry != null && findEnquiryById(enquiry.getEnquiryId()) == null) {
            this.enquiries.add(enquiry);
        } else if (enquiry != null) {
            System.err.println("Enquiry with ID " + enquiry.getEnquiryId() + " already exists.");
        }
    }

    /**
     * Removes an enquiry from the internal list based on its ID.
     *
     * @param enquiryId The ID of the enquiry to remove. If null, the method does
     *                  nothing.
     * @return true if an enquiry with the matching ID was found and removed, false
     *         otherwise.
     */
    @Override
    public boolean removeEnquiry(String enquiryId) {
        if (enquiryId != null) {
            return this.enquiries.removeIf(e -> enquiryId.equals(e.getEnquiryId()));
        }
        return false;
    }

    /**
     * Finds an enquiry by its unique ID from the internally managed list.
     *
     * @param enquiryId The ID of the enquiry to find.
     * @return The Enquiry object if found, or null if the ID is null or not found.
     */
    @Override
    public Enquiry findEnquiryById(String enquiryId) {
        if (enquiryId == null)
            return null;
        return this.enquiries.stream()
                .filter(e -> enquiryId.equals(e.getEnquiryId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a list of all enquiries submitted by a specific applicant.
     * Filters the internal enquiry list based on the applicantNric field.
     *
     * @param applicantNric The NRIC of the applicant.
     * @return A list of Enquiry objects submitted by the specified applicant.
     *         Returns an empty list if NRIC is null or no enquiries are found.
     */
    @Override
    public List<Enquiry> getEnquiriesByApplicant(String applicantNric) {
        if (applicantNric == null)
            return new ArrayList<>();
        return this.enquiries.stream()
                .filter(e -> applicantNric.equals(e.getApplicantNric()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all enquiries associated with a specific project.
     * Filters the internal enquiry list based on the projectName field.
     *
     * @param projectName The name of the project.
     * @return A list of Enquiry objects for the specified project. Returns an empty
     *         list if projectName is null or no enquiries are found.
     */
    @Override
    public List<Enquiry> getEnquiriesByProject(String projectName) {
        if (projectName == null)
            return new ArrayList<>();
        return this.enquiries.stream()
                .filter(e -> projectName.equals(e.getProjectName()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a copy of the list containing all enquiries currently managed by
     * the service.
     * Returning a copy prevents external modification of the internal state.
     *
     * @return A new ArrayList containing all Enquiry objects.
     */
    @Override
    public List<Enquiry> getAllEnquiries() {
        return new ArrayList<>(this.enquiries);
    }
}
