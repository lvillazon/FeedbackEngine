import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoogleClassroom {
    private static Course selectedCourse;
    private static Classroom service;
    private static List<CourseWork> assignments = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void createAndShowGUI() throws IOException {
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        Classroom service = authenticator.getClassroomService();
        List<Course> courses = getCourses(service);

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
        courseList.addListSelectionListener(createCourseListSelectionListener(frame, courseList, assignmentListModel, courses, service));
        assignmentsList.addListSelectionListener(createAssignmentsListSelectionListener(frame, courseList, assignmentsList, studentSubmissionsListModel, categorizedStudentsModel, courses, service));
        categorizedStudentsList.addListSelectionListener(createCategorizedStudentsListSelectionListener(categorizedStudentsList, studentSubmissionsList, studentSubmissionsListModel, submittedAttachmentsListModel));
        studentSubmissionsList.addListSelectionListener(createStudentSubmissionsListSelectionListener(frame, categorizedStudentsList, studentSubmissionsList));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }



    private static List<Course> getCourses(Classroom service) throws IOException {
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

    private static List<CourseWork> getAssignments(Classroom service, String courseId) throws IOException {
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

    private static List<NamedStudentSubmission> getStudentSubmissions(Classroom service, String courseId, String courseWorkId) throws IOException {
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

    private static ListSelectionListener createCourseListSelectionListener(JFrame frame, JList<Course> courseList, DefaultListModel<CourseWork> assignmentListModel, List<Course> courses, Classroom service) {
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

    private static ListSelectionListener createAssignmentsListSelectionListener(JFrame frame, JList<Course> courseList, JList<CourseWork> assignmentsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> categorizedStudentsModel, List<Course> courses, Classroom service) {
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


    private static ListSelectionListener createCategorizedStudentsListSelectionListener(JList<String> categorizedStudentsList, JList<NamedStudentSubmission> studentSubmissionsList, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel, DefaultListModel<String> submittedAttachmentsListModel) {
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


    private static ListSelectionListener createStudentSubmissionsListSelectionListener(JFrame frame, JList<String> categorizedStudentsList, JList<NamedStudentSubmission> studentSubmissionsList) {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = studentSubmissionsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    NamedStudentSubmission selectedSubmission = studentSubmissionsList.getModel().getElementAt(selectedIndex);
                    List<Attachment> attachments = selectedSubmission.getAttachments();
                    if (!attachments.isEmpty() && attachments.get(0).getDriveFile() != null) {
                        String fileId = attachments.get(0).getDriveFile().getId();
                        String fileUrl = "https://drive.google.com/file/d/" + fileId;
                        displayGoogleDoc(frame, fileUrl);
                    } else {
                        JOptionPane.showMessageDialog(frame, "The selected submission has no accessible attachments.", "No Attachments", JOptionPane.INFORMATION_MESSAGE);
                    }
               }
            }
        };
    }




    private static void displayGoogleDoc(JFrame parentFrame, String fileUrl) {
        JDialog dialog = new JDialog(parentFrame, "Google Doc", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parentFrame);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);

        try {
            editorPane.setPage(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            editorPane.setContentType("text/html");
            editorPane.setText("<html><body><h1>Error loading Google Doc</h1><p>Unable to load the document.</p></body></html>");
        }

        JScrollPane scrollPane = new JScrollPane(editorPane);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

}

