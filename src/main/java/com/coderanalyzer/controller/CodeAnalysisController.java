package com.coderanalyzer.controller;

import com.coderanalyzer.dto.AnalysisResponse;
import com.coderanalyzer.dto.CodeRequest;
import com.coderanalyzer.service.CodeAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody CodeRequest request) {
        return codeAnalysisService.analyzeCode(request);
    }
}