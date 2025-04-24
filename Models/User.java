/**
 * Abstract base class representing a user in the BTO system
 * Contains common attributes like NRIC, password, name, age, and marital status
 * Subclasses define specific roles (Applicant, HDBOfficer, HDBManager)
 *
 * @author Jun Yang
 */
package Models;

import Enums.MaritalStatus;
import Enums.UserRole;

public abstract class User {
    private final String nric;
    private String password;
    private final String name;
    private final int age;
    private final MaritalStatus maritalStatus;

    /**
     * Constructs a new User object
     *
     * @param nric The user's NRIC Must not be null
     * @param password The user's password. Must not be null
     * @param name The user's name. Must not be null
     * @param age The user's age
     * @param maritalStatus The user's marital status. Must not be null
     * @throws IllegalArgumentException if any required field is null
     */
    public User(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        if (nric == null || password == null || name == null || maritalStatus == null) {
            throw new IllegalArgumentException("User fields cannot be null");
        }
        this.nric = nric;
        this.password = password;
        this.name = name;
        this.age = age;
        this.maritalStatus = maritalStatus;
    }

    /**
     * Gets the user's NRIC.
     * 
     * @return The NRIC string.
     */
    public String getNric() {
        return nric;
    }

    /**
     * Gets the user's password.
     * 
     * @return The password string.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the user's name.
     * 
     * @return The name string.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the user's age.
     * 
     * @return The age integer.
     */
    public int getAge() {
        return age;
    }

    /**
     * Gets the user's marital status.
     * 
     * @return The marital status enum.
     */
    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * Sets the user's password.
     * 
     * @param password The new password string.
     */
    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            this.password = password;
        } else {
            System.err.println("Warning: Attempted to set null or empty password for NRIC: " + nric);
        }
    }

    /**
     * Abstract method to get the user's role.
     * 
     * @return The user role enum.
     */
    public abstract UserRole getRole();
}