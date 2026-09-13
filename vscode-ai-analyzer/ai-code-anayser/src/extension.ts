import * as vscode from 'vscode';
import axios from 'axios';

import { formatAndSaveMarkdownResponse } from './formatMarkdownResponse';
 

 
// This function runs once, when VSCode activates the extension.
export function activate(context: vscode.ExtensionContext) {
 
  // Register the command declared in package.json.
  const disposable = vscode.commands.registerCommand(
    'codeAnalyzer.analyze',
    async () => {
 
      // Step 1: Get the currently active editor.
      const editor = vscode.window.activeTextEditor;
      if (!editor) {
        vscode.window.showErrorMessage('Please open a file first.');
        return;
      }
 
      // Step 2: Get the text the developer has selected.
      const selection = editor.selection;
      const selectedCode = editor.document.getText(selection);
 
      // Step 3: Validate that something was actually selected.
      if (!selectedCode || selectedCode.trim().length === 0) {
        vscode.window.showErrorMessage('Please select some code to analyze.');
        return;
      }
 
      vscode.window.showInformationMessage('Analyzing selected code...');
 
      try {

		const prompt = `
You are a senior software engineer reviewing code for a junior developer.
Analyze the following code and respond using exactly these four numbered sections:
 
1. Explanation:
2. Errors / Problems:
3. Improved Version:
4. Dry Run:
 
Rules:
- Do not invent input values. If inputs are required and not provided, state your assumption clearly.
- Do not change the programming language unless explicitly asked.
- Keep the dry run based only on the actual code shown below.
 
Code:
${selectedCode}
`;


        // Step 4: Send the selected code to the local Ollama API.
        const response = await axios.post('http://localhost:11434/api/generate', {
          model: 'codellama:7b',
          prompt: `${prompt}`,
          stream: false,
        });

        // Step 5: Extract the generated text from the response.
        const analysisText: string = response.data.response;
        const parsedSections = parseSections(analysisText);
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0].uri.fsPath ?? '.';
        await formatAndSaveMarkdownResponse(parsedSections, workspaceFolder);
 
        // Step 6: Display the raw result (temporary — replaced with Markdown in Section 20).
        vscode.window.showInformationMessage('Analysis complete. See output.');
        const outputChannel = vscode.window.createOutputChannel('Code Analyzer');
        outputChannel.appendLine(analysisText);
        outputChannel.show();
 
      } catch (error: any) {
        vscode.window.showErrorMessage(
          'Unable to connect to Ollama. Please verify that the Docker container is running.'
        );
      }
    }
  );
 
  context.subscriptions.push(disposable);
}



function parseSections(rawText: string) {
  const explanation = rawText.split('2. Errors')[0]
    .replace('1. Explanation:', '').trim();
 
  const errors = rawText.split('2. Errors / Problems:')[1]
    ?.split('3. Improved Version:')[0].trim() ?? '';
 
  const improvedVersion = rawText.split('3. Improved Version:')[1]
    ?.split('4. Dry Run:')[0].trim() ?? '';
 
  const dryRun = rawText.split('4. Dry Run:')[1]?.trim() ?? '';
 
  return { explanation, errors, improvedVersion, dryRun };
}


export function deactivate() {}
