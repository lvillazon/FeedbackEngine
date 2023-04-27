import java.util.List;

public interface SubmissionFetchCallback {
    void onSubmissionsFetched(List<NamedStudentSubmission> submissions);
}
