import javax.swing.*;
import java.awt.*;

public class SubmissionDetailsFrame extends JFrame {
    public SubmissionDetailsFrame(NamedStudentSubmission submission) {
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

        detailsTextArea.append("Student Name: " + submission.getStudentName() + "\n");
        detailsTextArea.append("Submission ID: " + submission.getStudentSubmission().getId() + "\n");
        detailsTextArea.append("Submission Status: " + submission.getStudentSubmission().getState() + "\n");

        setVisible(true);
    }
}
