package com.coderanalyzer.service;

import com.coderanalyzer.dto.AnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CodeAnalysisServiceTest {

    private final CodeAnalysisService service = new CodeAnalysisService(
            mock(ChatClient.class), new ObjectMapper());

    @Test
    void parsesJsonWrappedInMarkdownAndConversation() {
        String response = "Here is the analysis:\n```json\n"
                + "{\"explanation\":\"Uses a loop\","
                + "\"errors\":\"None\","
                + "\"improvedVersion\":\"```java\\nSystem.out.println(\\\"ok\\\");\\n```\","
                + "\"dryRun\":\"Prints ok\\n\"}\n````";

        AnalysisResponse parsed = service.parseResponse(response);

        assertEquals("Uses a loop", parsed.getExplanation());
        assertEquals("```java\nSystem.out.println(\"ok\");\n```", parsed.getImprovedVersion());
        assertEquals("Prints ok\n", parsed.getDryRun());
    }

    @Test
    void rejectsResponseWithoutRequiredFields() {
        assertThrows(CodeAnalysisException.class,
                () -> service.parseResponse("{\"explanation\":\"Only one field\"}"));
    }

    @Test
    void convertsArrayFieldsToClientCompatibleStrings() {
        String response = "{\"explanation\":\"Uses a loop\","
                + "\"errors\":[],"
                + "\"improvedVersion\":\"sum = 3;\","
                + "\"dryRun\":\"Prints 3\"}";

        AnalysisResponse parsed = service.parseResponse(response);

        assertEquals("None", parsed.getErrors());
    }

    @Test
    void convertsDoubleEscapedLineBreaksToActualLineBreaks() {
        String response = "{\"explanation\":\"Valid method\","
                + "\"errors\":\"None\","
                + "\"improvedVersion\":\"return first + second;\\\\n\","
                + "\"dryRun\":\"Returns 3\"}";

        AnalysisResponse parsed = service.parseResponse(response);

        assertEquals("return first + second;\n", parsed.getImprovedVersion());
    }
}