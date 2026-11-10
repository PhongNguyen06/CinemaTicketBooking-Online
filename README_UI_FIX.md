# Stage 14G — Customer UI Fix

This is Stage 14G with **display-only fixes** for the two customer screens shown in screenshots. The API, MySQL schema and booking rules are unchanged.

Changes:
- Replaced white customer screen backgrounds with the established dark theme.
- My Bookings: booking list grows to fill available height; ticket detail is a labeled section below; date uses `dd/MM/yyyy HH:mm`.
- Seat Selection: screen guide, seat color legend, consistent controls and dark seat canvas; showtime uses `dd/MM/yyyy HH:mm`.

To run from the stage root in separate terminals:

Server: `cd cinema-server`, configure `DB_URL`, `DB_USER`, `DB_PASSWORD`, then `mvn spring-boot:run`.
Desktop: `cd cinema-desktop`, configure `CINEMA_API_URL=http://localhost:8080/`, then `mvn clean javafx:run`.

If the problem involves incorrect booking data, failed cancellation or reservation rather than just appearance, provide the exact action and corresponding error log.

No live Windows JavaFX/MySQL verification was performed here.
