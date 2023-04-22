import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.ListCoursesResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleClassroomExample {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        Credential credential = authenticator.authenticate();

        Classroom service = new Classroom.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName("Google Classroom API Example")
                .build();

        listCourses(service);
    }

    private static void listCourses(Classroom service) throws IOException {
        ListCoursesResponse response = service.courses().list().setPageSize(10).execute();
        List<Course> courses = response.getCourses();
        if (courses == null || courses.isEmpty()) {
            System.out.println("No courses found.");
        } else {
            System.out.println("Courses:");
            for (Course course : courses) {
                System.out.printf("%s (%s)\n", course.getName(), course.getId());
            }
        }
    }
}
