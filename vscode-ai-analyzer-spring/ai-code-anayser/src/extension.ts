import * as vscode from 'vscode';
import axios from 'axios';

import { formatAndSaveMarkdownResponse } from './formatMarkdownResponse';
 
const ANALYZE_CODE_URL = 'http://localhost:8080/api/code/analyze';

interface AnalysisResponse {
  explanation: string;
  errors: string;
  improvedVersion: string;
  dryRun: string;
}

 
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

        // Send the selected code to the local Spring Boot API.
        const response = await axios.post<AnalysisResponse>(ANALYZE_CODE_URL, {
          language: 'java',
          code: selectedCode,
        });

        const parsedSections = response.data;
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0].uri.fsPath ?? '.';
        await formatAndSaveMarkdownResponse(parsedSections, workspaceFolder);
 
        vscode.window.showInformationMessage('Analysis complete. See output.');
        const outputChannel = vscode.window.createOutputChannel('Code Analyzer');
        outputChannel.appendLine(JSON.stringify(parsedSections, null, 2));
        outputChannel.show();
 
      } catch (error: unknown) {
        vscode.window.showErrorMessage(
          'Unable to analyze the selected code. Please verify that the Spring Boot app is running on localhost:8080.'
        );
      }
    }
  );
 
  context.subscriptions.push(disposable);
}



export function deactivate() {}
