# ADR-0007: Server-side sessions with a same-origin cookie

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

Plainledger has a browser SPA and one backend. Users must log in, log out reliably, and only ever see their own data (RULE-14). The main threats for a browser client are XSS stealing credentials and CSRF.

## Decision

Authentication uses **server-side sessions** (Spring Security + Spring Session JDBC, stored in PostgreSQL). The session ID travels in a cookie that is `HttpOnly`, `SameSite=Lax` and `Secure` outside local development. State-changing requests require a CSRF token. The SPA and API are served from **one origin**: Nginx serves the static files and proxies `/api` to the backend, so no CORS configuration is needed.

## Consequences

- ✅ JavaScript never sees a credential, so XSS cannot exfiltrate a token.
- ✅ Logout is real: deleting the session row revokes it immediately. Sessions survive backend restarts.
- ✅ Stock Spring Security features, with little custom security code to get wrong.
- ⚠️ CSRF protection is required (built in; the SPA sends the token header).
- ⚠️ A database read per request to load the session. Negligible at this scale.
- ⚠️ Not designed for third-party API clients or mobile apps. If those come, add token auth for them.

## Alternatives considered

- **JWT in the browser (access + refresh tokens)** — stateless, but the token must be stored somewhere JavaScript can reach (or emulate a session with cookies anyway), and revocation on logout needs a denylist. More moving parts for no benefit with a single first-party client.
- **External identity provider (Keycloak, Auth0)** — production-grade, but adds a service to run or a paid dependency, and is out of proportion for the MVP.
- **In-memory sessions** — simpler, but every backend restart logs everyone out.
