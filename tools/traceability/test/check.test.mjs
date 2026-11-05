import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { after, describe, it } from "node:test";
import { fileURLToPath } from "node:url";
import { criteriaInName, evaluate, parseEvidenceJson, parseJUnitXml, parseSpec } from "../lib.mjs";

const CHECK = fileURLToPath(new URL("../check.mjs", import.meta.url));

function spec({ status = "done", prefix = "DEMO", criteria = ["DEMO-AC-01", "DEMO-AC-02"], path = "specs/900-demo/spec.md" } = {}) {
  const items = criteria.map((id) => (id.startsWith("~~") ? `- ~~**${id.slice(2)}**~~ — retired` : `- **${id}** — Given…`));
  const markdown = ["---", 'id: "900"', "title: Demo", `prefix: ${prefix}`, `status: ${status}`, "---", "", "## Acceptance criteria", "", ...items].join("\n");
  return parseSpec(markdown, path);
}

const passed = (test, ...criteria) => ({ test, status: "passed", criteria, source: "test" });
const failed = (test, ...criteria) => ({ test, status: "failed", criteria, source: "test" });

describe("parsing", () => {
  it("[FOUND-AC-09] reads front matter and acceptance criteria, including retired ones", () => {
    const parsed = spec({ criteria: ["DEMO-AC-01", "~~DEMO-AC-02"] });

    assert.equal(parsed.prefix, "DEMO");
    assert.equal(parsed.status, "done");
    assert.deepEqual(
      parsed.criteria.map(({ id, retired }) => ({ id, retired })),
      [
        { id: "DEMO-AC-01", retired: false },
        { id: "DEMO-AC-02", retired: true },
      ],
    );
  });

  it("[FOUND-AC-09] ignores AC IDs that are mentioned but not defined", () => {
    const parsed = parseSpec("---\nprefix: DEMO\nstatus: done\n---\nSee DEMO-AC-01 and **DEMO-AC-02** inline.\n", "x");

    assert.deepEqual(parsed.criteria, []);
  });

  it("[FOUND-AC-09] extracts one or several AC IDs from a test name", () => {
    assert.deepEqual(criteriaInName("App > [DEMO-AC-01] renders"), ["DEMO-AC-01"]);
    assert.deepEqual(criteriaInName("[DEMO-AC-01, DEMO-AC-02] and [DEMO-AC-03]"), ["DEMO-AC-01", "DEMO-AC-02", "DEMO-AC-03"]);
    assert.deepEqual(criteriaInName("no criteria [here]"), []);
  });

  it("[FOUND-AC-09] reads JUnit XML from Vitest and node:test, decoding entities and statuses", () => {
    const xml = `<?xml version="1.0"?>
      <testsuites><testsuite name="s">
        <testcase classname="src/App.test.tsx" name="App &gt; [DEMO-AC-01] renders &quot;x&quot;" time="0.1">
        </testcase>
        <testcase name="[DEMO-AC-02] breaks" time="0.1"><failure message="boom">stack</failure></testcase>
        <testcase name="[DEMO-AC-03] later" time="0"><skipped type="skipped"/></testcase>
        <testcase name="[DEMO-AC-04] self-closing" time="0"/>
        <testcase name="no criteria" time="0"/>
      </testsuite></testsuites>`;

    assert.deepEqual(
      parseJUnitXml(xml, "junit.xml").map(({ test, status, criteria }) => ({ test, status, criteria })),
      [
        { test: 'src/App.test.tsx > App > [DEMO-AC-01] renders "x"', status: "passed", criteria: ["DEMO-AC-01"] },
        { test: "[DEMO-AC-02] breaks", status: "failed", criteria: ["DEMO-AC-02"] },
        { test: "[DEMO-AC-03] later", status: "skipped", criteria: ["DEMO-AC-03"] },
        { test: "[DEMO-AC-04] self-closing", status: "passed", criteria: ["DEMO-AC-04"] },
      ],
    );
  });

  it("[FOUND-AC-09] reads backend evidence JSON and ignores unrelated JSON", () => {
    const evidence = JSON.stringify({
      format: "plainledger-evidence/v1",
      suite: "backend-unit",
      results: [{ test: "a.BTest#m", name: "m()", status: "passed", criteria: ["DEMO-AC-01"] }],
    });

    assert.deepEqual(parseEvidenceJson(evidence, "unit.json"), [
      { test: "a.BTest#m", status: "passed", criteria: ["DEMO-AC-01"], source: "unit.json" },
    ]);
    assert.equal(parseEvidenceJson('{"other": true}', "x.json"), null);
    assert.equal(parseEvidenceJson("not json", "x.json"), null);
  });
});

