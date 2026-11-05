// Acceptance-criteria traceability (ADR-0009): pure functions, no I/O, no dependencies.
//
//   specs + test results  ->  AC -> tests matrix  +  errors that fail the build

export const EVIDENCE_FORMAT = "plainledger-evidence/v1";
export const STATUSES = ["draft", "approved", "in-progress", "done"];

const AC_ID = String.raw`[A-Z][A-Z0-9]*-AC-\d{2,}`;
const AC_DEFINITION = new RegExp(String.raw`^\s*[-*]\s+(~~)?\*\*(${AC_ID})\*\*`);
const AC_REFERENCE = new RegExp(String.raw`\[(${AC_ID}(?:\s*,\s*${AC_ID})*)\]`, "g");

/**
 * Parses a spec.md: front matter (`id`, `title`, `prefix`, `status`) and acceptance criteria,
 * which are list items starting with a bold ID. A struck-through item (`~~**X-AC-01**~~`) is retired:
 * still a known ID, no longer required.
 */
export function parseSpec(markdown, path) {
  const frontMatter = {};
  const lines = markdown.split(/\r?\n/);
  let body = lines;
  if (lines[0]?.trim() === "---") {
    const end = lines.indexOf("---", 1);
    if (end > 0) {
      for (const line of lines.slice(1, end)) {
        const match = line.match(/^([A-Za-z_]+):\s*(.*)$/);
        if (match) frontMatter[match[1]] = match[2].trim().replace(/^["']|["']$/g, "");
      }
      body = lines.slice(end + 1);
    }
  }
  const criteria = [];
  body.forEach((line, index) => {
    const match = line.match(AC_DEFINITION);
    if (match) criteria.push({ id: match[2], retired: Boolean(match[1]), line: index + 1 });
  });
  return {
    path,
    id: frontMatter.id ?? "",
    title: frontMatter.title ?? path,
    prefix: frontMatter.prefix ?? "",
    status: frontMatter.status ?? "",
    criteria,
  };
}

/** Extracts AC IDs written as `[FOUND-AC-01]` or `[FOUND-AC-01, FOUND-AC-02]` in a test name. */
export function criteriaInName(name) {
  const ids = [];
  for (const match of name.matchAll(AC_REFERENCE)) {
    ids.push(...match[1].split(",").map((id) => id.trim()));
  }
  return [...new Set(ids)];
}

/** Parses evidence written by the backend's TraceabilityListener. Returns null for other JSON. */
export function parseEvidenceJson(text, source) {
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    return null;
  }
  if (data?.format !== EVIDENCE_FORMAT || !Array.isArray(data.results)) return null;
  return data.results.map((result) => ({
    test: result.test,
    status: result.status,
    criteria: result.criteria ?? [],
    source,
  }));
}

/**
 * Parses JUnit XML (Vitest, node:test, Playwright). AC IDs come from `[ID]` in the test name.
 * Returns only test cases that reference at least one AC.
 */
export function parseJUnitXml(xml, source) {
  const results = [];
  const testcase = /<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g;
  for (const match of xml.matchAll(testcase)) {
    const attributes = Object.fromEntries(
      [...match[1].matchAll(/([\w:-]+)\s*=\s*"([^"]*)"/g)].map(([, key, value]) => [key, decodeXml(value)]),
    );
    const name = attributes.name ?? "";
    const criteria = criteriaInName(name);
    if (criteria.length === 0) continue;
    const body = match[2] ?? "";
    const status = /<(failure|error)\b/.test(body) ? "failed" : /<skipped\b/.test(body) ? "skipped" : "passed";
    // node:test uses the constant classname "test"; it adds nothing to the name.
    const classname = attributes.classname && attributes.classname !== "test" ? attributes.classname : "";
    const test = classname ? `${classname} > ${name}` : name;
    results.push({ test, status, criteria, source });
  }
  return results;
}

function decodeXml(value) {
  return value.replace(/&(#x[0-9a-f]+|#\d+|lt|gt|amp|quot|apos);/gi, (entity, code) => {
    const named = { lt: "<", gt: ">", amp: "&", quot: '"', apos: "'" }[code.toLowerCase()];
    if (named) return named;
    const point = code[1].toLowerCase() === "x" ? parseInt(code.slice(2), 16) : parseInt(code.slice(1), 10);
    return String.fromCodePoint(point);
  });
}

/**
 * Builds the AC -> tests matrix and the list of errors.
 *
 * Errors (always): invalid status, duplicate AC ID, AC prefix not matching its spec, a test referencing
 * an unknown AC, a failed test referencing an AC.
 * Errors (specs with status `done` only): an active AC without a passing test.
 */
export function evaluate(specs, results) {
  const errors = [];
  const known = new Map();

  for (const spec of specs) {
    if (spec.criteria.length > 0 && !STATUSES.includes(spec.status)) {
      errors.push(`${spec.path}: status "${spec.status}" must be one of ${STATUSES.join(", ")}`);
    }
    for (const criterion of spec.criteria) {
      if (known.has(criterion.id)) {
        errors.push(`${criterion.id} is defined twice: ${known.get(criterion.id).spec.path} and ${spec.path}`);
        continue;
      }
      if (!criterion.id.startsWith(`${spec.prefix}-AC-`)) {
        errors.push(`${criterion.id} in ${spec.path} does not match the spec's prefix "${spec.prefix}"`);
      }
      known.set(criterion.id, { spec, criterion, tests: [] });
    }
  }

  for (const result of results) {
    for (const id of result.criteria) {
      const entry = known.get(id);
      if (!entry) {
        errors.push(`${result.test} (${result.source}) references unknown acceptance criterion ${id}`);
        continue;
      }
      entry.tests.push(result);
      if (result.status === "failed") {
        errors.push(`${id}: test failed: ${result.test}`);
      }
    }
  }

  const rows = [...known.values()].map(({ spec, criterion, tests }) => {
    const passing = tests.filter((t) => t.status === "passed");
    const failing = tests.filter((t) => t.status === "failed");
    const enforced = spec.status === "done" && !criterion.retired;
    let state;
    if (criterion.retired) state = "retired";
    else if (failing.length > 0) state = "failing";
    else if (passing.length > 0) state = "covered";
    else state = enforced ? "missing" : "pending";
    if (state === "missing") errors.push(`${criterion.id} (${spec.path}, done) has no passing test`);
    return { spec, id: criterion.id, state, tests };
  });

  return { rows, errors: [...new Set(errors)] };
}

const ICONS = { covered: "✅ covered", failing: "❌ failing", missing: "❌ missing", pending: "⏳ pending", retired: "➖ retired" };

/** Renders the matrix and errors as GitHub-flavored markdown. */
export function renderMarkdown({ rows, errors }, evidenceCount) {
  const out = ["# Acceptance criteria traceability", ""];
  const bySpec = Map.groupBy(rows, (row) => row.spec.path);
  const active = rows.filter((r) => r.state !== "retired");
  const covered = active.filter((r) => r.state === "covered").length;
  out.push(`**${covered} / ${active.length}** acceptance criteria covered by passing tests · ${evidenceCount} result files read`, "");

  for (const [, specRows] of bySpec) {
    const spec = specRows[0].spec;
    const enforcement = spec.status === "done" ? "enforced" : "reported only";
    out.push(`## ${spec.id} — ${spec.title} · \`${spec.status}\` (${enforcement})`, "");
    out.push("| AC | State | Passing tests |", "|---|---|---|");
    for (const row of specRows) {
      out.push(`| ${row.id} | ${ICONS[row.state]} | ${passingTests(row.tests) || "—"} |`);
    }
    out.push("");
  }

  if (errors.length > 0) {
    out.push(`## ❌ ${errors.length} error(s)`, "", ...errors.map((e) => `- ${e}`), "");
  } else {
    out.push("No errors.", "");
  }
  return out.join("\n");
}

/** One entry per test; parameterized and property tests run many times and are shown with a count. */
function passingTests(tests) {
  const counts = new Map();
  for (const t of tests.filter((t) => t.status === "passed")) {
    const name = t.test.replace(/^com\.plainledger\./, "");
    counts.set(name, (counts.get(name) ?? 0) + 1);
  }
  return [...counts].map(([name, n]) => `\`${name}\`${n > 1 ? ` ×${n}` : ""}`).join("<br>");
}
