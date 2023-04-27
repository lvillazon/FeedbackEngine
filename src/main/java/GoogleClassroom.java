import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import com.google.api.services.drive.Drive;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class GoogleClassroom {
    private final Classroom classroomService;
    private final Drive driveService;
//    private final List<CourseWork> assignments = new ArrayList<>();
    private ProgressDialog  progress;
    private JFrame frame;

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
    }

    private void createAndShowGUI() throws IOException {
        List<Course> courses = getCourses(classroomService);

        frame = new JFrame("Google Classroom Courses and Assignments");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        progress = new ProgressDialog(frame, "Progress", "thinking...");

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

        DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel = new DefaultListModel<>();
        JList<NamedStudentSubmission> studentSubmissionsList = new JList<>(studentSubmissionsListModel);
        studentSubmissionsList.setCellRenderer(new StudentSubmissionCellRenderer());

        JSplitPane courseAndAssignmentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, courseScrollPane, assignmentScrollPane);
        courseAndAssignmentSplitPane.setDividerLocation(400);

        DefaultListModel<String> categorizedStudentsModel = new DefaultListModel<>();
        JList<String> categorizedStudentsList = new JList<>(categorizedStudentsModel);
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

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

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

    private List<NamedStudentSubmission> getStudentSubmissions(Classroom service, String courseId, String courseWorkId) throws IOException {
        List<NamedStudentSubmission> submissions = new ArrayList<>();
        AtomicInteger progressValue = new AtomicInteger(0);
        Semaphore semaphore = new Semaphore(0); // Initialize the semaphore with 0 permits

        // Show the progress dialog on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            progress.setModal(false);
            progress.setLocationRelativeTo(frame);
            progress.setVisible(true);
        });

        Thread fetchSubmissionsThread = new Thread(() -> {
            String nextPageToken = null;
            do {
                System.out.println("LOG: getting submission page...");
                try {
                    com.google.api.services.classroom.model.ListStudentSubmissionsResponse response = service
                            .courses()
                            .courseWork()
                            .studentSubmissions()
                            .list(courseId, courseWorkId)
                            .setPageSize(10)
                            .setPageToken(nextPageToken)
                            .execute();

                    for (StudentSubmission submission : response.getStudentSubmissions()) {
                        String studentId = submission.getUserId();
                        System.out.println("LOG: getting " + studentId);
                        UserProfile studentProfile = service.userProfiles().get(studentId).execute();
                        String studentName = studentProfile.getName().getFullName();
                        NamedStudentSubmission namedSubmission = new NamedStudentSubmission(studentName, submission);
                        submissions.add(namedSubmission);
                        progressValue.incrementAndGet();

                        SwingUtilities.invokeLater(() -> {
                            progress.setValue(progressValue.get());
                            semaphore.release(); // Release a permit after updating the progress bar
                        });

                        try {
                            semaphore.acquire(); // Wait for a permit, blocking the loop until the progress bar is updated
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    nextPageToken = response.getNextPageToken();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (nextPageToken != null);

            SwingUtilities.invokeLater(() -> {
                progress.setValue(progress.getMaximum());
                progress.setVisible(false);
                frame.setCursor(Cursor.getDefaultCursor());
            });
        });

        fetchSubmissionsThread.start();

        // Wait for the fetchSubmissionsThread to finish
        try {
            fetchSubmissionsThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
                        for (com.google.api.services.classroom.model.CourseWork assignment : assignments) {
                            assignmentListModel.addElement(assignment);
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

    private ListSelectionListener createAssignmentsListSelectionListener(JFrame frame, JList<Course> courseList, JList<CourseWork> assignmentsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> categorizedStudentsModel, List<Course> courses, Classroom service) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = assignmentsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String courseId = courses.get(courseList.getSelectedIndex()).getId();
                    String courseWorkId = assignmentsList.getSelectedValue().getId();

                    // Set the cursor to wait cursor
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    // Show the progress dialog
                    //SwingUtilities.invokeLater(() -> {
                        // Show the progress dialog
                        //progress.setModal(false);
                        //progress.setLocationRelativeTo(frame);
                        //progress.setVisible(true);
                    //});

                    System.out.println("LOG: create viewer");
                    StudentAttachmentsViewer attachmentsViewer = new StudentAttachmentsViewer(driveService, new LinkedHashMap<>());
                    try {
                        System.out.println("LOG: get submissions");
                        List<NamedStudentSubmission> studentSubmissions = getStudentSubmissions(service, courseId, courseWorkId);
                        System.out.println(studentSubmissions.size() + " submissions");
                        studentSubmissionsListModel.clear();
                        categorizedStudentsModel.clear();

                        // ... (rest of the method)

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        // Set the cursor back to default cursor
                        frame.setCursor(Cursor.getDefaultCursor());

                        // Hide the progress dialog
                        SwingUtilities.invokeLater(() -> {
                            progress.setVisible(false);
                        });
                    }
                }
            }
        };
    }

    private ListSelectionListener createCategorizedStudentsListSelectionListener(JList<String> categorizedStudentsList, JList<NamedStudentSubmission> studentSubmissionsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> submittedAttachmentsListModel) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = categorizedStudentsList.getSelectedIndex();
                if (selectedIndex != -1 && !studentSubmissionsListModel.isEmpty()) {
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
//                        String htmlContent = null;
//                        try {
//                            htmlContent = outputStream.toString(StandardCharsets.UTF_8.name());
//                        } catch (UnsupportedEncodingException ex) {
//                            ex.printStackTrace();
//                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "The selected submission has no accessible attachments.", "No Attachments", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };
    }

    private Map<String, String> fetchAllStudentAttachments(java.util.List<NamedStudentSubmission> studentSubmissions) {
        Map<String, String> studentAttachments = new LinkedHashMap<>();

        for (NamedStudentSubmission submission : studentSubmissions) {
            System.out.println("LOG: fetching...");
            List<Attachment> attachments = submission.getAttachments();
            if (!attachments.isEmpty() && attachments.get(0).getDriveFile() != null) {
                String documentId = attachments.get(0).getDriveFile().getId();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    driveService.files().export(documentId, "text/html").executeMediaAndDownloadTo(outputStream);
                    String htmlContent = outputStream.toString(StandardCharsets.UTF_8.name());
                    studentAttachments.put(submission.getStudentName(), htmlContent);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return studentAttachments;
    }

}

