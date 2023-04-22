import javax.swing.*;
import java.awt.*;

class CourseWorkCellRenderer extends JLabel implements ListCellRenderer<com.google.api.services.classroom.model.CourseWork> {
    @Override
    public Component getListCellRendererComponent(JList<? extends com.google.api.services.classroom.model.CourseWork> list, com.google.api.services.classroom.model.CourseWork value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null) {
            setText(value.getTitle());
        } else {
            setText("");
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);

        return this;
    }
}
