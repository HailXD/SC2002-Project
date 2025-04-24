- [x] **1. Valid User Login**  
  - Expected: User can access their dashboard based on their roles  
  - Failure: User cannot log in or receives incorrect error messages

- [x] **2. Invalid NRIC Format**  
  - Expected: User receives a notification about incorrect NRIC format  
  - Failure: User is allowed to log in with an invalid NRIC

- [x] **3. Incorrect Password**  
  - Expected: System denies access and alerts the user to incorrect password  
  - Failure: User logs in successfully with a wrong password

- [x] **4. Password Change Functionality**  
  - Expected: System updates password, prompts re‑login, and allows login with new credentials  
  - Failure: System does not update the password or denies access with the new password

- [x] **5. Project Visibility Based on User Group and Toggle**  
  - Expected: Projects are visible only to users based on age, marital status, and the visibility setting  
  - Failure: Users see projects not relevant to their group or when visibility is off

- [x] **6. Project Application**  
  - Expected: Users can apply only for projects relevant to their group or when visibility is on  
  - Failure: Users can apply for projects not relevant to their group or when visibility is off

- [x] **7. Viewing Application Status after Visibility Toggle Off**  
  - Expected: Applicants continue to access their application details regardless of visibility setting  
  - Failure: Application details become inaccessible once visibility is off

- [x] **8. Single Flat Booking per Successful Application**  
  - Expected: System allows booking one flat and restricts further bookings  
  - Failure: Applicant is able to book more than one flat
  - Comments: Can't even continue to apply if already applied/booked for another so i assume yes

- [x] **9. Applicant’s Enquiries Management**  
  - Expected: Enquiries can be submitted, displayed, modified, and removed successfully  
  - Failure: Enquiries cannot be submitted, edited, deleted, or do not display correctly
  - Comments: Officers don't work yet, so don't know if officer replying works, but can submit, print, edit, remove from applicant

- [x] **10. HDB Officer Registration Eligibility**  
  - Expected: System allows registration only under compliant conditions  
  - Failure: System allows registration while the officer is an applicant or registered for another project in the same period

- [x] **11. HDB Officer Registration Status**  
  - Expected: Officers can view pending or approved status updates on their profiles  
  - Failure: Status updates are not visible or are incorrect, but can submit application

- [x] **12. Project Detail Access for HDB Officer**  
  - Expected: Officers can always access full project details, even when visibility is off  
  - Failure: Project details are inaccessible when visibility is toggled off

- [x] **13. Restriction on Editing Project Details**  
  - Expected: Edit functionality is disabled or absent for HDB Officers  
  - Failure: Officers are able to make changes to project details

- [x] **14. Response to Project Enquiries**  
  - Expected: Officers & Managers can access and respond to enquiries efficiently  
  - Failure: Officers & Managers cannot see enquiries, or their responses are not recorded11

- [x] **15. Flat Selection and Booking Management**  
  - Expected: Officers retrieve correct applications, update flat availability accurately, and log booking details correctly  
  - Failure: Incorrect retrieval or updates, or failure to reflect booking details accurately
  - Comments: Currently applicant choose whether to apply for 2 or 3 room BTO, I assume that is correct.

- [x] **16. Receipt Generation for Flat Booking**  
  - Expected: Accurate and complete receipts are generated for each successful booking  
  - Failure: Receipts are incomplete, inaccurate, or fail to generate

- [x] **17. Create, Edit, and Delete BTO Project Listings**  
  - Expected: Managers can add new projects, modify existing details, and remove projects  
  - Failure: Inability to create, edit, or delete projects or errors during these operations

- [x] **18. Single Project Management per Application Period**  
  - Expected: System prevents assigning more than one project to a manager within the same application dates  
  - Failure: Manager is able to handle multiple projects during the same period

- [x] **19. Toggle Project Visibility**  
  - Expected: Changes in visibility should be accurately reflected in the project list visible to applicants  
  - Failure: Visibility settings do not update or do not affect the project listing as expected

- [x] **20. View All and Filtered Project Listings**  
  - Expected: Managers see all projects and can filter down to their own projects  
  - Failure: Inability to view all projects or incorrect filtering results

- [x] **21. Manage HDB Officer Registrations**  
  - Expected: Managers handle officer registrations effectively, with system updates reflecting changes accurately  
  - Failure: Mismanagement of registrations or slot counts do not update properly

- [x] **22. Approve or Reject BTO Applications and Withdrawals**  
  - Expected: Approvals and rejections process correctly, with system updates reflecting the decisions  
  - Failure: Incorrect or failed processing of applications or withdrawals
  
- [x] **23. Generate and Filter Reports**  
  - Expected: Accurate report generation with options to filter by various categories  
  - Failure: Reports are inaccurate, incomplete, or filtering does not work as expected
