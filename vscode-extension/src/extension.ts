import * as http from 'node:http';
import * as https from 'node:https';
import * as vscode from 'vscode';

const ANALYZE_URL = 'http://localhost:8080/api/code/analyze';
const REQUEST_TIMEOUT_MS = 120_000;

interface AnalysisResponse {
	explanation: string;
	errors: string;
	improvedVersion: string;
	dryRun: string;
}

interface AnalysisRequest {
	code: string;
	language: string;
}

export function activate(context: vscode.ExtensionContext): void {
	const command = vscode.commands.registerCommand(
		'ai-code-analyzer.analyzeSelectedCode',
		analyzeSelectedCode,
	);

	context.subscriptions.push(command);
}

async function analyzeSelectedCode(): Promise<void> {
	const editor = vscode.window.activeTextEditor;
	if (!editor) {
		await vscode.window.showWarningMessage('Open a file and select some code to analyze.');
		return;
	}

	const code = editor.document.getText(editor.selection).trim();
	if (!code) {
		await vscode.window.showWarningMessage('Please select some code to analyze.');
		return;
	}

	const requestBody: AnalysisRequest = {
		code,
		language: editor.document.languageId,
	};

	await vscode.window.withProgress(
		{
			location: vscode.ProgressLocation.Notification,
			title: 'Analyzing selected code',
			cancellable: false,
		},
		async () => {
			try {
				const response = await requestAnalysis(requestBody);
				const markdown = formatAnalysis(editor.document, response);
				const resultDocument = await vscode.workspace.openTextDocument({
					content: markdown,
					language: 'markdown',
				});
				await vscode.window.showTextDocument(resultDocument, vscode.ViewColumn.Beside);
			} catch (error) {
				const message = error instanceof Error ? error.message : 'The analysis request failed.';
				await vscode.window.showErrorMessage(message);
			}
		},
	);
}

function requestAnalysis(body: AnalysisRequest): Promise<AnalysisResponse> {
	const payload = JSON.stringify(body);
	const url = new URL(ANALYZE_URL);
	const client = url.protocol === 'https:' ? https : http;

	return new Promise((resolve, reject) => {
		const request = client.request(
			url,
			{
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Content-Length': Buffer.byteLength(payload),
				},
				timeout: REQUEST_TIMEOUT_MS,
			},
			(response) => {
				let responseBody = '';
				response.setEncoding('utf8');
				response.on('data', (chunk: string) => {
					responseBody += chunk;
				});
				response.on('end', () => {
					if (response.statusCode !== 200) {
						reject(new Error(`Analyzer returned HTTP ${response.statusCode ?? 'error'}.`));
						return;
					}

					let parsedResponse: unknown;
					try {
						parsedResponse = JSON.parse(responseBody);
					} catch {
						reject(new Error('Analyzer returned invalid JSON.'));
						return;
					}

					if (!isAnalysisResponse(parsedResponse)) {
						reject(new Error('Analyzer returned an incomplete response.'));
						return;
					}

					resolve(parsedResponse);
				});
			},
		);

		request.on('timeout', () => {
			request.destroy(new Error('The analyzer request timed out.'));
		});
		request.on('error', (error: NodeJS.ErrnoException) => {
			if (error.code === 'ECONNREFUSED') {
				reject(new Error('Unable to connect to the analyzer. Start the Spring Boot backend first.'));
				return;
			}
			reject(new Error(`Analyzer request failed: ${error.message}`));
		});

		request.write(payload);
		request.end();
	});
}

function isAnalysisResponse(value: unknown): value is AnalysisResponse {
	if (!value || typeof value !== 'object') {
		return false;
	}

	const response = value as Record<string, unknown>;
	return ['explanation', 'errors', 'improvedVersion', 'dryRun']
		.every((field) => typeof response[field] === 'string' && response[field].trim().length > 0);
}

function formatAnalysis(document: vscode.TextDocument, response: AnalysisResponse): string {
	const fileName = document.fileName.split(/[\\/]/).pop() || 'selection';
	return `# Code Analysis\n\n**File:** ${fileName}  \n**Language:** ${document.languageId}\n\n## Explanation\n\n${response.explanation}\n\n## Errors / Problems\n\n${response.errors}\n\n## Improved Version\n\n${response.improvedVersion}\n\n## Dry Run\n\n${response.dryRun}\n`;
}

export function deactivate(): void {
	// No persistent resources need cleanup.
}
