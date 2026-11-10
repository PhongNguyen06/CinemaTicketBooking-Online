# Stage 14H — VPS deployment preparation (single Spring Boot instance)

This is Stage 14G_UIFix_v2 plus deployment files. No MySQL data is overwritten and JavaFX source is retained. **This is not a deployed server or a generated Windows installer.**

## Architecture
Windows JavaFX -> HTTPS/WSS `api.your-domain.example` -> Caddy -> Spring Boot (single instance) -> private MySQL 8.4 container. Only ports 80/443 exposed. MySQL and API port 8080 are **not published**.

## Preconditions
- Linux VPS (Ubuntu LTS or another supported Linux distribution) with Docker Engine + Docker Compose plugin installed; an actual public IPv4 address, and a domain name you control. Point DNS A record for `api.your-domain.example` to VPS public IPv4; allow inbound TCP 80/443 (and UDP 443 optional).
- Before opening to real users, complete code security review and test build, login, Booking and WebSocket over HTTPS. JDK 21 and Maven are needed for local builds only. Your local MySQL on Windows is **not automatically transferred** to the VPS.
- Back up **both** current MySQL database and any old CSV files; never run the app against an empty database expecting the application to auto-create tables.

## First deployment
1. Copy `cinema-server` to VPS, then `cd cinema-server/deploy-online`.
2. `cp .env.example .env`, replace `APP_DOMAIN` with the real domain, provide unique strong random passwords; `chmod 600 .env`. Don't commit/share the file. Restrict access to the VPS and its Docker socket.
3. `docker compose --env-file .env -f compose.prod.yaml config` (inspect configuration; **do not share this output as it may contain secrets**).
4. `docker compose --env-file .env -f compose.prod.yaml up -d --build`.
5. **Database initialization/restore is mandatory**: new MySQL volume contains an empty `cinema_booking` schema. Transfer an SQL dump from your existing Windows MySQL using Workbench Data Export or `mysqldump --single-transaction --routines --triggers --databases cinema_booking > cinema_booking.sql` and transfer securely to VPS; stop API and import using the documented restore process. A `.sql.gz` export is supported by `restore.sh`. If exporting plain `.sql`, first gzip it. Check constraints and generated columns after restore. **Do not import into an already populated target without a backup.**
6. `./check-db.sh` and verify nine tables exist, then `curl -i https://YOUR_REAL_DOMAIN/api/health`. The actual API requires data and a valid account; test login and create/read a test movie.
7. Set Windows desktop `CINEMA_API_URL=https://YOUR_REAL_DOMAIN/` and run `mvn javafx:run` in cinema-desktop. WSS URL is derived automatically. Two machines on different networks should display the same data and receive updates.

## Transfer/backup specifics
- Windows PowerShell export example (MySQL client installed): `mysqldump.exe -h 127.0.0.1 -u root -p --single-transaction --routines --triggers --databases cinema_booking --result-file=cinema_booking.sql`. Password is prompted. Check export before transferring (dump contains sensitive user records).
- On VPS `gzip cinema_booking.sql`; `./restore.sh ./cinema_booking.sql.gz`; this REPLACES destination content and requires typing RESTORE. It automatically stops API before restore. **If restore fails, API remains stopped; investigate before restarting.**
- `./backup.sh` backs up current VPS MySQL. Store the encrypted copy off-server, schedule periodic backups, periodically test restores to a separate test instance.
- After importing your current DB, keep the Stage14F booking unique index and AUTO_INCREMENT properties on `bookings.id`, `tickets.id`. Verify with `SHOW CREATE TABLE tickets` and `SHOW COLUMNS FROM bookings LIKE 'id'`.

## Cloud considerations
- Caddy requests HTTPS certificate automatically if DNS points to VPS and TCP 80/443 are reachable. Reverse proxy supports WebSocket upgrades; production desktop uses HTTPS/WSS.
- **Do not share local-only URL** `http://localhost:8080/` with clients. Use the public HTTPS domain. No direct MySQL connection/credentials are needed on customer PCs.
- Keep one Spring Boot instance: sessions and WebSocket broadcasts are held in memory. Restart logs out users; multiple API replicas are not safe until session/broadcast shared-state design is added.
- Rate limiting, secure authentication/session handling, input validation, server-side authorization audit, monitoring, log rotation, TLS/certificate checks, automated backups and privacy review are prerequisites to production exposure, not proven by this ZIP.
- Changes to `compose.prod.yaml` are deployment only. It intentionally doesn't distribute an MSI installer yet. Windows packaging is the next stage after online functionality is tested.
- If the domain is pointing to a VPS other than the host running MySQL, use a private managed MySQL endpoint instead and adapt the compose deployment accordingly; never publish 3306.

## Troubleshooting
- `docker compose ... logs --tail=100 api` and `... logs --tail=100 caddy` (redact secrets, tokens, personal information).
- `curl -i https://YOUR_REAL_DOMAIN/api/health` should return 200 only after API is healthy; health checks alone do not prove MySQL authentication/booking works.
- HTTPS fails: inspect DNS, firewall rules, ports 80/443 and Caddy logs.
- Login fails: confirm user records and password hash format in restored DB, and Spring Boot logs; don't send credentials or tokens.
