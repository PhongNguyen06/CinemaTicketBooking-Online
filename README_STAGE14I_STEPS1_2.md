# Stage 14I — Steps 1 and 2

Đã bổ sung `cinema-desktop/src/main/java/com/cinema/Launcher.java` và hai plugin trong `cinema-desktop/pom.xml`:

- `maven-jar-plugin`: đặt `Main-Class: com.cinema.Launcher` và classpath `lib/` vào manifest.
- `maven-dependency-plugin`: khi chạy `mvn clean package`, copy runtime dependencies vào `target/lib/`.

Lưu ý: Source vốn đã có `DesktopLauncher.java` cho script cũ; file này được giữ nguyên để tránh phá cách đóng gói trước đây.

Trên Windows, mở PowerShell tại `cinema-desktop` rồi chạy:

```powershell
mvn clean package
java -jar .\target\cinema-ticket-booking-javafx-1.0-SNAPSHOT.jar
```

Chưa chạy Maven trong môi trường tạo ZIP; hãy xác nhận BUILD SUCCESS và JavaFX mở được trên Windows. Project chưa được đóng gói thành MSI.
