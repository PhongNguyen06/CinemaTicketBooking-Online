# Stage 14J FIX1 — Customer Registration (Render / TiDB)

## What changed

- `cinema-server/.../SecurityConfig.java`: stateless Bearer API; anonymous `POST /api/auth/register`, `POST /api/auth/login`, and a new `GET /api/auth/registration-status` are explicitly permitted. Other routes remain authenticated.
- `cinema-server/.../AuthController.java`: adds the public version probe `14J-fix1`; registration still validates user input, creates customer and user within a transaction, hashes passwords using PBKDF2 and fixes role to USER; database uniqueness collisions respond HTTP 409.
- `cinema-server/src/test/java/com/cinema/server/RegistrationSecurityTest.java`: verifies anonymous GET registration status returns 200 and POST `{}` returns HTTP 400 (instead of 403).
- `cinema-desktop/.../LoginController.java`: clearer error if the online deployment is still outdated.

## IMPORTANT: Deploy backend before testing the GUI

A 403 from `POST /api/auth/register` on Render *can happen when Render is still serving an older build*. This code is only effective after the actual GitHub repo connected to Render includes it and Render deploys that commit successfully. This bundle cannot update a live deployment automatically.

1. Back up your existing repo. **Copy the updated `cinema-server/src` directory and relevant `cinema-server/pom.xml` from this ZIP into your existing `CinemaTicketBooking-Online` Git working tree**. Do not copy secrets or `.env` into Git.
2. From the repository root run:
   ```powershell
   git status
   git add cinema-server/src
   git commit -m "Fix public customer registration on Render"
   git push origin main
   ```
3. In Render Dashboard `cinema-booking-api` -> Events / Deploys, confirm the **new** commit SHA has finished deploying and status is Live. Your service root directory must be `cinema-server`.
4. Verify *without creating a user*:
   ```powershell
   curl.exe -i --max-time 90 "https://cinema-booking-api-8dsw.onrender.com/api/auth/registration-status"
   curl.exe -i --max-time 90 -X POST "https://cinema-booking-api-8dsw.onrender.com/api/auth/register" -H "Content-Type: application/json" --data-binary "{}"
   ```
   Expected: first **HTTP 200** with `"version":"14J-fix1"`, second **HTTP 400** for incomplete form. If first is 404 or 403, Render is still running an old build or different code. If first is 200 and second 403, inspect Spring Security configuration/logs of the deployed instance.
5. Run updated `cinema-desktop` with `mvn clean javafx:run`, verify customer sign-up and login using a fresh test user. Only after testing, rebuild EXE/MSI.

## Security and data

The endpoint never accepts an admin role. Database account passwords are PBKDF2-SHA256. Existing customer passwords previously saved as plaintext must be reset before making the service broadly available. Add DB-level UNIQUE constraints for customer email and phone, rate limiting, and email verification before production use.

No TiDB data migration is needed for this change. Never commit `.env`, keys, DB dumps, or login credentials.

## Build / test

Use `cd cinema-server; mvn test` on a computer with Maven/JDK 21+. The test is intentionally a Spring MVC slice and should not require a live TiDB connection. Code/ZIP was inspected in this environment; Maven/Render live deployment may not be verifiable here.