describe("evaluation", () => {
  it("[FOUND-AC-09] succeeds when every AC of a done spec has a passing test", () => {
    const { rows, errors } = evaluate([spec()], [passed("t1", "DEMO-AC-01"), passed("t2", "DEMO-AC-02")]);

    assert.deepEqual(errors, []);
    assert.deepEqual(
      rows.map((r) => r.state),
      ["covered", "covered"],
    );
  });

  it("[FOUND-AC-09] fails when an AC of a done spec has no passing test", () => {
    const { errors } = evaluate([spec()], [passed("t1", "DEMO-AC-01"), { ...passed("t2", "DEMO-AC-02"), status: "skipped" }]);

    assert.deepEqual(errors, ["DEMO-AC-02 (specs/900-demo/spec.md, done) has no passing test"]);
  });

  it("[FOUND-AC-09] reports but does not fail uncovered ACs of specs that are not done", () => {
    for (const status of ["draft", "approved", "in-progress"]) {
      const { rows, errors } = evaluate([spec({ status })], [passed("t1", "DEMO-AC-01")]);

      assert.deepEqual(errors, [], status);
      assert.deepEqual(
        rows.map((r) => r.state),
        ["covered", "pending"],
        status,
      );
    }
  });

  it("[FOUND-AC-09] does not require retired ACs", () => {
    const { rows, errors } = evaluate([spec({ criteria: ["DEMO-AC-01", "~~DEMO-AC-02"] })], [passed("t1", "DEMO-AC-01")]);

    assert.deepEqual(errors, []);
    assert.equal(rows[1].state, "retired");
  });

  it("[FOUND-AC-09] fails when a test references an unknown AC, whatever the spec status", () => {
    const { errors } = evaluate([spec({ status: "draft" })], [passed("t1", "DEMO-AC-99")]);

    assert.deepEqual(errors, ["t1 (test) references unknown acceptance criterion DEMO-AC-99"]);
  });

  it("[FOUND-AC-09] fails when a test referencing an AC failed, even if another test passed", () => {
    const { rows, errors } = evaluate(
      [spec({ status: "in-progress" })],
      [passed("t1", "DEMO-AC-01"), failed("t2", "DEMO-AC-01")],
    );

    assert.deepEqual(errors, ["DEMO-AC-01: test failed: t2"]);
    assert.equal(rows[0].state, "failing");
  });

  it("[FOUND-AC-09] fails on duplicate AC IDs across specs", () => {
    const { errors } = evaluate([spec(), spec({ path: "specs/901-copy/spec.md" })], [passed("t", "DEMO-AC-01", "DEMO-AC-02")]);

    assert.deepEqual(errors, [
      "DEMO-AC-01 is defined twice: specs/900-demo/spec.md and specs/901-copy/spec.md",
      "DEMO-AC-02 is defined twice: specs/900-demo/spec.md and specs/901-copy/spec.md",
    ]);
  });

  it("[FOUND-AC-09] fails when an AC does not match its spec's prefix", () => {
    const { errors } = evaluate([spec({ criteria: ["OTHER-AC-01"] })], [passed("t", "OTHER-AC-01")]);

    assert.deepEqual(errors, ['OTHER-AC-01 in specs/900-demo/spec.md does not match the spec\'s prefix "DEMO"']);
  });

  it("[FOUND-AC-09] fails on an unknown spec status", () => {
    const { errors } = evaluate([spec({ status: "finished" })], [passed("t", "DEMO-AC-01", "DEMO-AC-02")]);

    assert.deepEqual(errors, ['specs/900-demo/spec.md: status "finished" must be one of draft, approved, in-progress, done']);
  });
});

describe("command line", () => {
  const root = mkdtempSync(join(tmpdir(), "traceability-"));
  after(() => rmSync(root, { recursive: true, force: true }));

  function workspace(name, { status, evidence }) {
    const dir = join(root, name);
    mkdirSync(join(dir, "specs", "900-demo"), { recursive: true });
    mkdirSync(join(dir, "specs", "_templates"), { recursive: true });
    mkdirSync(join(dir, "evidence", "nested"), { recursive: true });
    writeFileSync(
      join(dir, "specs", "900-demo", "spec.md"),
      `---\nid: "900"\ntitle: Demo\nprefix: DEMO\nstatus: ${status}\n---\n\n- **DEMO-AC-01** — x\n- **DEMO-AC-02** — y\n`,
    );
    // Templates contain placeholder IDs and must be ignored.
    writeFileSync(join(dir, "specs", "_templates", "spec.md"), "---\nprefix: PREFIX\nstatus: draft\n---\n- **PREFIX-AC-01** — x\n");
    writeFileSync(
      join(dir, "evidence", "unit.json"),
      JSON.stringify({ format: "plainledger-evidence/v1", suite: "unit", results: [{ test: "A#a", status: "passed", criteria: ["DEMO-AC-01"] }] }),
    );
    writeFileSync(join(dir, "evidence", "nested", "junit.xml"), evidence);
    return dir;
  }

  function run(dir) {
    const env = { ...process.env };
    delete env.GITHUB_STEP_SUMMARY;
    return spawnSync(process.execPath, [CHECK, "--specs", "specs", "--evidence", "evidence", "--report", "out/report.md"], {
      cwd: dir,
      encoding: "utf8",
      env,
    });
  }

  it("[FOUND-AC-09] exits 0 and prints the matrix when all ACs of a done spec pass", () => {
    const dir = workspace("ok", { status: "done", evidence: '<testsuite><testcase name="[DEMO-AC-02] works"/></testsuite>' });

    const result = run(dir);

    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /\*\*2 \/ 2\*\* acceptance criteria covered by passing tests · 2 result files read/);
    assert.match(result.stdout, /\| DEMO-AC-01 \| ✅ covered \| `A#a` \|/);
    assert.match(result.stdout, /No errors\./);
    assert.doesNotMatch(result.stdout, /PREFIX-AC-01/);
  });

  it("[FOUND-AC-09] exits 1 and lists the errors when an AC of a done spec is not covered", () => {
    const dir = workspace("missing", { status: "done", evidence: "<testsuite></testsuite>" });

    const result = run(dir);

    assert.equal(result.status, 1);
    assert.match(result.stdout, /\| DEMO-AC-02 \| ❌ missing \| — \|/);
    assert.match(result.stdout, /DEMO-AC-02 \(specs\/900-demo\/spec\.md, done\) has no passing test/);
  });
});
