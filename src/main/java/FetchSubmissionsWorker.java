import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Attachment;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FetchSubmissionsWorker extends SwingWorker<List<NamedStudentSubmission>, Integer> {
    private GoogleClassroom googleClassroom;
    private String courseId;
    private String courseWorkId;
    private Map<String, String> studentAttachments;
    private DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel;

//    public FetchSubmissionsWorker(Classroom service, String courseId, String courseWorkId, Map<String, String> studentAttachments, DefaultListModel<NamedStudentSubmission> studentSubmissionsListModel) {
    public FetchSubmissionsWorker(GoogleClassroom gc, String courseId, String courseWorkId, Map<String, String> studentAttachments) {
        this.courseId = courseId;
        this.courseWorkId = courseWorkId;
        this.studentAttachments = studentAttachments;
        this.googleClassroom = gc;
        this.studentSubmissionsListModel = googleClassroom.studentSubmissionsListModel;
    }

    @Override
    protected List<NamedStudentSubmission> doInBackground() throws Exception {
        // Call the modified getStudentSubmissions method with 'this' as an argument
        return googleClassroom.getStudentSubmissions(courseId, courseWorkId, studentAttachments, this);
    }

    public void publishProgress(int progress) {
        this.publish(progress);
    }

    @Override
    protected void process(List<Integer> increments) {
        for (Integer progress : increments) {
            googleClassroom.progressBar.setValue(googleClassroom.progressBar.getValue() + progress);
        }
    }

    @Override
    protected void done() {
        List<NamedStudentSubmission> submissions = null;
        try {
            submissions = get();
            for (NamedStudentSubmission submission : submissions) {
                studentSubmissionsListModel.addElement(submission);
            }
            // Add the students with their submission status to the categorizedStudentsModel
            for (int i = 0; i < studentSubmissionsListModel.getSize(); i++) {
                NamedStudentSubmission submission = studentSubmissionsListModel.getElementAt(i);
                String studentName = submission.getStudentName();
                String submissionStatus = submission.getSubmission().getState();
                googleClassroom.categorizedStudentsModel.addElement(studentName + " - " + submissionStatus);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            googleClassroom.progressBar.setVisible(false);
            googleClassroom.progressBar.setValue(0);
            System.out.println("LOG: create viewer");
            //googleClassroom.attachmentsViewer = new StudentAttachmentsViewer(googleClassroom.driveService, new LinkedHashMap<>());
            googleClassroom.attachmentsViewer = new StudentAttachmentsViewer(googleClassroom.classroomService, googleClassroom.driveService, studentAttachments, googleClassroom.categorizedStudentsList, courseId, courseWorkId);
            System.out.println("LOG: " + studentAttachments.size() + " attachments");
            googleClassroom.attachmentsViewer.setStudentAttachments(studentAttachments, submissions);
            googleClassroom.attachmentsViewer.refreshViewer();
            // Set the cursor back to default cursor
            googleClassroom.frame.setCursor(Cursor.getDefaultCursor());
        }
    }

    public void processAttachments(List<Attachment> attachments, String studentName) {
        for (Attachment attachment : attachments) {
            if (attachment.getDriveFile() != null) {
                String fileId = attachment.getDriveFile().getId();
                // Store the file ID instead of file content
                studentAttachments.put(studentName, fileId);
            }
        }
    }


}
