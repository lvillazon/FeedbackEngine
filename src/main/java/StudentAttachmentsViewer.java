import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.drive.Drive;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.swing.*;

public class StudentAttachmentsViewer {
    private Drive driveService;
    private Map<String, String> studentAttachments;
    private JFrame docFrame;
    private JEditorPane editorPane;
    private int currentIndex;
    private boolean guiCreated;

    public StudentAttachmentsViewer(Drive driveService, Map<String, String> studentAttachments) {
        this.driveService = driveService;
        this.studentAttachments = studentAttachments;
        this.currentIndex = 0;
        this.guiCreated = false; // this flag prevents updates before the docFrame has been created

        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(getCurrentAttachmentHtml());

        JScrollPane scrollPane = new JScrollPane(editorPane);
        docFrame = new JFrame("Google Docs Document");
        docFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton backButton = new JButton("Back");
        JButton forwardButton = new JButton("Forward");

        backButton.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateDocument();
            }
        });

        forwardButton.addActionListener(e -> {
            if (currentIndex < studentAttachments.size() - 1) {
                currentIndex++;
                updateDocument();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);

        docFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        docFrame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        docFrame.setSize(800, 600);
        docFrame.setVisible(true);
        guiCreated = true;
    }

    private String getCurrentAttachmentHtml() {
        if (currentIndex < 0 || studentAttachments.isEmpty()) {
            return "";
        }

        String originalHtml = (String) studentAttachments.values().toArray()[currentIndex];
        /*
        the html from Google Docs contains escaped characters in the <meta> tag
        that don't render properly in a JEditor pane.
        To fix this for now, we remove the <meta> tag using a regex.
        If there are other tags that break JEditor, we may need to move to
        the JavaFX WebView component, which has better HTML/CS/JS support.
        */
        String cleanedHtml = originalHtml.replaceAll("<meta[^>]*>", "");
        return cleanedHtml;
    }

    private void updateDocument() {
        editorPane.setText(getCurrentAttachmentHtml());
        editorPane.revalidate();
        editorPane.repaint();
    }

    public void setStudentAttachments(Map<String, String> studentAttachments) {
        this.studentAttachments = studentAttachments;
    }

    public void refreshViewer() {
        if (guiCreated && !studentAttachments.isEmpty()) {
            currentIndex = 0;
            updateDocument();
        }
    }

}
