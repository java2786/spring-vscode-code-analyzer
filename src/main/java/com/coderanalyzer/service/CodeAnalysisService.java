package com.coderanalyzer.service;

import com.coderanalyzer.dto.AnalysisResponse;
import com.coderanalyzer.dto.CodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeAnalysisService.class);
    private static final String CODE_PLACEHOLDER = "{code}";
    private static final String LANGUAGE_PLACEHOLDER = "{language}";
    private static final Pattern SECTION_HEADER = Pattern.compile(
            "(?im)^\\s*([1-4])\\.\\s*(Explanation|Errors\\s*/\\s*Problems|Improved\\s+Version|Dry\\s+Run)\\s*:\\s*$");

    private final ChatClient chatClient;

    @Value("${prompts.analyze}")
    private String analyzePromptTemplate;

    public CodeAnalysisService(ChatClient chatClient) {
        this.chatClient = chatClient;
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
            return parseResponse(chatClient.call(prompt));
        } catch (CodeAnalysisException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Code analysis request failed", exception);
            throw new CodeAnalysisException("Unable to analyze the supplied code", exception);
        }
    }

    private AnalysisResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new CodeAnalysisException("The AI provider returned an empty response");
        }

        Map<Section, String> sections = new EnumMap<>(Section.class);
        Matcher matcher = SECTION_HEADER.matcher(rawResponse);
        Section currentSection = null;
        int contentStart = 0;

        while (matcher.find()) {
            if (currentSection != null) {
                sections.put(currentSection, rawResponse.substring(contentStart, matcher.start()).trim());
            }
            currentSection = Section.fromNumber(matcher.group(1));
            contentStart = matcher.end();
        }

        if (currentSection != null) {
            sections.put(currentSection, rawResponse.substring(contentStart).trim());
        }

        if (sections.size() != Section.values().length || sections.values().stream().anyMatch(String::isBlank)) {
            throw new CodeAnalysisException("The AI response must contain all four required sections");
        }

        return new AnalysisResponse(
                sections.get(Section.EXPLANATION),
                sections.get(Section.ERRORS),
                sections.get(Section.IMPROVED_VERSION),
                sections.get(Section.DRY_RUN));
    }

    private enum Section {
        EXPLANATION,
        ERRORS,
        IMPROVED_VERSION,
        DRY_RUN;

        private static Section fromNumber(String number) {
            return switch (number) {
                case "1" -> EXPLANATION;
                case "2" -> ERRORS;
                case "3" -> IMPROVED_VERSION;
                case "4" -> DRY_RUN;
                default -> throw new CodeAnalysisException("Unknown analysis section: " + number);
            };
        }
    }
}