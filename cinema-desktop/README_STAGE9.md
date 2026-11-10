# Stage 9 — Giao diện và đóng gói Windows

## Những thay đổi
- Làm mới toàn bộ `style.css`: sidebar, nút, trường nhập, bảng, màu và trạng thái ghế.
- Cập nhật `login.fxml` và `dashboard.fxml` mà không đổi các tên `fx:id` và event handlers.
- Thêm `DesktopLauncher.java` cho launcher đóng gói.
- Thêm `package-windows.ps1` để tạo Windows **app-image** với `jpackage`.

## Chạy dự án trong IntelliJ (Windows)
1. Sử dụng **JDK 21** cho Maven và Java; kiểm tra `java -version`, `mvn -version`, `jpackage --version`.
2. Tại thư mục có `pom.xml`: `mvn clean javafx:run`.
3. Chế độ lưu trữ mặc định là CSV; giữ nguyên `database/` và chạy từ thư mục project.
4. MySQL chỉ bật khi đã sửa dữ liệu CSV có khóa ngoại lỗi và import thành công. Nếu dùng MySQL, đặt `CINEMA_STORAGE=mysql`, `CINEMA_DB_URL`, `CINEMA_DB_USER`, `CINEMA_DB_PASSWORD` trong **cùng Terminal** trước khi chạy. Không commit mật khẩu vào Git.

## Tạo ứng dụng Windows (app-image)
Trong PowerShell có JDK 21, Maven và `jpackage`:

```powershell
powershell -ExecutionPolicy Bypass -File .\package-windows.ps1
```

Kết quả mong đợi tại `dist/CinemaTicketBooking/`, có `CinemaTicketBooking.exe` và `LaunchCinema.bat`.
Với chế độ **CSV**, nên đặt thư mục ứng dụng ở vị trí có quyền ghi (không phải `Program Files`), khởi động qua `LaunchCinema.bat` để giữ working directory đúng. Đừng di chuyển riêng file `.exe`; cần toàn bộ thư mục cùng `app/`, `runtime/`, và `database/`.

**Chưa phải bộ cài MSI/EXE installer.** Đây là Windows app-image chứa launcher `.exe`. Cần thực thi script và kiểm thử trên Windows để xác nhận packaging; không thể kiểm thử Windows jpackage trong môi trường tạo file hiện tại.

## Việc còn cần hoàn thiện trước khi triển khai thực tế
- Chuyển CSV sang MySQL sau khi sửa orphaned foreign keys (một số phòng tham chiếu `cinema_id=2` không tồn tại).
- Đặt Booking và Ticket trong **một transaction** có khóa/unique constraint để tránh bán trùng ghế.
- Chuyển mật khẩu dạng văn bản sang hash phù hợp, hoàn thiện quyền truy cập và sao lưu dữ liệu.
- Khi phân phối ứng dụng nhiều máy, thiết kế cấu hình MySQL an toàn và nơi lưu cấu hình/ dữ liệu ghi được.
