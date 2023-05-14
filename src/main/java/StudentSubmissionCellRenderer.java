import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import com.google.api.services.classroom.model.StudentSubmission;

public class StudentSubmissionCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof NamedStudentSubmission) {
            NamedStudentSubmission namedStudentSubmission = (NamedStudentSubmission) value;
            String studentName = namedStudentSubmission.getStudentName();
            String assignmentStatus = namedStudentSubmission.getSubmission().getState();
            String displayText = studentName + " - " + assignmentStatus;
            return super.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus);
        } else {
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
