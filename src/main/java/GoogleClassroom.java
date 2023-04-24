import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import com.google.api.services.drive.Drive;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public class GoogleClassroom {
    private Course selectedCourse;
    private Classroom classroomService;
    private Drive driveService;
    private List<CourseWork> assignments = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new GoogleClassroom().createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
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

        JFrame frame = new JFrame("Google Classroom Courses and Assignments");
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
        studentSubmissionsList.addListSelectionListener(createStudentSubmissionsListSelectionListener(frame, categorizedStudentsList, studentSubmissionsList));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }



    private List<Course> getCourses(Classroom service) throws IOException {
        List<Course> courses = service.courses().list().setPageSize(10).execute().getCourses();
        if (courses == null || courses.isEmpty()) {
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
        String nextPageToken = null;

        do {
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
                UserProfile studentProfile = service.userProfiles().get(studentId).execute();
                String studentName = studentProfile.getName().getFullName();
                NamedStudentSubmission namedSubmission = new NamedStudentSubmission(studentName, submission);
                submissions.add(namedSubmission);
            }

            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null);

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
                    try {
                        List<NamedStudentSubmission> studentSubmissions = getStudentSubmissions(service, courseId, courseWorkId);
                        System.out.println(studentSubmissions.size() + " submissions");
                        studentSubmissionsListModel.clear();
                        categorizedStudentsModel.clear();

                        Map<String, List<NamedStudentSubmission>> categorizedStudents = new LinkedHashMap<>();
                        categorizedStudents.put("TURNED_IN:", new ArrayList<>());
                        categorizedStudents.put("ASSIGNED:", new ArrayList<>());
                        categorizedStudents.put("NEW:", new ArrayList<>());
                        categorizedStudents.put("RETURNED:", new ArrayList<>());
                        categorizedStudents.put("NOT_SUBMITTED:", new ArrayList<>());

                        for (NamedStudentSubmission submission : studentSubmissions) {
                            String studentName = submission.getStudentName();
                            String status = submission.getStudentSubmission().getState();

                            switch (status) {
                                case "TURNED_IN":
                                    categorizedStudents.get("TURNED_IN:").add(submission);
                                    break;
                                case "CREATED":
                                    categorizedStudents.get("ASSIGNED:").add(submission);
                                    break;
                                case "NEW":
                                    categorizedStudents.get("NEW:").add(submission);
                                case "RETURNED":
                                    categorizedStudents.get("RETURNED:").add(submission);
                                default:
                                    categorizedStudents.get("NOT_SUBMITTED:").add(submission);
                                    break;
                            }
                        }

                        for (Map.Entry<String, List<NamedStudentSubmission>> entry : categorizedStudents.entrySet()) {
                            categorizedStudentsModel.addElement(entry.getKey());
                            for (NamedStudentSubmission namedSubmission : entry.getValue()) {
                                categorizedStudentsModel.addElement("  - " + namedSubmission.getStudentName());
                                studentSubmissionsListModel.addElement(namedSubmission);
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
//                        submittedAttachmentsListModel.addElement(attachment.getFilename());
                        submittedAttachmentsListModel.addElement(attachment.getDriveFile().getTitle());
                    }
                }
            }
        };
    }


    private ListSelectionListener createStudentSubmissionsListSelectionListener(JFrame frame, JList<String> categorizedStudentsList, JList<NamedStudentSubmission> studentSubmissionsList) {
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
                        String htmlContent = null;
                        try {
                            htmlContent = outputStream.toString(StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException ex) {
                            ex.printStackTrace();
                        }
                        displayDocumentText(htmlContent);
                    } else {
                        JOptionPane.showMessageDialog(frame, "The selected submission has no accessible attachments.", "No Attachments", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };
    }

    private void displayDocumentText(String htmlContent) {
        // Initialize the JFXPanel (needed for rendering JavaFX content in a Swing application)
        JFXPanel jfxPanel = new JFXPanel();

        // Create a new JFrame and add the JFXPanel to it
        JFrame docframe = new JFrame("Google Docs Document");
        docframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        docframe.getContentPane().add(jfxPanel);
        docframe.setSize(800, 600);
        docframe.setVisible(true);

        // Run the WebView code on the JavaFX Application Thread
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webView.getEngine().loadContent(htmlContent);

            jfxPanel.setScene(new Scene(webView));
        });
    }

    private String extractDocId(String fileURL) {
        Pattern pattern = Pattern.compile("(?<=/d/)[^/]+");
        Matcher matcher = pattern.matcher(fileURL);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    public void displayGoogleDoc(JFrame frame, String docId) {
        try {
            //Drive driveService = googleAuthenticator.getDriveService();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().export(docId, "text/plain")
                    .executeMediaAndDownloadTo(outputStream);
            displayText(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayPDF(byte[] pdfContent) {
        try {
            File tempFile = File.createTempFile("document", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), pdfContent);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                System.err.println("Desktop is not supported, cannot display the PDF.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayText(String textContent) {
        JTextArea textArea = new JTextArea(textContent);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        JOptionPane.showMessageDialog(null, scrollPane, "Document Content", JOptionPane.INFORMATION_MESSAGE);
    }


}

