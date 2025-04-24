/**
 * Represents an enquiry submitted by an applicant regarding a specific BTO project.
 * Contains the enquiry text, applicant details, project name, dates, and potentially a reply from an HDB staff member.
 * Manages its own unique ID generation using a static counter.
 *
 * @author Jordon
 */
package Models;

import java.util.Date;

public class Enquiry {
    private final String enquiryId;
    private final String applicantNric;
    private final String projectName;
    private String enquiryText;
    private String replyText;
    private String repliedByNric;
    private final Date enquiryDate;
    private Date replyDate;
    private static long nextId = 1;

    /**
     * Constructs a new Enquiry object when an applicant submits it.
     * Automatically generates a unique enquiry ID (e.g., "ENQ1", "ENQ2", ...).
     * Initializes reply fields to null.
     *
     * @param applicantNric The NRIC of the applicant submitting the enquiry. Cannot
     *                      be null.
     * @param projectName   The name of the project the enquiry is about. Cannot be
     *                      null.
     * @param enquiryText   The text content of the enquiry. Cannot be null or
     *                      empty.
     * @param enquiryDate   The date the enquiry was submitted. Cannot be null.
     * @throws IllegalArgumentException if any parameter is invalid (null or empty
     *                                  text).
     */
    public Enquiry(String applicantNric, String projectName, String enquiryText, Date enquiryDate) {
        if (applicantNric == null || projectName == null || enquiryText == null || enquiryText.trim().isEmpty()
                || enquiryDate == null) {
            throw new IllegalArgumentException("Invalid Enquiry parameters");
        }
        synchronized (Enquiry.class) {
            this.enquiryId = "ENQ" + (nextId++);
        }
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.enquiryText = enquiryText;
        this.enquiryDate = enquiryDate;
        this.replyText = null;
        this.repliedByNric = null;
        this.replyDate = null;
    }

    /**
     * Constructs an Enquiry object when loading data from storage.
     * Allows setting all fields, including the ID, reply details, and dates.
     * Updates the static `nextId` counter based on the loaded ID to prevent future
     * collisions.
     *
     * @param enquiryId     The unique ID of the enquiry being loaded. Cannot be
     *                      null.
     * @param applicantNric The NRIC of the applicant. Cannot be null.
     * @param projectName   The name of the project. Cannot be null.
     * @param enquiryText   The text content of the enquiry. Cannot be null.
     * @param replyText     The text of the reply (can be null if not replied).
     * @param repliedByNric The NRIC of the staff member who replied (can be null).
     * @param enquiryDate   The date the enquiry was submitted. Cannot be null.
     * @param replyDate     The date the reply was submitted (can be null).
     * @throws IllegalArgumentException if required fields (ID, NRIC, project, text,
     *                                  enquiry date) are null.
     */
    public Enquiry(String enquiryId, String applicantNric, String projectName, String enquiryText, String replyText,
            String repliedByNric, Date enquiryDate, Date replyDate) {
        if (enquiryId == null || applicantNric == null || projectName == null || enquiryText == null
                || enquiryDate == null) {
            throw new IllegalArgumentException("Required Enquiry fields cannot be null when loading");
        }
        this.enquiryId = enquiryId;
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.enquiryText = enquiryText;
        this.replyText = (replyText == null || replyText.trim().isEmpty()) ? null : replyText.trim();
        this.repliedByNric = (repliedByNric == null || repliedByNric.trim().isEmpty()) ? null : repliedByNric.trim();
        this.enquiryDate = enquiryDate;
        this.replyDate = replyDate;

        updateNextId(enquiryId);
    }

