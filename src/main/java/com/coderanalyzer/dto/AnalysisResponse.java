package com.coderanalyzer.dto;

public class AnalysisResponse {
    private String explanation;
    private String errors;
    private String improvedVersion;
    private String dryRun;

    public AnalysisResponse() {
    }

    public AnalysisResponse(String explanation, String errors, String improvedVersion, String dryRun) {
        this.explanation = explanation;
        this.errors = errors;
        this.improvedVersion = improvedVersion;
        this.dryRun = dryRun;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getImprovedVersion() {
        return improvedVersion;
    }

    public void setImprovedVersion(String improvedVersion) {
        this.improvedVersion = improvedVersion;
    }

    public String getDryRun() {
        return dryRun;
    }

    public void setDryRun(String dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public String toString() {
        return "AnalysisResponse [explanation=" + explanation + ", errors=" + errors + ", improvedVersion="
                + improvedVersion + ", dryRun=" + dryRun + "]";
    }
    
}