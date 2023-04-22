import com.google.api.services.classroom.model.StudentSubmission;

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
}
