import com.google.api.services.classroom.model.Course;

import javax.swing.*;
import java.awt.*;

public class CourseCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Course) {
            Course course = (Course) value;
            value = course.getName();
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
