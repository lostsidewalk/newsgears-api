package com.lostsidewalk.buffy.app.recommendation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.lostsidewalk.buffy.discovery.FeedRecommendationInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;


@Slf4j
@Service
public class FeedRecommendationService {

    private static final Gson GSON = new Gson();

    @Value("${recommendation.service.url}")
    private String openAiUrl;

    @Value("${recommendation.service.api.key}")
    private String openAiApiKey;

    @Value("${recommendation.service.default-model}")
    private String defaultModel;

    @Value("${recommendation.service.prompt-query-template}")
    private String promptQueryTemplate;

    @Value("${recommendation.service.default-tokens-per-query}")
    private Integer defaultTokensPerQuery;

    @Value("${recommendation.service.default-query-temp}")
    private Double defaultQueryTemp;

    @Cacheable(value="feedRecommendationCache")
    public FeedRecommendationInfo recommendSimilarFeeds(String feedUrl) {
        try {
            HttpURLConnection urlConnection = openConnection();
            //
            String prompt = String.format(this.promptQueryTemplate, feedUrl);
            String jsonInputString = GSON.toJson(OpenAiRequest.from(this.defaultModel, prompt, this.defaultTokensPerQuery, this.defaultQueryTemp));
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(UTF_8);
                os.write(input, 0, input.length);
            }
            //
            int responseCode = urlConnection.getResponseCode();
            log.info("Recommendation engine responseCode={}", responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // TODO: change to debug when ready
            log.info("Recommendation engine raw response={}", response);
            FeedRecommendationInfo parsedResponse = parseResponse(response.toString());
            log.info("Recommendation engine response URLs={}", parsedResponse);
            return parsedResponse;
        } catch (Exception e) {
            log.error("Recommendation engine failed due to: {}", e.getMessage());
            // throw new RecommendationException(feedUrl, e);
        }

        return null;
    }

    private HttpURLConnection openConnection() throws IOException {
        URL recommenderUrl = new URL(this.openAiUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) recommenderUrl.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Authorization", "Bearer " + this.openAiApiKey);
        urlConnection.setReadTimeout(20 * 1_000);
        return urlConnection;
    }

    @Data
    static class OpenAiRequest {

        final String model;

        final String prompt;

        @SerializedName("max_tokens")
        final Integer maxTokens;

        final Double temperature;

        public static OpenAiRequest from(String model, String prompt, Integer maxTokens, Double temperature) {
            return new OpenAiRequest(model, prompt, maxTokens, temperature);
        }
    }

    private static FeedRecommendationInfo parseResponse(String response) {
        // parse the top-level object into JSON
        JsonObject jsonObject = GSON.fromJson(response, JsonObject.class);
        // check for 'choices' element..
        if (jsonObject.has("choices")) {
            JsonElement choicesElem = jsonObject.getAsJsonArray("choices");
            // ..that is an array
            if (choicesElem.isJsonArray()) {
                JsonArray choicesArr = choicesElem.getAsJsonArray();
                // ..that is not empty
                if (!choicesArr.isEmpty()) {
                    // grab the first element
                    JsonElement choiceElem = choicesArr.get(0);
                    // ..make sure it's an object
                    if (choiceElem.isJsonObject()) {
                        JsonObject choiceObj = choiceElem.getAsJsonObject();
                        // ..that has a 'text' property
                        if (choiceObj.has("text")) {
                            JsonElement textElem = choiceObj.get("text");
                            // ..that is a string
                            if (textElem.isJsonPrimitive()) {
                                // create the recommendation info from that string
                                List<String> recommendedUrls = extractUrls(textElem.getAsString());
                                return FeedRecommendationInfo.from(recommendedUrls);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static List<String> extractUrls(String recommendation) {
        List<String> extractedUrls = new ArrayList<>();
        String[] parts = StringUtils.split(recommendation, '\n');
        for (String p : parts) {
            if (isNotBlank(p)) {
                int protocolStartIdx = p.indexOf("http");
                if (protocolStartIdx >= 0) {
                    extractedUrls.add(trim(p.substring(protocolStartIdx)));
                }
            }
        }

        return extractedUrls;
    }
}
