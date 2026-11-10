# Cinema Ticket Booking — Stage 14A (Spring Boot online API)

## Scope / Phạm vi thực tế

* `cinema-desktop/`: **nguyên bản Stage 13**, giữ đầy đủ các màn hình và chức năng cũ; thêm lớp `remote/CinemaApiClient.java` để chuẩn bị Stage 14B. **JavaFX Stage 13 vẫn kết nối trực tiếp MySQL**, chưa phải client online an toàn. **KHÔNG cài/phát hành bản desktop này cho người dùng ngoài hệ thống**.
* `cinema-server/`: Spring Boot 3.5.16 / Java 21 REST API: đăng nhập bằng hash PBKDF2 của Stage 13, token phiên trong bộ nhớ, GET catalog (9 bảng theo quyền), Admin CRUD Movies, Actuator health. Không có endpoint đặt/hủy vé ở Stage 14A; các dịch vụ ghi khác đang còn ở desktop Stage 13.
* **Chưa tích hợp WebSocket**, chưa hoàn tất client đồng bộ. Stage 14B phải chuyển *tất cả* đọc/ghi sang API, bắt đầu bằng login/movie, rồi admin CRUD, cuối cùng booking transaction server-side; sau đó 14C mới đóng gói máy khách.

## 1. Chuẩn bị MySQL

Sao lưu `cinema_booking` đang dùng (mysqldump hoặc Workbench Data Export). Schema giữ nguyên 9 bảng ở `cinema-desktop/sql/schema.sql`. Đảm bảo đã chạy stage10 unique guard và không có ghế đặt trùng. API *không tự xóa hoặc import dữ liệu*.

**Quan trọng:** Thử API trên **database sao chép/test** trước. Tài khoản cũ có mật khẩu plaintext (LEGACY) sẽ bị từ chối bởi API cho đến khi được chuyển sang PBKDF2 thông qua login Stage 11–13 (trên môi trường tin cậy), hoặc quy trình reset password an toàn. Không bật fallback plaintext trên Internet.

## 2. Chạy backend cục bộ (PowerShell, IntelliJ Terminal)

```powershell
cd cinema-server
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/cinema_booking"
$env:DB_USER = "cinema_app"
$env:DB_PASSWORD = "YOUR_DB_PASSWORD"
mvn spring-boot:run
```

Bạn cần tạo user `cinema_app` trong Workbench và cấp quyền SELECT, INSERT, UPDATE, DELETE trên `cinema_booking.*`; tuyệt đối không đưa `root` hay mật khẩu DB vào app JavaFX.

Không có tài khoản admin hard-code. Dùng tài khoản ADMIN đã lưu dạng hash trong database Stage 11–13.

Test:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
$body = @{username="admin";password="YOUR_ADMIN_PASSWORD"} | ConvertTo-Json
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body $body
$headers = @{Authorization="Bearer $($r.token)"}
Invoke-RestMethod -Uri http://localhost:8080/api/movies -Headers $headers
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/movies -Headers $headers -ContentType 'application/json' -Body '{"title":"Test API Movie","duration":110,"genre":"Action","description":"Stage 14A test"}'
```

POST/PUT/DELETE movies chỉ ADMIN; GET movies yêu cầu đăng nhập. Nếu muốn kiểm tra từ máy khác, trước hết đưa API ra HTTPS qua reverse proxy, không public trực tiếp MySQL hoặc port 8080.

## 3. Triển khai online

Có hai lựa chọn: (1) VPS chạy backend + MySQL trong mạng Docker nội bộ, hoặc (2) hosting container backend + MySQL được quản lý. `compose.yaml` minh họa phương án VPS, **không tự mở public 3306/8080**. Reverse proxy (Caddy/Nginx) ở máy chủ cung cấp HTTPS domain `https://api.example.com` và proxy đến `127.0.0.1:8080`. DNS, TLS, backup, firewall, cập nhật bảo mật do người vận hành cấu hình. 

Nếu chọn Docker VPS, chạy `cp .env.example .env`, thay giá trị bằng mật khẩu mạnh rồi `docker compose up -d --build`; database mới cần import `schema.sql` và dữ liệu từ bản sao lưu **một lần**, không chạy importer CSV tùy tiện. Để giữ dữ liệu MySQL hiện tại: backup/export từ máy nhà rồi restore vào DB online. Không chuyển file `.env` hay backup lên máy client. **Không mở cổng 3306 ra Internet.**

## 4. API

| Endpoint | Quyền | Ý nghĩa |
|---|---|---|
| `GET /api/health` | Public | Liveness đơn giản, không kiểm tra DB |
| `POST /api/auth/login` | Public | Nhận username/password, trả token 8 giờ |
| `GET /api/auth/me` | Login | Thông tin phiên |
| `POST /api/auth/logout` | Login | Thu hồi token |
| `GET /api/movies`, `/api/movies/{id}` | Login | Xem phim |
| `POST /api/movies` | ADMIN | Thêm phim |
| `PUT /api/movies/{id}` | ADMIN | Sửa phim |
| `DELETE /api/movies/{id}` | ADMIN | Xóa phim nếu không có suất chiếu |
| `GET /api/cinemas`, `/api/rooms`, `/api/seats`, `/api/showtimes` | Login | Danh mục, read-only |
| `GET /api/customers`, `/api/bookings`, `/api/tickets` | ADMIN | Dữ liệu nhạy cảm — chỉ Admin |

Response token phiên được lưu **trong RAM**, không chia sẻ giữa nhiều replica, mất khi restart. Stage 14B/production cần shared session store hoặc token auth production và rate-limit brute-force, audit logging, migrations, monitoring.

## 5. Cần hoàn thiện trước khi phát hành

- Chuyển JavaFX `LoginController`, `DashboardController` và tất cả các Controller/Service/Repository sang HTTP API. **Gỡ MySQL JDBC, DB URL và DB password khỏi desktop distribution**.
- Server-side booking transactional + unique active-seat DB constraint, ownership checks và role checks ở mọi API.
- WebSocket/SSE hoặc polling có version để các máy tự làm mới dữ liệu sau cập nhật; Stage 14A mới đồng bộ dữ liệu *trong cùng DB* khi **gọi GET lại**, không tự push màn hình.
- Secure HTTPS (reverse proxy), rate limiting cho login, backup MySQL và kiểm thử thực tế nhiều máy trước khi phát hành.

