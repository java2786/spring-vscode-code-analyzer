package com.coderanalyzer.controller;

import com.coderanalyzer.dto.AnalysisResponse;
import com.coderanalyzer.dto.CodeRequest;
import com.coderanalyzer.service.CodeAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    @PostMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse analyze(@RequestBody CodeRequest request) {
        System.out.println("Received request: " + request);
        return codeAnalysisService.analyzeCode(request);
    }
}