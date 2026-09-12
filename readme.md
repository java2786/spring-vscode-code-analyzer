# AI-Powered VSCode Code Analyzer — Spring Boot Extension Guide

A modern, production-grade developer tool demonstrating the integration of enterprise Java backends with state-of-the-art Large Language Models. This project bridges intelligent code analysis directly into the IDE workflow, serving as a blueprint for modern AI-assisted engineering.


## Enterprise Highlights & Core Capabilities
- Flexible AI Provider Architecture: Seamlessly switch between local inference (Ollama) and cloud APIs (OpenAI, Hugging Face) using Spring Profiles without altering business logic.
- Modern Java Backend: Built on Spring Boot and Spring AI for robust prompt orchestration, externalized configurations, and modular REST endpoints.
- IDE Productivity Integration: A lightweight TypeScript-based VS Code extension that brings instant, context-aware code reviews, complexity checks, and refactoring insights right to the developer's fingertips.
- AI-Assisted Engineering ("Vibe Coding"): Accelerated delivery leveraging modern AI-pairing workflows with GitHub Copilot, demonstrating rapid prototyping and high-quality software craftsmanship.




## Table of Contents
1. [Project Overview & Architecture Flow](#1-project-overview--architecture-flow)
2. [System Requirements & Prerequisites](#2-system-requirements--prerequisites)
3. [Spring Boot Backend Requirements](#3-spring-boot-backend-requirements)
4. [GitHub Copilot Implementation Prompts](#4-github-copilot-implementation-prompts)
   - [Prompt 1: Generate Spring Boot Service & DTOs](#prompt-1-generate-the-spring-boot-service-and-response-dtos)
   - [Prompt 2: Review Backend & Run Tests](#prompt-2-generate-the-spring-boot-rest-controller)
   - [Prompt 3: Create HTTP Testing File](#prompt-3-create-or-update-the-vscode-extension-client-typescript)
   - [Prompt 4: Create VSCode Extension](#prompt-4-create-vscode-extension)
   - [Prompt 5: Make Backend Dynamic via Profiles](#prompt-5-make-the-springboot-dynamic)
5. [End-to-End Verification Checklist](#5-end-to-end-verification-checklist)

---

## 0. Ollama setup
```bash
# 1. Pull the Ollama image
docker pull ollama/ollama
 
# 2. Run the Ollama container
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
 
# 3. Verify the container is running
docker ps
 
# 4. Download a model inside the container
docker exec -it ollama ollama pull codellama:7b
 
# 5. List installed models
docker exec -it ollama ollama list


# Stop the container
docker stop ollama
 
# Start it again later
docker start ollama
 
# View container logs
docker logs ollama
 
# Remove the container completely (models remain in the volume)
docker rm -f ollama

# Verify Ollama through the Ollama CLI
docker exec -it ollama ollama run codellama:7b "Explain what a for loop does in JavaScript"

# Verify Ollama through the HTTP API with curl
curl http://localhost:11434/api/generate \
  -d '{
    "model": "codellama:7b",
    "prompt": "Explain this JavaScript code: console.log(\"Hello\");",
    "stream": false
  }'

```
---

## 1. Project Overview & Architecture Flow

This document outlines the requirements and implementation steps for building a custom VSCode extension that analyzes source code using a local Large Language Model via Ollama, integrated through a Java Spring Boot backend . 

### Architecture Flow
* **Developer** selects code in the VSCode editor and invokes the analysis command .
* **VSCode Custom Extension** sends an HTTP POST request containing the source code payload to the **Spring Boot REST API** .
* **Spring Boot Backend** processes the request, applies externalized prompt templates, and queries the **Ollama API** running locally inside Docker .
* **Ollama** runs the local LLM (`codellama:7b`), returning the generated analysis response back to Spring Boot, which formats it and returns it to the VSCode extension to display in a new Markdown result tab .

---

## 2. System Requirements & Prerequisites
* **Java Development Kit (JDK):** Version 17 or 21 
* **Build Tool:** Maven 
* **Container Runtime:** Docker Desktop (for running the Ollama server container) 
* **LLM Engine:** Ollama running containerized (`docker pull ollama/ollama`) with `codellama:7b` pulled 
* **Development Environment:** Visual Studio Code with Node.js/TypeScript tooling for extension scaffolding 

---

## 3. Spring Boot Backend Requirements

The backend acts as the core orchestration layer between the VSCode extension and the local LLM .

### 3.1 Maven Dependencies (`pom.xml`)
The backend must use Spring Boot 3.x with the Spring AI Ollama starter package for reducing boilerplate:
* Spring Boot Starter Web (`spring-boot-starter-web`) 
* Spring AI Ollama Spring Boot Starter (`spring-ai-ollama-spring-boot-starter`) 

### 3.2 Externalized Configuration (`application.properties`)
To avoid hardcoding prompts and ensure maintainability, configure the application parameters and prompt templates externally :
* Server port set to `8080` 
* Ollama base URL set to `http://localhost:11434` 
* Model option set to `codellama:7b` with a low temperature (e.g., `0.2`) 
* Externalized prompt templates for full analysis, complexity scoring, and refactoring .

### 3.3 Core Endpoints
* `POST /api/code/analyze`: Accepts code payload and returns a structured breakdown containing code explanation, errors/problems, an improved version, and a step-by-step dry run.
* `POST /api/code/complexity`: Evaluates cyclomatic-style complexity score and category. (not implemented yet)
* `POST /api/code/refactor-suggestions`: Returns high-impact refactoring suggestions. (not implemented yet)

---

## 4. GitHub Copilot Implementation Prompts

Use the following well-tested prompts sequentially with GitHub Copilot in your workspace to generate the code components.

### Prompt 1: Generate the Spring Boot Service and Response DTOs
```text
@workspace

Act as a senior Spring Boot architect.

The Java source files for this project were intentionally deleted. Rebuild the Spring Boot backend for an AI-powered VS Code code analyzer.

Requirements:
- Spring Boot 3.x
- Java 17
- Maven
- Spring AI Ollama
- Ollama running at http://localhost:11434
- Model: codellama:7b
- Server port: 8080
- Base package: com.coderanalyzer

Create the complete backend structure:
- The Spring Boot application entry point
- com.coderanalyzer.controller.CodeAnalysisController
- com.coderanalyzer.dto.CodeRequest
- com.coderanalyzer.dto.AnalysisResponse
- com.coderanalyzer.service.CodeAnalysisService
- com.coderanalyzer.service.CodeAnalysisException

Expose:
POST /api/code/analyze

Request JSON:
{
  "code": "source code here",
  "language": "java"
}

Response JSON:
{
  "explanation": "...",
  "errors": "...",
  "improvedVersion": "...",
  "dryRun": "..."
}

The controller must remain thin. Put prompt construction, Ollama interaction, response validation, and response parsing in the service.

The AI response must contain exactly these sections:
1. Explanation:
2. Errors / Problems:
3. Improved Version:
4. Dry Run:

Reject blank source code and malformed or incomplete AI responses with clear exceptions. Use externalized prompts from application.properties. Do not add unrelated features. After creating the files, explain the folder structure and compile the project with Maven.
```

### Prompt 2: Generate the Spring Boot REST Controller
```text
@workspace

Review the Spring Boot backend you just created for the AI code analyzer.

Check:
- All Java files compile with Java 17.
- The Spring AI Ollama dependency matches the configured API.
- The application entry point is present.
- The controller is mapped to POST /api/code/analyze.
- CodeRequest accepts code and language.
- AnalysisResponse serializes all four response fields.
- Blank code is rejected.
- Empty AI responses are rejected.
- Missing analysis sections are rejected.
- Prompts are loaded from application.properties.
- No source code is logged unnecessarily.

Fix only issues related to these requirements, then run:

mvn clean test
```

### Prompt 3: Create HTTP Testing File
```text
@workspace

Create a `request.http` file at the root of the project to test the backend REST API. 

Requirements:
- Target endpoint: `POST http://localhost:8080/api/code/analyze`
- Headers: `Content-Type: application/json`
- Include at least 3 distinct test requests covering different programming languages (e.g., Java, Python, and JavaScript/Dart).
- Each request body must provide valid JSON containing both `code` (a realistic code snippet) and `language` fields matching the expected `CodeRequest` DTO structure.
```

### Prompt 4: Create VSCode Extension
```text
@workspace

Create a complete TypeScript VS Code extension in a new vscode-extension folder.

The extension must:
- Add a command named “Analyze Selected Code”.
- Add the command to the editor right-click menu.
- Read selected text from the active editor.
- Display a warning if no editor or selection exists.
- Send POST http://localhost:8080/api/code/analyze.
- Send this JSON payload:
  {
    "code": "<selected code>",
    "language": "<active document language id>"
  }
- Handle connection failures, timeouts, invalid JSON, HTTP errors, and incomplete responses.
- Format explanation, errors, improved version, and dry run as Markdown.
- Open the result in a new Markdown editor tab.
- Use TypeScript and strict compiler settings.
- Include package.json, tsconfig.json, src/extension.ts, README.md, .gitignore, and .vscodeignore.
- Do not use React or a webview.
- Compile the extension with npm run compile.
```

### Prompt 5: Make the Spring Boot Dynamic
```text
@workspace

Act as a Senior Spring Boot Architect. 

Refactor our existing Spring AI backend to support multi-provider AI model switching via Spring Profiles, while keeping the core codebase and service logic unified.

Requirements:
- Spring Boot 3.x with Spring AI
- Create 3 distinct Spring Profiles:
  1. `ollama` (Default profile; connects to local Ollama at http://localhost:11434 using CodeLlama:7b)
  2. `openai` (Connects to OpenAI API; requires externalized API key management via application-openai.properties / environment variables)
  3. `huggingface` (Connects to Hugging Face inference API; requires externalized API key management via application-huggingface.properties / environment variables)
- Use Spring AI's abstraction layer or conditional bean configuration so that model request formatting, client instantiation, and execution seamlessly adapt to whichever profile is active.
- Ensure sensitive credentials (API keys for OpenAI and Hugging Face) are strictly segregated per profile and never hardcoded.
- Update application properties / YAML configuration files to cleanly separate default configurations from profile-specific overrides.

Please provide the updated configuration files, profile-specific property templates, and any necessary bean configuration updates required to achieve dynamic provider switching.
```

---

## 5. End-to-End Verification Checklist

| Step | Component / Action | Expected Result & Execution Command |
| :--- | :--- | :--- |
| **1** | **Start Docker & Ollama** | Verify the Ollama container is running on port 11434 and `codellama:7b` is loaded.<br>`docker ps`<br>`docker exec -it ollama ollama list` |
| **2** | **Run Spring Boot Backend** | Build and run the Maven project to start the backend application on port 8080.<br>`mvn clean spring-boot:run` |
| **3** | **Launch VSCode Extension Host** | Open your extension project root directory in VSCode and **press `F5`** to open the Extension Development Host window. |
| **4** | **Test Workflow** | Select a snippet of code in any editor window, **`right-click`**, select the **`analysis command`**, and verify that the Markdown results tab populates successfully. |