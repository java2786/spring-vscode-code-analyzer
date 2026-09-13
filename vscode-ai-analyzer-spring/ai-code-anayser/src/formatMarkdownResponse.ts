import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
 
interface ParsedSections {
  explanation: string;
  errors: string;
  improvedVersion: string;
  dryRun: string;
}
 
export async function formatAndSaveMarkdownResponse(
  parsed: ParsedSections,
  workspaceFolder: string
) {
  const markdown = `# Code Analysis Result
 
## Explanation
${parsed.explanation}
 
## Issues / Missing Parts
${parsed.errors}
 
## Corrected Code
\`\`\`code
${parsed.improvedVersion}
\`\`\`
 
## Dry Run
${parsed.dryRun}
`;
 
  const filePath = path.join(workspaceFolder, 'code-analysis-result.md');
  fs.writeFileSync(filePath, markdown, 'utf8');
 
  const doc = await vscode.workspace.openTextDocument(filePath);
  await vscode.window.showTextDocument(doc, { preview: false });
}
