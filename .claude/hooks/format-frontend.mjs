#!/usr/bin/env node
// PostToolUse hook: format frontend files with Prettier right after the agent edits them,
// so formatting never becomes a review topic. Java formatting is left to Spotless
// (`make fmt`, enforced in CI) because a Maven run per edit is too slow.
// The hook never blocks the agent: any failure is reported and ignored.
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { join, relative, resolve } from "node:path";

const FORMATTABLE = /\.(ts|tsx|js|jsx|mjs|css|json|html|md)$/;

try {
  const input = JSON.parse(readFileSync(0, "utf8"));
  const filePath = input?.tool_input?.file_path;
  const projectDir = process.env.CLAUDE_PROJECT_DIR ?? process.cwd();
  const frontendDir = resolve(projectDir, "frontend");
  const prettier = join(frontendDir, "node_modules", ".bin", "prettier");

  if (
    filePath &&
    FORMATTABLE.test(filePath) &&
    !relative(frontendDir, resolve(filePath)).startsWith("..") &&
    existsSync(prettier)
  ) {
    spawnSync(prettier, ["--write", "--log-level", "warn", filePath], {
      cwd: frontendDir,
      stdio: "inherit",
    });
  }
} catch (error) {
  console.error(`format-frontend hook skipped: ${error.message}`);
}
process.exit(0);
