import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Comment;
//import com.google.api.services.drive.model.Location;
import com.google.api.services.drive.model.File;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.*;
import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.StudentSubmission;


public class StudentAttachmentsViewer {
    private Drive driveService;
    private Map<String, String> studentAttachments;
    private List<NamedStudentSubmission> namedStudentSubmissions;
    private JFrame docFrame;
    private JEditorPane editorPane;
    private int currentIndex;
    private boolean guiCreated;
    private JLabel studentInfoLabel;
    private JList<String> categorizedStudentsList;  // link to the list of students on the GoogleClassroom form
    private JTextArea feedbackTextArea;
    private JButton saveButton;
    private List<String> feedbackList;
    private Classroom classroomService;
    private String courseId;
    private String courseWorkId;

    public StudentAttachmentsViewer(Classroom classroomService, Drive driveService, Map<String, String> studentAttachments, JList<String> categorizedStudentsList, String courseId, String courseWorkId) {
        this.classroomService = classroomService;
        this.driveService = driveService;
        this.studentAttachments = studentAttachments;
        this.currentIndex = 0;
        this.guiCreated = false; // this flag prevents updates before the docFrame has been created
        this.categorizedStudentsList = categorizedStudentsList;
        this.feedbackList = new ArrayList<>(Collections.nCopies(studentAttachments.size(), ""));
        this.courseId = courseId;
        this.courseWorkId = courseWorkId;
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
        docFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        JButton backButton = new JButton("Back");
        JButton forwardButton = new JButton("Forward");

        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex < studentAttachments.size() - 1) {
                    // Set the cursor to wait cursor
                    docFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    currentIndex++;
                    updateDocument();

                    // Restore the cursor
                    docFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex > 0) {
                    // Set the cursor to wait cursor
                    docFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    currentIndex--;
                    updateDocument();

                    // Restore the cursor
                    docFrame.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        studentInfoLabel = new JLabel();
        studentInfoLabel.setHorizontalAlignment(JLabel.CENTER);
        docFrame.getContentPane().add(studentInfoLabel, BorderLayout.NORTH);

        feedbackTextArea = new JTextArea();
        feedbackTextArea.setLineWrap(true);
        feedbackTextArea.setWrapStyleWord(true);
        JScrollPane feedbackScrollPane = new JScrollPane(feedbackTextArea);
        feedbackScrollPane.setPreferredSize(new Dimension(300, 600));

        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String feedbackText = feedbackTextArea.getText();
            saveFeedback(feedbackText);
        });


        JPanel buttonPanel = new JPanel();
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(saveButton);

        JButton markButton = new JButton("Mark");
        buttonPanel.add(markButton);

        markButton.addActionListener(e -> {
            // Get the text of the current student attachment
            String studentAttachmentText = getCurrentAttachmentHtml();
            // Get the mark scheme (hardcoded for now)
            String markScheme = getMarkScheme();

            // Combine the student work and the mark scheme
            String combinedText = studentAttachmentText + "\n\n" + markScheme;

            // Call the OpenAI API with the combined text
            ChatGPT chatGPT = new ChatGPT();
            combinedText = "Tell me a joke about Elvis."; // TODO Placeholder just to check input text limit is not an issue
            String apiResponse = chatGPT.getGeneratedText(combinedText);

            // Paste the API response into the feedbackTextArea
            feedbackTextArea.setText(apiResponse);
        });


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, feedbackScrollPane);

        docFrame.getContentPane().add(splitPane, BorderLayout.CENTER);
        docFrame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        docFrame.setSize(1100, 600);
        docFrame.setVisible(true);
        guiCreated = true;
        updateDocument();
    }

    private String getCurrentAttachmentHtml() {
        if (currentIndex < 0 || studentAttachments.isEmpty()) {
            return "";
        }

        String fileId = (String) studentAttachments.values().toArray()[currentIndex];
        String originalHtml = fetchFileContent(fileId);

        String cleanedHtml = originalHtml.replaceAll("<meta[^>]*>", "");
        return cleanedHtml;
    }

    private void updateDocument() {
        editorPane.setText(getCurrentAttachmentHtml());
        editorPane.revalidate();
        editorPane.repaint();

        if (!namedStudentSubmissions.isEmpty()) {
            NamedStudentSubmission submission = namedStudentSubmissions.get(currentIndex);
            String studentName = submission.getStudentName();
            String submissionStatus = submission.getSubmission().getState();
            String submissionDate = submission.getSubmission().getUpdateTime();
            studentInfoLabel.setText(String.format("Student: %s | Status: %s | Date: %s", studentName, submissionStatus, submissionDate));
        }

        // fetch the feedback for this student from the local list
        if (currentIndex >= 0 && currentIndex < feedbackList.size()) {
            feedbackTextArea.setText(feedbackList.get(currentIndex));
        }

        // update the selected item on the categorised students list to match this assignment submission
        if (categorizedStudentsList != null) {
            categorizedStudentsList.setSelectedIndex(currentIndex);
        }
    }

    public void setStudentAttachments(Map<String, String> studentAttachments, List<NamedStudentSubmission> namedStudentSubmissions) {
        this.studentAttachments = studentAttachments;
        this.namedStudentSubmissions = namedStudentSubmissions;
    }

    public void refreshViewer() {
        if (guiCreated && !studentAttachments.isEmpty()) {
            currentIndex = 0;
            updateDocument();
        }
    }

    public void setCurrentStudentByName(String studentName) {
        int index = new ArrayList<>(studentAttachments.keySet()).indexOf(studentName);
        if (index >= 0) {
            currentIndex = index;
            updateDocument();
        }
    }

    public void show() {
        docFrame.setVisible(true);
    }

    public void hide() {
        docFrame.setVisible(false);
    }

    private String getCurrentFileId() {
        if (currentIndex < 0 || studentAttachments.isEmpty()) {
            return null;
        }
//        return (String) studentAttachments.keySet().toArray()[currentIndex];
        return (String) studentAttachments.values().toArray()[currentIndex];
    }

    private void saveFeedback(String feedback) {
        if (!feedback.trim().isEmpty()) {
            try {
                String fileId = getCurrentFileId();
                if (fileId != null) {
                    Comment commentContent = new Comment();
                    commentContent.setContent(feedback);

                    Comment savedComment = driveService.comments().create(fileId, commentContent).execute();
                    JOptionPane.showMessageDialog(docFrame, "Feedback saved successfully.", "Feedback Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(docFrame, "Error saving feedback. Please try again.", "Error Saving Feedback", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(docFrame, "Error saving feedback. Please try again.", "Error Saving Feedback", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(docFrame, "Please enter feedback before saving.", "No Feedback Entered", JOptionPane.WARNING_MESSAGE);
        }
    }


    private void addCommentToDocument(Drive driveService, String fileId, String content) throws IOException {
        Comment commentContent = new Comment().setContent(content);

        driveService.comments().create(fileId, commentContent).execute();
    }

    private String fetchFileContent(String fileId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            driveService.files().export(fileId, "text/html").executeMediaAndDownloadTo(outputStream);
            String htmlContent = outputStream.toString(StandardCharsets.UTF_8.name());
            return htmlContent;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String getMarkScheme() {
        return "You are an ChatBot optimised to provide useful, constructive feedback for Computer Science students. Evaluate the work listed below:";
    }

}
