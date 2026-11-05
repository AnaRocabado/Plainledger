// Smoke tests against a running stack (`make up`, then `make smoke`).
// They go through the web origin, exactly like a browser: Nginx -> backend -> PostgreSQL.
import assert from "node:assert/strict";
import { describe, it } from "node:test";

const BASE_URL = process.env.BASE_URL ?? "http://localhost:3000";

async function get(path) {
  const response = await fetch(new URL(path, BASE_URL), { redirect: "manual" });
  return { response, body: await response.text() };
}

function assertServesTheApp({ response, body }) {
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html/);
  assert.match(body, /<title>Plainledger<\/title>/);
  assert.match(body, /<div id="root"><\/div>/);
}

describe(`stack at ${BASE_URL}`, () => {
  it("[FOUND-AC-01] serves the web app at the root URL", async () => {
    assertServesTheApp(await get("/"));
  });

  it("[FOUND-AC-01] serves the same app for client-side routes", async () => {
    assertServesTheApp(await get("/portfolio"));
  });

  it("[FOUND-AC-02] reports health UP, including the database, through the web origin", async () => {
    const { response, body } = await get("/actuator/health");

    assert.equal(response.status, 200);
    const health = JSON.parse(body);
    assert.equal(health.status, "UP");
    assert.equal(health.components?.db?.status, "UP");
  });

  it("[FOUND-AC-05] proxies /api to the backend, which answers unknown paths with Problem Details", async () => {
    const { response, body } = await get("/api/does-not-exist");

    assert.equal(response.status, 404);
    assert.match(response.headers.get("content-type") ?? "", /^application\/problem\+json/);
    assert.equal(JSON.parse(body).status, 404);
  });
});
