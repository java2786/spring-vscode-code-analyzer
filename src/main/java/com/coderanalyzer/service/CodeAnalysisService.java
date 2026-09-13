package com.coderanalyzer.service;

import com.coderanalyzer.dto.AnalysisResponse;
import com.coderanalyzer.dto.CodeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class CodeAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeAnalysisService.class);
    private static final String CODE_PLACEHOLDER = "{code}";
    private static final String LANGUAGE_PLACEHOLDER = "{language}";
    private static final Pattern JSON_FENCE = Pattern.compile(
            "(?s)^```(?:json)?\\s*(.*?)\\s*```$");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${prompts.analyze}")
    private String analyzePromptTemplate;

    public CodeAnalysisService(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyzeCode(CodeRequest request) {
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            throw new CodeAnalysisException("Source code must not be blank");
        }

        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "unknown"
                : request.getLanguage().trim();
        String prompt = analyzePromptTemplate
                .replace(CODE_PLACEHOLDER, request.getCode())
                .replace(LANGUAGE_PLACEHOLDER, language);

        try {
            String rawResponse = chatClient.call(prompt);
            return parseResponse(rawResponse);
        } catch (CodeAnalysisException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Code analysis request failed", exception);
            throw new CodeAnalysisException("Unable to analyze the supplied code", exception);
        }
    }

    AnalysisResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new CodeAnalysisException("The AI provider returned an empty response");
        }

        String json = extractJsonObject(rawResponse);
        try {
            JsonNode root = objectMapper.readTree(json);
            AnalysisResponse response = new AnalysisResponse(
                fieldAsString(root, "explanation"),
                fieldAsString(root, "errors"),
                fieldAsString(root, "improvedVersion"),
                fieldAsString(root, "dryRun"));
            if (isBlank(response.getExplanation())
                    || isBlank(response.getErrors())
                    || isBlank(response.getImprovedVersion())
                    || isBlank(response.getDryRun())) {
                throw new CodeAnalysisException("The AI response must contain all four required JSON fields");
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new CodeAnalysisException("The AI provider returned invalid JSON", exception);
        }
    }

    private String fieldAsString(JsonNode root, String fieldName) throws JsonProcessingException {
        JsonNode value = root.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return normalizeText(value.textValue());
        }
        if (value.isArray()) {
            if (value.isEmpty()) {
                return "None";
            }
            return value.toString();
        }
        return value.toString();
    }

    private String normalizeText(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private String extractJsonObject(String rawResponse) {
        String response = rawResponse.replaceFirst("^\\uFEFF", "").trim();
        var fenceMatcher = JSON_FENCE.matcher(response);
        if (fenceMatcher.matches()) {
            response = fenceMatcher.group(1).trim();
        }

        int objectStart = response.indexOf('{');
        int objectEnd = response.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) {
            throw new CodeAnalysisException("The AI provider returned no JSON object");
        }
        return response.substring(objectStart, objectEnd + 1);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}