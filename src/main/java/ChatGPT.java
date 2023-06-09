import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.Json;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.ArrayMap;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatGPT {
    private static final String BASE_URL = "https://api.openai.com";
    private static final String VERSION = "v1";
    private static final String MODEL = "gpt-4"; //gpt-3.5-turbo";
    private static final String API_KEY;

    // load the API key from a file (which is not synced to Git)
    static {
        String apiKey = null;
        try {
            InputStream inputStream = ChatGPT.class.getResourceAsStream("/chatGPT_api_key.txt");
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

    public String removeHtmlTags(String htmlString) {
        // using a regex isn't smart enough to properly clean the code of all the HTML
        // which causes input strings that are wildly over the 8000 token limit
        // return htmlString.replaceAll("\\<.*?\\>", "");
        // This attempts to parse it more cleverly
        return Jsoup.parse(htmlString).text();
    }

    public String getGeneratedText(String input) {
        String plainText = removeHtmlTags(input);
        System.out.println("Calling ChatGPT with the following input:");
        System.out.println(plainText);
        System.out.println("+++ end of input +++");
        String generatedText = "";
        String testlink = "Mark this work: https://docs.google.com/document/d/1saLgGsGxaH8MecGPbdZXbK3ddR2QDGoQ8RhDvyzl5Os/edit#heading=h.gjdgxs";

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    (HttpRequest request) -> {
                        request.setParser(JSON_FACTORY.createJsonObjectParser());
                        request.getHeaders().setAuthorization("Bearer " + API_KEY);
                    });

            GenericUrl url = new GenericUrl(BASE_URL + "/" + VERSION + "/chat/completions");

            List<Map<String, String>> messages = Arrays.asList(
                    new HashMap<String, String>() {{ put("role", "system"); put("content", "You are a helpful assistant."); }},
                    new HashMap<String, String>() {{ put("role", "user"); put("content", testlink); }}
            );
            Map<String, Object> data = new HashMap<>();
            data.put("model", MODEL);
            data.put("messages", messages);
            data.put("plugin", "link reader"); // this activates the Link Reader plugin for the current session

            HttpContent content = new JsonHttpContent(JSON_FACTORY, data);
            HttpRequest request = requestFactory.buildPostRequest(url, content);
            HttpResponse response = request.execute();

            GenericJson responseData = response.parseAs(GenericJson.class);
            List<Object> choices = (List<Object>) responseData.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choiceMap = (Map<String, Object>) choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choiceMap.get("message");
                generatedText = (String) message.get("content");
            }

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
