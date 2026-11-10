# Stage 14J — Customer self-registration (JavaFX + Spring Boot)

This is based on Stage 14I Packaging. **Deploy cinema-server to Render before distributing the updated cinema-desktop.** Otherwise `/api/auth/register` is not available online.

## Changes

- `cinema-server/src/main/java/com/cinema/server/AuthController.java`: POST `/api/auth/register` (201 Created); input validation; duplicate checks; creates Customer and linked User in a `@Transactional` operation; PBKDF2-SHA256 password hashing; forces role `USER`; primary keys allocated as high positive random IDs, compatible with the existing non-AUTO_INCREMENT `customers.id`/`users.id` columns.
- `cinema-server/src/main/java/com/cinema/server/SecurityConfig.java`: permit unauthenticated POST `/api/auth/register`.
- `cinema-desktop/src/main/java/com/cinema/remote/CinemaApiClient.java`: register endpoint client.
- `cinema-desktop/src/main/java/com/cinema/controller/LoginController.java`: online registration form, client-side validation, meaningful errors, keeps form open if invalid.

## Deployment

1. Commit and push the updated `cinema-server` to the private GitHub repository connected to Render.
2. Ensure Render redeploys and is Live. Keep database secrets in Render environment variables; do not upload credentials or database dumps.
3. Build the updated `cinema-desktop` using `mvn clean package`, then `.\build-windows.ps1` (or `.\build-windows.ps1 -Msi` with WiX installed).
4. Open JavaFX -> Create customer account -> enter username, password (12+ chars), full name, email and 10-digit phone. After successful registration log in with the new account.
5. Verify TiDB: `SELECT u.id,u.username,u.role,u.customer_id,c.name,c.email,c.phone FROM users u JOIN customers c ON c.id=u.customer_id ORDER BY u.id DESC LIMIT 10;` Do not print password hashes unnecessarily.

## Recommended DB safeguards (run only after checking existing data)

The current schema does not have UNIQUE indexes on `customers.email` and `customers.phone`. Java prechecks prevent ordinary duplicate registration, but concurrent registrations require database-level unique constraints for a strict guarantee.

Check duplicates first:
```sql
SELECT LOWER(email),COUNT(*) FROM customers WHERE email IS NOT NULL GROUP BY LOWER(email) HAVING COUNT(*)>1;
SELECT phone,COUNT(*) FROM customers WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*)>1;
```

If both empty, run on TiDB:
```sql
CREATE UNIQUE INDEX uq_customers_email ON customers(email);
CREATE UNIQUE INDEX uq_customers_phone ON customers(phone);
```

Email case-insensitive uniqueness depends on the column collation. If it is case-sensitive, use an appropriate case-insensitive collation or indexed normalized email strategy. Registration normalizes new email to lowercase.

## Security note

Registration endpoint is publicly available by design. Add rate limiting, bot protection and email verification before production launch. Existing plaintext passwords in imported accounts need resetting/upgrading separately. Render Free can cold-start and cause transient client timeouts.

## Verification

Source validation: XML/ZIP syntax inspected. Maven not installed in this editing environment; compile/build and online integration tests must be run on Windows and Render.