    /**
     * Static helper method called during loading to update the `nextId` counter.
     * Ensures that newly generated IDs will be greater than any loaded ID.
     * Parses the numeric part of the loaded ID (e.g., "123" from "ENQ123").
     * Synchronized to handle potential concurrency if loading were multi-threaded
     * (though currently unlikely).
     *
     * @param loadedEnquiryId The enquiry ID string loaded from storage.
     */
    public static void updateNextId(String loadedEnquiryId) {
        if (loadedEnquiryId != null && loadedEnquiryId.startsWith("ENQ")) {
            try {
                long idNum = Long.parseLong(loadedEnquiryId.substring(3));
                synchronized (Enquiry.class) {
                    if (idNum >= nextId) {
                        nextId = idNum + 1;
                    }
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                System.err.println("Warning: Could not parse enquiry ID for nextId update: " + loadedEnquiryId);
            }
        }
    }

    /**
     * Static method to be called once after all enquiries have been loaded.
     * Prints the final value of the `nextId` counter for informational purposes.
     */
    public static void finalizeNextIdInitialization() {
        System.out.println("Enquiry nextId initialized to: " + nextId);
    }

    /**
     * Gets the unique ID of the enquiry.
     * 
     * @return The enquiry ID string (e.g., "ENQ1").
     */
    public String getEnquiryId() {
        return enquiryId;
    }

    /**
     * Gets the NRIC of the applicant who submitted the enquiry.
     * 
     * @return The applicant's NRIC string.
     */
    public String getApplicantNric() {
        return applicantNric;
    }

    /**
     * Gets the name of the project the enquiry is associated with.
     * 
     * @return The project name string.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Gets the text content of the enquiry.
     * 
     * @return The enquiry text string.
     */
    public String getEnquiryText() {
        return enquiryText;
    }

    /**
     * Gets the text content of the reply, if any.
     * 
     * @return The reply text string, or null if not replied.
     */
    public String getReplyText() {
        return replyText;
    }

    /**
     * Gets the NRIC of the staff member who replied, if any.
     * 
     * @return The replier's NRIC string, or null if not replied.
     */
    public String getRepliedByNric() {
        return repliedByNric;
    }

    /**
     * Gets the date the enquiry was submitted.
     * 
     * @return The enquiry submission date.
     */
    public Date getEnquiryDate() {
        return enquiryDate;
    }

    /**
     * Gets the date the reply was submitted, if any.
     * 
     * @return The reply submission date, or null if not replied.
     */
    public Date getReplyDate() {
        return replyDate;
    }

    /**
     * Sets the enquiry text. Can only be done if the enquiry has not yet been
     * replied to.
     * Ensures the new text is not null or empty.
     *
     * @param enquiryText The new enquiry text.
     * @return true if the text was successfully updated, false otherwise (already
     *         replied or invalid text).
     */
    public boolean setEnquiryText(String enquiryText) {
        if (!isReplied() && enquiryText != null && !enquiryText.trim().isEmpty()) {
            this.enquiryText = enquiryText.trim();
            return true;
        } else if (isReplied()) {
            System.err.println("Cannot edit an enquiry that has already been replied to (ID: " + enquiryId + ").");
            return false;
        } else {
            System.err.println("Enquiry text cannot be empty.");
            return false;
        }
    }

    /**
     * Sets the reply details for this enquiry.
     * Marks the enquiry as replied.
     * Ensures reply text, replier NRIC, and reply date are not null or empty.
     *
     * @param replyText     The text content of the reply.
     * @param repliedByNric The NRIC of the staff member providing the reply.
     * @param replyDate     The date the reply was submitted.
     * @return true if the reply details were successfully set, false otherwise
     *         (invalid parameters).
     */
    public boolean setReply(String replyText, String repliedByNric, Date replyDate) {
        if (replyText != null && !replyText.trim().isEmpty() && repliedByNric != null && replyDate != null) {
            this.replyText = replyText.trim();
            this.repliedByNric = repliedByNric;
            this.replyDate = replyDate;
            return true;
        } else {
            System.err.println("Invalid reply parameters provided for enquiry ID: " + enquiryId);
            return false;
        }
    }

    /**
     * Checks if this enquiry has been replied to.
     * An enquiry is considered replied if the `replyText` is not null and not
     * empty.
     *
     * @return true if the enquiry has a reply, false otherwise.
     */
    public boolean isReplied() {
        return this.replyText != null && !this.replyText.isEmpty();
    }
}
