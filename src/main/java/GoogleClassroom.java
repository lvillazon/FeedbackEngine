import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.StudentSubmission;

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

        // ... (the rest of the code remains the same)

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

