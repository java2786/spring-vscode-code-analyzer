package com.coderanalyzer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesMultilineCodeAndQuotesAsValidJson() throws Exception {
        String improvedVersion = "```java\nSystem.out.println(\"hello\");\n```";
        AnalysisResponse response = new AnalysisResponse(
                "Use a \"clear\" explanation.",
                "No issues found.",
                improvedVersion,
                "Prints the value on a new line.");

        String json = objectMapper.writeValueAsString(response);
        AnalysisResponse parsed = objectMapper.readValue(json, AnalysisResponse.class);

        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\"clear\\\""));
        assertEquals(improvedVersion, parsed.getImprovedVersion());
        assertEquals("Use a \"clear\" explanation.", parsed.getExplanation());
    }
}