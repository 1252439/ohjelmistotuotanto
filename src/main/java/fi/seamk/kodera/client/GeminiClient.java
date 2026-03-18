package fi.seamk.kodera.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class GeminiClient {
    
    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String configuredModel;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateAnalysis(String prompt) {
        if (!hasConfiguredApiKey()) {
            return "DEMO: Gemini API key not configured. Analysis: Code submission received and status is pending manual review.";
        }
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // Add content
            requestBody.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);
            
            // Add generation config to prevent empty responses
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            // Keep output as deterministic as possible for stable grading.
            generationConfig.put("temperature", 0.0);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 4096);

            List<String> modelsToTry = buildModelCandidates();

            String lastError = "";
            String errorDetails = "";
            String parsedErrorMessage = "";

            for (String model : modelsToTry.stream().distinct().toList()) {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return extractAnalysisFromResponse(response.body());
                }

                errorDetails = response.body();
                lastError = "status " + response.statusCode() + " (model=" + model + ")";
                parsedErrorMessage = extractApiErrorMessage(errorDetails);

                // 404: malli ei saatavilla, 429: quota/rate limit, 503: tilapäinen häiriö.
                // Näissä kokeillaan vielä seuraavaa mallia.
                // Retry on model-not-found style 400 errors with next model.
                boolean retryableModel400 = response.statusCode() == 400 && isLikelyModelAvailabilityError(parsedErrorMessage);

                if (response.statusCode() != 404 && response.statusCode() != 429 && response.statusCode() != 503 && !retryableModel400) {
                    // Log the actual error for debugging
                    System.err.println("Gemini API Error: " + lastError);
                    System.err.println("Response body: " + errorDetails);

                    break;
                }
            }

            if (isInvalidApiKeyError(parsedErrorMessage)) {
                return "Error: Gemini API key is invalid. Update GEMINI_API_KEY in your .env file and restart the app container.";
            }

            if (!parsedErrorMessage.isEmpty()) {
                return "Error: Gemini API call failed: " + lastError + ". Reason: " + parsedErrorMessage;
            }

            return "Error: Gemini API call failed: " + lastError + ". Details: " + (errorDetails.length() > 200 ? errorDetails.substring(0, 200) + "..." : errorDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error analyzing submission: " + e.getMessage();
        }
    }

    private String extractApiErrorMessage(String errorDetails) {
        try {
            JsonNode errorNode = objectMapper.readTree(errorDetails);
            return errorNode.path("error").path("message").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isLikelyModelAvailabilityError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String lower = errorMessage.toLowerCase();
        return lower.contains("not found")
            || lower.contains("is not supported")
            || lower.contains("unsupported")
            || lower.contains("not available")
            || lower.contains("unknown model")
            || lower.contains("invalid model");
    }

    private boolean isInvalidApiKeyError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String lower = errorMessage.toLowerCase();
        return lower.contains("api key not valid")
            || lower.contains("invalid api key")
            || lower.contains("api_key_invalid")
            || lower.contains("permission denied");
    }

    private boolean hasConfiguredApiKey() {
        if (apiKey == null) {
            return false;
        }

        String normalizedKey = apiKey.trim();
        if (normalizedKey.isEmpty()) {
            return false;
        }

        return !normalizedKey.equalsIgnoreCase("your-gemini-api-key-here");
    }

    private List<String> buildModelCandidates() {
        Set<String> modelSet = new LinkedHashSet<>();
        modelSet.add(configuredModel);
        // Try stable models first, then experimental
        modelSet.add("gemini-1.5-flash");
        modelSet.add("gemini-1.5-flash-latest");
        modelSet.add("gemini-1.5-pro");
        modelSet.add("gemini-1.5-pro-latest");
        modelSet.add("gemini-2.0-flash-exp");
        modelSet.add("gemini-pro");
        modelSet.addAll(fetchAvailableModels());
        return new ArrayList<>(modelSet);
    }

    private List<String> fetchAvailableModels() {
        List<String> availableModels = new ArrayList<>();

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return availableModels;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode models = root.path("models");
            if (!models.isArray()) {
                return availableModels;
            }

            for (JsonNode modelNode : models) {
                JsonNode methods = modelNode.path("supportedGenerationMethods");
                boolean supportsGenerateContent = false;

                if (methods.isArray()) {
                    for (JsonNode method : methods) {
                        if ("generateContent".equals(method.asText())) {
                            supportsGenerateContent = true;
                            break;
                        }
                    }
                }

                if (!supportsGenerateContent) {
                    continue;
                }

                String fullName = modelNode.path("name").asText("");
                if (fullName.startsWith("models/")) {
                    availableModels.add(fullName.substring("models/".length()));
                }
            }
        } catch (Exception ignored) {
            // Jos mallilista ei ole saatavilla, käytetään kovakoodattuja fallback-malleja.
        }

        return availableModels;
    }
    
    private String extractAnalysisFromResponse(String jsonResponse) {
        try {
            ObjectNode node = objectMapper.readValue(jsonResponse, ObjectNode.class);
            return node.at("/candidates/0/content/parts/0/text").asText("Analysis not available");
        } catch (Exception e) {
            return "Error parsing Gemini response";
        }
    }
}
