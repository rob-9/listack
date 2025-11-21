package com.leinterview.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${interview.demo-mode:true}")
    private boolean demoMode;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Random random = new Random();

    public String getGeminiResponse(String prompt) {
        if (demoMode || apiKey == null || apiKey.isEmpty() || "demo".equals(apiKey)) {
            return getDemoResponse();
        }

        try {
            String feedbackContext = """
                You are helping a friend with a mock interview. The following text is part of a mock interview.
                Your task is to provide really short friendly feedback on how your friend can improve their speaking.
                Start feedback with a score between 0-9. E.g. "8; <feedback>"
                Please calibrate your score so that the 'average' person interview will score 5.
                """;

            String fullPrompt = feedbackContext + "\n" + prompt;

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", fullPrompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + apiKey)
                    .post(RequestBody.create(gson.toJson(requestBody), JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Gemini API error: {}", response.code());
                    return "Error communicating with Gemini API: " + response.code();
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("candidates")) {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        JsonArray partsArray = contentObj.getAsJsonArray("parts");
                        if (partsArray.size() > 0) {
                            return partsArray.get(0).getAsJsonObject().get("text").getAsString();
                        }
                    }
                }

                return "Error parsing Gemini response";
            }
        } catch (IOException e) {
            log.error("Error calling Gemini API", e);
            return "Error communicating with Gemini API: " + e.getMessage();
        }
    }

    public String getResponseFromGemini(String problemName) {
        if (demoMode || apiKey == null || apiKey.isEmpty() || "demo".equals(apiKey)) {
            return getDemoProblemResponse(problemName);
        }

        try {
            String prompt = "Very briefly display the problem " + problemName + " from leetcode and one sample output";

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + apiKey)
                    .post(RequestBody.create(gson.toJson(requestBody), JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "Error: " + response.code();
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("candidates")) {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        JsonArray partsArray = contentObj.getAsJsonArray("parts");
                        if (partsArray.size() > 0) {
                            return partsArray.get(0).getAsJsonObject().get("text").getAsString();
                        }
                    }
                }

                return "Error parsing response";
            }
        } catch (IOException e) {
            log.error("Error calling Gemini API", e);
            return "Error: " + e.getMessage();
        }
    }

    private String getDemoResponse() {
        int[] scores = {6, 7, 8, 5, 9};
        String[] feedbacks = {
                "Great enthusiasm! Try to speak a bit slower for clarity.",
                "Good technical knowledge. Work on providing more specific examples.",
                "Excellent structure in your answer. Could use more confident delivery.",
                "Nice problem-solving approach. Remember to explain your thought process.",
                "Strong communication skills. Try to be more concise in your explanations."
        };

        int score = scores[random.nextInt(scores.length)];
        String feedback = feedbacks[random.nextInt(feedbacks.length)];

        return score + "; " + feedback + " (Demo Mode - Get real AI feedback by setting GEMINI_API_KEY)";
    }

    private String getDemoProblemResponse(String problemName) {
        return String.format("""
                **%s** (Demo Mode)

                Given an array of integers, return indices of two numbers that add up to a target.

                **Example:**
                - Input: nums = [2,7,11,15], target = 9
                - Output: [0,1] (because nums[0] + nums[1] = 2 + 7 = 9)

                *This is a demo response. Set GEMINI_API_KEY for real questions.*
                """, problemName);
    }
}
