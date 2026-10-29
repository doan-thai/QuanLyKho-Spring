# QUY TẮC KIẾN TRÚC & VIẾT CODE (SPRING CLOUD MICROSERVICES + MYSQL)

## 1. Tech Stack & Phiên bản
- **Java:** 21 (Sử dụng Virtual Threads `spring.threads.virtual.enabled=true` cho các service gọi I/O nhiều như Gateway, Report, AI).
- **Spring Boot:** 3.3.x & **Spring Cloud:** 2023.0.x.
- **Database:** MySQL 8.0 (`mysql-connector-j`). Sử dụng bảng mã `utf8mb4` và `utf8mb4_unicode_ci` để lưu tiếng Việt có dấu.
- **Build Tool:** Maven Multi-Module (1 Parent `pom.xml` quản lý `dependencyManagement`).
- **Giao tiếp nội bộ (Inter-service Communication):** Sử dụng `RestClient` (Spring 6.1+) hoặc `Spring Cloud OpenFeign`. Tự động chuyển tiếp (forward) Header `Authorization: Bearer ...` khi gọi từ `ai-service`, `report-service`, `supplier-service` sang `inventory-service`.

## 2. Quy chuẩn MySQL & JPA / Hibernate
- Không còn dùng kiểu `NVARCHAR` hay `DATETIME2` của SQL Server. Trên MySQL 8.0:
  - Chuỗi ký tự dùng `VARCHAR(n)` (mặc định hỗ trợ Unicode với `utf8mb4`).
  - Thời gian dùng `LocalDateTime` ánh xạ sang `DATETIME(6)`.
  - Tiền tệ/Điểm số dùng `BigDecimal` với `@Column(precision = 18, scale = 2)`.
- Đặt tên bảng và cột trong MySQL theo chuẩn `snake_case` (mặc định của `SpringPhysicalNamingStrategy`), ví dụ: bảng `users`, `products`, `import_receipts`, `export_receipts`, `stock_transactions`, `suppliers`.
- Dùng `@Version` (Optimistic Locking) trên `Product` để chống lỗi Race Condition (âm kho) khi xuất hàng đồng thời.

## 3. Quy tắc Tương thích 100% với Frontend React (`frontend/`)
1. **Cổng (Ports) cố định:**
   - `api-gateway`: `5000`
   - `auth-service`: `5001`
   - `inventory-service`: `5002`
   - `supplier-service`: `5003`
   - `report-service`: `5004`
   - `ai-service`: `5005`
2. **CORS tập trung tại Gateway:** Chỉ cấu hình CORS tại `api-gateway` (cho phép `http://localhost:5173`, `http://localhost:4173` với `allowCredentials: true`). Các service phía sau tắt CORS để tránh lỗi trùng lặp header `Access-Control-Allow-Origin`.
3. **JSON Contract (camelCase):** Giữ nguyên 100% tên trường JSON trả về như bản .NET cũ để không phải sửa bất kỳ file `.jsx` nào trong `frontend/`.
4. **JWT & Role:**
   - Dùng chung JWT Secret cấu hình qua biến môi trường `JWT_SECRET` (bắt buộc, tối thiểu 32 byte), `Issuer` (`QuanLyKho.AuthService`), `Audience` (`QuanLyKho.Client`). Không hardcode secret trong code/config.
   - Hỗ trợ đọc JWT từ cả Header `Authorization: Bearer <token>` VÀ Cookie `access_token`.
   - Giữ nguyên 3 tên Role phân biệt hoa thường: `"Admin"`, `"ThuKho"`, `"KeToan"`. Sử dụng `hasAuthority('Admin')` hoặc `hasAnyAuthority('Admin', 'ThuKho')` trong Spring Security (không tự động gắn tiền tố `ROLE_` để khớp 100% với claim trong JWT).

## 4. Quy tắc Viết Code
- Bắt buộc dùng **Constructor Injection** với `@RequiredArgsConstructor` (`private final`), không dùng `@Autowired` trên field.
- Sử dụng module `common-lib` để chia sẻ `JwtAuthenticationFilter`, `JwtUtils` và `GlobalExceptionHandler` cho các microservices, tránh lặp lại code.