import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ChatGPT {
    private static final String BASE_URL = "https://api.openai.com";
    private static final String VERSION = "v1";
    private static final String MODEL = "gpt-4";
    private static final String API_KEY;

    // load the API key from a file (which is not synced to Git)
    static {
        String apiKey = null;
        try {
            InputStream inputStream = ChatGPT.class.getResourceAsStream("/api_key.txt");
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    apiKey = reader.readLine().trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load API key from file.");
        }
        API_KEY = apiKey;
    }

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public String getGeneratedText(String input) {
        String generatedText = "";

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    (HttpRequest request) -> {
                        request.setParser(JSON_FACTORY.createJsonObjectParser());
                        request.getHeaders().setAuthorization("Bearer " + API_KEY);
                    });

            GenericUrl url = new GenericUrl(BASE_URL + "/" + VERSION + "/engines/" + MODEL + "/completions");
            Map<String, Object> data = new HashMap<>();
            data.put("prompt", input);
            data.put("max_tokens", 8000); // Set the max tokens or adjust according to your needs

            HttpContent content = new UrlEncodedContent(data);
            HttpRequest request = requestFactory.buildPostRequest(url, content);
            HttpResponse response = request.execute();

            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = response.parseAs(Map.class);
            generatedText = (String) responseData.get("choices");
        } catch (IOException e) {
            System.err.println("An error occurred while trying to connect to the OpenAI API:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred:");
            e.printStackTrace();
        }

        return generatedText;
    }
}
