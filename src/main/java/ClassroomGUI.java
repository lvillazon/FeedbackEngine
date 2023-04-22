import javax.swing.*;
import java.awt.*;
import java.util.List;
import com.google.api.services.classroom.model.Course;

public class ClassroomGUI {
    private JFrame frame;
    private JList<Course> coursesList;

    public ClassroomGUI(List<Course> courses) {
        initialize(courses);
    }
/*
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GoogleAuthenticator authenticator = new GoogleAuthenticator();
                com.google.api.services.classroom.Classroom service = authenticator.getClassroomService();
                List<Course> courses = GoogleClassroom.getCourses(service);
                ClassroomGUI window = new ClassroomGUI(courses);
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
*/
    private void initialize(List<Course> courses) {
        frame = new JFrame();
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        coursesList = new JList<>(new DefaultListModel<>());
        coursesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        coursesList.setLayoutOrientation(JList.VERTICAL);
        coursesList.setVisibleRowCount(-1);

        JScrollPane scrollPane = new JScrollPane(coursesList);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        for (Course course : courses) {
            ((DefaultListModel<Course>) coursesList.getModel()).addElement(course);
        }
    }
}
