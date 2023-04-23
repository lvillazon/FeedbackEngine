import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.StudentSubmission;

import java.util.ArrayList;
import java.util.List;

public class NamedStudentSubmission {
    private String studentName;
    private StudentSubmission studentSubmission;

    public NamedStudentSubmission(String studentName, StudentSubmission studentSubmission) {
        this.studentName = studentName;
        this.studentSubmission = studentSubmission;
    }

    public String getStudentName() {
        return studentName;
    }

    public StudentSubmission getStudentSubmission() {
        return studentSubmission;
    }

    public List<com.google.api.services.classroom.model.Attachment> getAttachments() {
        List<com.google.api.services.classroom.model.Attachment> attachments = new ArrayList<>();
        if (this.studentSubmission.getAssignmentSubmission() != null && this.studentSubmission.getAssignmentSubmission().getAttachments() != null) {
            attachments.addAll(this.studentSubmission.getAssignmentSubmission().getAttachments());
        }
        return attachments;
    }


}
