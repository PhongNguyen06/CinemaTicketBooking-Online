# Stage 14G UI Fix v2 — Seat legend colors

- Based on Stage 14G UI Fix; retains all existing JavaFX source, Spring Boot backend and database integration.
- In `remote-seat-selection.fxml`, replaces `● Available`, `● VIP`, `● Booked`, `● Selected` Label-only legend with 12px color dot Regions plus separate light-colored labels.
- In `style.css`, adds `.legend-dot-*` classes whose colors match the seat buttons. This prevents `.customer-legend .label` from overriding legend colors.
- No database migration is needed.

Run `mvn clean javafx:run` within `cinema-desktop` with the Spring Boot API running, as in Stage 14G.
