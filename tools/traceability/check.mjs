#!/usr/bin/env node
// Acceptance-criteria traceability check (ADR-0009).
//
// Usage: node tools/traceability/check.mjs [--specs <dir>] [--evidence <dir>]... [--report <file>]
//
// Reads every specs/<NNN-name>/spec.md and every result file under the evidence directories
// (plainledger-evidence JSON and JUnit XML), prints the AC -> tests matrix, and exits with 1 on errors.
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { parseArgs } from "node:util";
import { evaluate, parseEvidenceJson, parseJUnitXml, parseSpec, renderMarkdown } from "./lib.mjs";

const DEFAULT_EVIDENCE = [
  "backend/target/traceability",
  "frontend/test-results",
  "tools/traceability/test-results",
  "tools/smoke/test-results",
];

const { values } = parseArgs({
  options: {
    specs: { type: "string", default: "specs" },
    evidence: { type: "string", multiple: true },
    report: { type: "string" },
  },
});

const specs = readdirSync(values.specs, { withFileTypes: true })
  .filter((entry) => entry.isDirectory() && !entry.name.startsWith("_"))
  .map((entry) => join(values.specs, entry.name, "spec.md"))
  .filter(existsSync)
  .map((path) => parseSpec(readFileSync(path, "utf8"), path));

const evidenceFiles = (values.evidence ?? DEFAULT_EVIDENCE).filter(existsSync).flatMap(listFiles);
const results = evidenceFiles.flatMap((file) => {
  const text = readFileSync(file, "utf8");
  const source = relative(process.cwd(), file);
  if (file.endsWith(".json")) return parseEvidenceJson(text, source) ?? [];
  if (file.endsWith(".xml")) return parseJUnitXml(text, source);
  return [];
});

const evaluation = evaluate(specs, results);
const markdown = renderMarkdown(evaluation, evidenceFiles.length);

console.log(markdown);
if (values.report) {
  mkdirSync(dirname(values.report), { recursive: true });
  writeFileSync(values.report, markdown);
}
if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, `${markdown}\n`);
}
process.exit(evaluation.errors.length > 0 ? 1 : 0);

function listFiles(path) {
  if (statSync(path).isFile()) return [path];
  return readdirSync(path, { withFileTypes: true }).flatMap((entry) => listFiles(join(path, entry.name)));
}
