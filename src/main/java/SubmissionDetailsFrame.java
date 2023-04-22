import com.google.api.services.classroom.model.StudentSubmission;

import javax.swing.*;
import java.awt.*;

public class SubmissionDetailsFrame extends JFrame {
    public SubmissionDetailsFrame(StudentSubmission submission) {
        setTitle("Submission Details");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        JTextArea detailsTextArea = new JTextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setLineWrap(true);
        detailsTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(detailsTextArea);
        add(scrollPane, BorderLayout.CENTER);

        detailsTextArea.append("Student ID: " + submission.getUserId() + "\n");
        detailsTextArea.append("Submission ID: " + submission.getId() + "\n");
        detailsTextArea.append("Submission Status: " + submission.getState() + "\n");

        setVisible(true);
    }
}
