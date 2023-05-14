import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import com.google.api.services.drive.Drive;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

public class GoogleClassroom {
    public final Classroom classroomService;
    public final Drive driveService;
//    private final List<CourseWork> assignments = new ArrayList<>();
    public JProgressBar  progressBar;
    public JFrame frame;
    public DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel;
    public DefaultListModel<String> categorizedStudentsModel;
    public JList<String> categorizedStudentsList;
    public StudentAttachmentsViewer attachmentsViewer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GoogleClassroom().createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                System.out.println("Security exception!");
                e.printStackTrace();
            }
        });
    }

    public GoogleClassroom() throws IOException, GeneralSecurityException {
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        classroomService = authenticator.getClassroomService();
        driveService = authenticator.getDriveService();
        attachmentsViewer = null; //initially null until we have retrieved some submissions
    }

    private void createAndShowGUI() throws IOException {
        List<Course> courses = getCourses(classroomService);

        frame = new JFrame("Google Classroom Courses and Assignments");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        DefaultListModel<Course> courseListModel = new DefaultListModel<>();
        for (Course course : courses) {
            courseListModel.addElement(course);
        }

        JList<Course> courseList = new JList<>(courseListModel);
        courseList.setCellRenderer(new CourseCellRenderer());
        JScrollPane courseScrollPane = new JScrollPane(courseList);

        DefaultListModel<com.google.api.services.classroom.model.CourseWork> assignmentListModel = new DefaultListModel<>();
        JList<com.google.api.services.classroom.model.CourseWork> assignmentsList = new JList<>(assignmentListModel);
        assignmentsList.setCellRenderer(new CourseWorkCellRenderer());
        JScrollPane assignmentScrollPane = new JScrollPane(assignmentsList);

        studentSubmissionsListModel = new DefaultListModel<>();
        JList<NamedStudentSubmission> studentSubmissionsList = new JList<>(studentSubmissionsListModel);
        studentSubmissionsList.setCellRenderer(new StudentSubmissionCellRenderer());

        JSplitPane courseAndAssignmentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, courseScrollPane, assignmentScrollPane);
        courseAndAssignmentSplitPane.setDividerLocation(400);

        categorizedStudentsModel = new DefaultListModel<>();
        categorizedStudentsList = new JList<>(categorizedStudentsModel);
        categorizedStudentsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categorizedStudentsList.setLayoutOrientation(JList.VERTICAL);
        categorizedStudentsList.setVisibleRowCount(-1);
        JScrollPane categorizedStudentsListScrollPane = new JScrollPane(categorizedStudentsList);


        DefaultListModel<String> submittedAttachmentsListModel = new DefaultListModel<>();
        JList<String> submittedAttachmentsList = new JList<>(submittedAttachmentsListModel);
        JScrollPane submittedAttachmentsScrollPane = new JScrollPane(submittedAttachmentsList);

        JSplitPane submissionsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categorizedStudentsListScrollPane, submittedAttachmentsScrollPane);

        submissionsSplitPane.setDividerLocation(400);
        submissionsSplitPane.setLeftComponent(categorizedStudentsListScrollPane);
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, courseAndAssignmentSplitPane, submissionsSplitPane);
        mainSplitPane.setDividerLocation(200);

        // assign selection listeners
        courseList.addListSelectionListener(createCourseListSelectionListener(frame, courseList, assignmentListModel, courses, classroomService));
        assignmentsList.addListSelectionListener(createAssignmentsListSelectionListener(frame, courseList, assignmentsList, studentSubmissionsListModel, categorizedStudentsModel, courses, classroomService));
        categorizedStudentsList.addListSelectionListener(createCategorizedStudentsListSelectionListener(categorizedStudentsList, studentSubmissionsList, studentSubmissionsListModel, submittedAttachmentsListModel));
        studentSubmissionsList.addListSelectionListener(createStudentSubmissionsListSelectionListener(frame, studentSubmissionsList));

        // Create a JProgressBar instance
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false); // Initially set it to invisible

        // Add the progress bar to the main panel at the bottom
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH); // Add the progress bar here

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }



    private List<Course> getCourses(Classroom service) {
        List<Course> courses = new ArrayList<>();
        try {
            String nextPageToken = null;
            do {
                ListCoursesResponse response = service.courses()
                        .list()
                        .setCourseStates(Arrays.asList("ACTIVE")) // Add this line to filter only active courses
                        .setPageSize(99)
                        .setPageToken(nextPageToken)
                        .execute();
                courses.addAll(response.getCourses());
                nextPageToken = response.getNextPageToken();
            } while (nextPageToken != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
        } else {
            System.out.println("Courses:");
            for (Course course : courses) {
                System.out.printf("%s (%s)\n", course.getName(), course.getId());
            }
        }
        return courses;
    }

    private List<CourseWork> getAssignments(Classroom service, String courseId) throws IOException {
        List<CourseWork> courseWorks = service.courses().courseWork().list(courseId).setPageSize(10).execute().getCourseWork();
        if (courseWorks == null || courseWorks.isEmpty()) {
            System.out.println("No assignments found.");
        } else {
            System.out.println("Assignments:");
            for (CourseWork courseWork : courseWorks) {
                System.out.printf("%s (%s)\n", courseWork.getTitle(), courseWork.getId());
            }
        }
        return courseWorks;
    }

    public List<NamedStudentSubmission> getStudentSubmissions(String courseId, String courseWorkId, Map<String, String> studentAttachments, FetchSubmissionsWorker worker) throws IOException {
        List<NamedStudentSubmission> submissions = new ArrayList<>();
        String nextPageToken = null;
            do {
                System.out.println("LOG: getting submission page...");
                com.google.api.services.classroom.model.ListStudentSubmissionsResponse response = classroomService
                    .courses()
                    .courseWork()
                    .studentSubmissions()
                    .list(courseId, courseWorkId)
                    .setPageSize(10)
                    .setPageToken(nextPageToken)
                    .execute();

            for (StudentSubmission submission : response.getStudentSubmissions()) {
                String studentId = submission.getUserId();
                System.out.println("LOG: fetching student " + studentId);
                UserProfile studentProfile = classroomService.userProfiles().get(studentId).execute();
                String studentName = studentProfile.getName().getFullName();
                NamedStudentSubmission namedSubmission = new NamedStudentSubmission(studentName, submission);
                submissions.add(namedSubmission);

                // Fetch attachment
                System.out.println("LOG: fetching attachments");
                List<Attachment> attachments = namedSubmission.getSubmission().getAssignmentSubmission().getAttachments();
                worker.processAttachments(attachments, studentName);

                if (!attachments.isEmpty() && attachments.get(0).getDriveFile() != null) {
                    String documentId = attachments.get(0).getDriveFile().getId();
                    studentAttachments.put(studentName, documentId); // Store the file ID instead of the HTML content
                }
                worker.publishProgress(1);
            }

            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null);

        System.out.println("LOG: fetch complete!");
        return submissions;
    }

    private ListSelectionListener createCourseListSelectionListener(JFrame frame, JList<Course> courseList, DefaultListModel<CourseWork> assignmentListModel, List<Course> courses, Classroom service) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = courseList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String courseId = courses.get(selectedIndex).getId();

                    // Set the cursor to wait cursor
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        List<com.google.api.services.classroom.model.CourseWork> assignments = getAssignments(service, courseId);
                        assignmentListModel.clear();
                        categorizedStudentsModel.clear();
                        if (assignments != null) {
                            for (com.google.api.services.classroom.model.CourseWork assignment : assignments) {
                                assignmentListModel.addElement(assignment);
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        // Set the cursor back to default cursor
                        frame.setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        };
    }
    Map<String, String> studentAttachments = new LinkedHashMap<>();

    private ListSelectionListener createAssignmentsListSelectionListener(JFrame frame, JList<Course> courseList, JList<CourseWork> assignmentsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> categorizedStudentsModel, List<Course> courses, Classroom service) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = assignmentsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String courseId = courses.get(courseList.getSelectedIndex()).getId();
                    String courseWorkId = assignmentsList.getSelectedValue().getId();

                    if (attachmentsViewer !=null) {
                        attachmentsViewer.hide(); // get rid of any existing viewer
                    }

                    // Set the cursor to wait cursor
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    // get count of students in the class, for the progress bar
                    try {
                        int numberOfStudents = getNumberOfStudents(service, courseId);
                        progressBar.setMaximum(numberOfStudents);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    // Show the progress bar
                    progressBar.setVisible(true);
                    frame.revalidate();
                    frame.repaint();

                    // Clear the categorizedStudentsModel before updating
                    categorizedStudentsModel.clear();

                    // Create worker object to fetch submissions in the background
                    FetchSubmissionsWorker worker = new FetchSubmissionsWorker(this, courseId, courseWorkId, studentAttachments);
                    worker.execute();
                }
            }
        };
    }

    private ListSelectionListener createCategorizedStudentsListSelectionListener(JList<String> categorizedStudentsList, JList<NamedStudentSubmission> studentSubmissionsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> submittedAttachmentsListModel) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = categorizedStudentsList.getSelectedIndex();
                if (selectedIndex != -1 && !studentSubmissionsListModel.isEmpty()) {
                    // remove the submission status text to give just the student name
                    String studentName = categorizedStudentsList.getSelectedValue().split(" - ")[0];
                    attachmentsViewer.setCurrentStudentByName(studentName);
                    attachmentsViewer.show();  // just in case the window has been closed

                    studentSubmissionsList.setSelectedIndex(selectedIndex);

                    // Clear the submittedAttachmentsListModel
                    submittedAttachmentsListModel.clear();

                    // Get the selected NamedStudentSubmission
                    NamedStudentSubmission selectedSubmission = studentSubmissionsListModel.get(selectedIndex);

                    // Iterate through the attachments and add their filenames to the submittedAttachmentsListModel
                    List<Attachment> attachments = selectedSubmission.getAttachments();
                    for (Attachment attachment : attachments) {
                        submittedAttachmentsListModel.addElement(attachment.getDriveFile().getTitle());
                    }
                }
            }
        };
    }


    private ListSelectionListener createStudentSubmissionsListSelectionListener(JFrame frame, JList<NamedStudentSubmission> studentSubmissionsList) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = studentSubmissionsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    NamedStudentSubmission selectedSubmission = studentSubmissionsList.getModel().getElementAt(selectedIndex);
                    List<Attachment> attachments = selectedSubmission.getAttachments();
                    if (!attachments.isEmpty() && attachments.get(0).getDriveFile() != null) {
                        String documentId = attachments.get(0).getDriveFile().getId(); // The ID of the Google Docs document from the assignment
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try {
                            driveService.files().export(documentId, "text/html").executeMediaAndDownloadTo(outputStream);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "The selected submission has no accessible attachments.", "No Attachments", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };
    }

    private int getNumberOfStudents(Classroom service, String courseId) throws IOException {
        ListStudentsResponse response = service.courses().students().list(courseId).execute();
        List<Student> students = response.getStudents();
        return (students != null) ? students.size() : 0;
    }

}

