import com.google.api.services.classroom.Classroom;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.ClassroomScopes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class GoogleAuthenticator {
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    //private static final String[] SCOPES = {
    //        "https://www.googleapis.com/auth/classroom.courses.readonly",
    //        "https://www.googleapis.com/auth/classroom.coursework.students.readonly"
    //};
    private static final List<String> SCOPES = Arrays.asList(
            ClassroomScopes.CLASSROOM_COURSES_READONLY,
            ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY,
            ClassroomScopes.CLASSROOM_ROSTERS_READONLY);

    public Credential authenticate() throws IOException {
        InputStream in = GoogleAuthenticator.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Classroom getClassroomService() throws IOException {
        Credential credential = authenticate();
        return new Classroom.Builder(new NetHttpTransport(), JSON_FACTORY, credential)
                .setApplicationName("Google Classroom API Java Quickstart")
                .build();
    }
}
