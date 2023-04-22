import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.UserProfile;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoogleClassroom {
    private static Course selectedCourse;
    private static Classroom service;
    private static List<CourseWork> assignments;

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
        JList<NamedStudentSubmission> studentSubmissionsList = new JList<NamedStudentSubmission>(studentSubmissionsListModel);
        studentSubmissionsList.setCellRenderer(new StudentSubmissionCellRenderer());
        JScrollPane studentSubmissionScrollPane = new JScrollPane(studentSubmissionsList);

        JScrollPane studentSubmissionsScrollPane = new JScrollPane(studentSubmissionsList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, courseScrollPane, assignmentScrollPane);
        splitPane.setDividerLocation(400);

        courseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = courseList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String courseId = courses.get(selectedIndex).getId();
                    try {
                        List<com.google.api.services.classroom.model.CourseWork> assignments = getAssignments(service, courseId);
                        assignmentListModel.clear();
                        for (com.google.api.services.classroom.model.CourseWork assignment : assignments) {
                            assignmentListModel.addElement(assignment);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        assignmentsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = assignmentsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String courseId = courses.get(courseList.getSelectedIndex()).getId();
                    String courseWorkId = assignments.get(selectedIndex).getId();
                    try {
                        List<StudentSubmission> studentSubmissions = getStudentSubmissions(service, courseId, courseWorkId);
                        studentSubmissionsListModel.clear();
                        for (StudentSubmission submission : studentSubmissions) {
                            String studentId = submission.getUserId();
                            try {
                                UserProfile studentProfile = service.userProfiles().get(studentId).execute();
                                String studentName = studentProfile.getName().getFullName();
                                NamedStudentSubmission namedSubmission = new NamedStudentSubmission(studentName, submission);
                                studentSubmissionsListModel.addElement(namedSubmission);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });


        /*
        assignmentsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                com.google.api.services.classroom.model.CourseWork selectedAssignment = assignmentsList.getSelectedValue();
                if (selectedAssignment != null) {
                    try {
                        String courseId = courseList.getSelectedValue().getId();
                        List<StudentSubmission> submissions = getStudentSubmissions(service, courseId, selectedAssignment.getId());
                        DefaultListModel<String> studentSubmissionsModel = new DefaultListModel<>();
                        Map<String, List<String>> categorizedStudents = new LinkedHashMap<>();
                        categorizedStudents.put("TURNED_IN:", new ArrayList<>());
                        categorizedStudents.put("ASSIGNED:", new ArrayList<>());
                        categorizedStudents.put("NOT_SUBMITTED:", new ArrayList<>());

                        for (StudentSubmission submission : submissions) {
                            String studentId = submission.getUserId();
                            Student student = service.courses().students().get(courseId, studentId).execute();
                            String studentName = student.getProfile().getName().getFullName();
                            String status = submission.getState();

                            switch (status) {
                                case "TURNED_IN":
                                    categorizedStudents.get("TURNED_IN:").add(studentName);
                                    break;
                                case "CREATED":
                                    categorizedStudents.get("ASSIGNED:").add(studentName);
                                    break;
                                case "NEW":
                                default:
                                    categorizedStudents.get("NOT_SUBMITTED:").add(studentName);
                                    break;
                            }
                        }

                        for (Map.Entry<String, List<String>> entry : categorizedStudents.entrySet()) {
                            studentSubmissionsModel.addElement(entry.getKey());
                            for (String student : entry.getValue()) {
                                studentSubmissionsModel.addElement("  - " + student);
                            }
                        }

                        for (StudentSubmission submission : studentSubmissions) {
                            String studentId = submission.getUserId();
                            try {
                                User studentProfile = service.users().get(studentId).execute();
                                String studentName = studentProfile.getName().getFullName();
                                NamedStudentSubmission namedSubmission = new NamedStudentSubmission(studentName, submission);
                                studentSubmissionsListModel.addElement(namedSubmission);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });

         */

        studentSubmissionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = studentSubmissionsList.getSelectedIndex();
                if (selectedIndex != -1) {
                    StudentSubmission selectedSubmission = studentSubmissionsListModel.getElementAt(selectedIndex);
                    new SubmissionDetailsFrame(selectedSubmission);
                }
            }
        });


        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitPane, BorderLayout.CENTER);

        frame.getContentPane().add(studentSubmissionsScrollPane, BorderLayout.EAST);


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

    private static List<StudentSubmission> getStudentSubmissions(Classroom service, String courseId, String courseWorkId) throws IOException {
        List<StudentSubmission> submissions = new ArrayList<>();
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
            submissions.addAll(response.getStudentSubmissions());
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null);

        return submissions;
    }
}

