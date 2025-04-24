/**
 * Represents an HDB Manager user in the BTO system
 *
 * @author Jun Yang
 */
package Models;

import Enums.MaritalStatus;
import Enums.UserRole;

public class HDBManager extends User {
    /**
     * Constructs a new HDBOfficer object
     *
     * @param nric          The applicant's NRIC. Must not be null
     * @param password      The applicant's password. Must not be null
     * @param name          The applicant's name. Must not be null
     * @param age           The applicant's age
     * @param maritalStatus The applicant's marital status. Must not be null
     * @throws IllegalArgumentException if any required field is null
     */
    public HDBManager(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        super(nric, password, name, age, maritalStatus);
    }

    /**
     * Gets the user's role
     * 
     * @return The user role enum
     */
    @Override
    public UserRole getRole() { return UserRole.HDB_MANAGER; }

}