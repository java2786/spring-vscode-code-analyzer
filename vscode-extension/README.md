# AI Code Analyzer

Analyze selected source code in VS Code using the local Spring Boot backend and Ollama.

## Usage

1. Start the Spring Boot backend from the repository root:

   ```bash
   mvn spring-boot:run
   ```

2. Ensure Ollama is running at `http://localhost:11434` with the configured model available.
3. In VS Code, open this folder and press `F5` to launch the Extension Development Host.
4. Select code in an editor, right-click, and choose **Analyze Selected Code**.

The extension sends the selection and the active document language ID to `POST http://localhost:8080/api/code/analyze`, then opens the explanation, errors, improved version, and dry run in a new Markdown tab.

## Development

```bash
npm install
npm run compile
```

The extension uses TypeScript with strict compiler settings and does not use a webview.
