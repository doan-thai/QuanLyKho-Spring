# ĐẶC TẢ KỸ THUẬT HỆ THỐNG SYNCSTOCK (SPRING CLOUD MICROSERVICES + MYSQL)

## 1. Kiến trúc Hạ tầng & Cơ sở dữ liệu MySQL
- **Container `mysql` (Port `3306:3306`):** Khởi chạy cùng script `scripts/init-mysql.sql` tạo sẵn 3 Databases:
  1. `auth_db` (dành riêng cho `auth-service`)
  2. `warehouse_db` (dành riêng cho `inventory-service`)
  3. `supplier_db` (dành riêng cho `supplier-service`)
- **Container `rabbitmq` (Port `5672`, `15672`):** Hạ tầng Message Broker sẵn sàng cho các tác vụ bất đồng bộ.

---

## 2. Đặc tả Chi tiết Từng Microservice

### 2.1. Module `common-lib` (Thư viện dùng chung nội bộ)
- Chứa `JwtTokenProvider` và `OncePerRequestFilter` (`JwtAuthenticationFilter`):
  - Trích xuất JWT từ Header `Authorization` hoặc Cookie `access_token`.
  - Xác thực chữ ký HMAC-SHA256, `issuer`, `audience`, `expiration`.
  - Đọc claim `userId`, `username`, `role` và nạp vào `SecurityContextHolder` với `SimpleGrantedAuthority(role)`.
- Chứa `GlobalExceptionHandler` (`@RestControllerAdvice`) chuẩn hóa phản hồi lỗi `400`, `401`, `403`, `404`, `500`.

---

### 2.2. Service 1: `api-gateway` (Port `5000`)
- **Công nghệ:** `spring-cloud-starter-gateway-mvc` (hoặc Reactive Gateway).
- **Cấu hình Routing (Thay thế `yarp.json`):**
  - `/api/auth/**` -> `http://localhost:5001` (Docker: `http://auth-service:8080`)
  - `/api/inventory/**` -> `http://localhost:5002` (Docker: `http://inventory-service:8080`)
  - `/api/supplier/**` -> `http://localhost:5003` (Docker: `http://supplier-service:8080`)
  - `/api/report/**` -> `http://localhost:5004` (Docker: `http://report-service:8080`)
  - `/api/chat/**` -> `http://localhost:5005` (Docker: `http://ai-service:8080`)
  - `/images/**` -> `http://localhost:5002` (Để hiển thị ảnh sản phẩm từ `inventory-service`).
- **CORS Config:** Cho phép origins `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:4173` (và biến môi trường `CORS_ALLOWED_ORIGINS`), methods `*`, headers `*`, `allowCredentials = true`.

---

### 2.3. Service 2: `auth-service` (Port `5001` - Database: `auth_db`)
- **Bảng `users` (Entity `User`):**
  - `id`: INT AUTO_INCREMENT (PK)
  - `username`: VARCHAR(50) UNIQUE NOT NULL
  - `password_hash`: VARCHAR(255) NOT NULL
  - `role`: VARCHAR(20) NOT NULL DEFAULT `'ThuKho'` (Validate: `^(Admin|ThuKho|KeToan)$`)
  - `is_active`: BOOLEAN NOT NULL DEFAULT `true`
- **API Endpoints:**
  1. `POST /api/auth/register` (Public):
     - Kiểm tra trùng `username`.
     - Nếu bảng `users` chưa có bất kỳ `Admin` nào -> Bắt buộc tài khoản đầu tiên đăng ký phải có `role = "Admin"`.
     - Nếu đã có `Admin` -> Chỉ cho phép đăng ký `role = "ThuKho"`.
     - Mã hóa password bằng `BCryptPasswordEncoder`.
     - Trả về `200 OK`: `{ "message": "Đăng ký thành công", "user": { "id", "username", "role", "isActive" } }`.
  2. `POST /api/auth/login` (Public):
     - Nếu `!user.isActive` -> Trả `401 { "message": "Tài khoản đã bị khóa." }`.
     - Nếu sai mật khẩu -> Tăng `FailedLoginCount` trong `HttpSession`, trả `401 { "message": "Sai username hoặc password", "failedLoginCountInSession": failed }`.
     - Nếu đúng -> Reset `FailedLoginCount = 0`, sinh JWT (chứa claims: `sub`, `unique_name`, `userId`, `username`, `role`), set HttpOnly Cookie `access_token` (maxAge = 60 phút) và trả `200 OK`:
       `{ "token", "username", "role", "failedLoginCountInSession": 0 }`.
  3. `GET /api/auth/admin` (Authority: `Admin`), `GET /api/auth/warehouse` (`Admin, ThuKho`), `GET /api/auth/report` (`Admin, KeToan`): Các API probe kiểm tra quyền.
  4. **Quản lý User (`/api/auth/users` - Chỉ `Admin`):**
     - `GET /api/auth/users?role=...`: Danh sách user sắp xếp theo `username`.
     - `POST /api/auth/users`: Tạo user mới với role `ThuKho` hoặc `KeToan`, trả `201 Created`.
     - `PUT /api/auth/users/{id}/role`: Cập nhật role (`ThuKho` | `KeToan`).
     - `PATCH /api/auth/users/{id}/status`: Khóa/Mở khóa tài khoản (`{ "isActive": boolean }`).
     - `DELETE /api/auth/users/{id}`: Xóa user (Chặn không cho phép xóa chính `userId` đang đăng nhập), trả `204 No Content`.

---

### 2.4. Service 3: `inventory-service` (Port `5002` - Database: `warehouse_db`)
- **Các Bảng MySQL (Entities):**
  1. `categories`: `id` (INT PK), `name` (VARCHAR(100) NOT NULL).
  2. `products`: `id` (INT PK), `name` (VARCHAR(255) NOT NULL), `category` (VARCHAR(100) NOT NULL), `unit` (VARCHAR(50) NOT NULL), `stock` (INT NOT NULL DEFAULT 0), `min_stock` (INT NOT NULL DEFAULT 10), `image_url` (VARCHAR(500)), `version` (BIGINT `@Version`).
  3. `import_receipts`: `id` (INT PK), `code` (VARCHAR(50)), `note` (VARCHAR(500)), `supplier_id` (INT NULL), `import_date` (DATETIME(6) NOT NULL).
  4. `export_receipts`: `id` (INT PK), `code` (VARCHAR(50)), `reason` (VARCHAR(500)), `export_date` (DATETIME(6) NOT NULL).
  5. `stock_transactions`: `id` (INT PK), `product_id` (INT NOT NULL), `transaction_type` (VARCHAR(20) NOT NULL: `"IMPORT"` | `"EXPORT"`), `quantity` (INT NOT NULL), `unit_price` (DECIMAL(18,2) NOT NULL DEFAULT 0), `supplier_id` (INT NULL), `transaction_date` (DATETIME(6) NOT NULL), `note` (VARCHAR(500)).
- **Data Seeding (`CommandLineRunner`):** Nếu bảng trống, tự động seed 3 `Categories` (`Điện tử`, `Gia dụng`, `Thực phẩm`) và 20 `Products` mẫu y hệt bản .NET.
- **API Endpoints:**
  1. `GET /api/inventory/product?searchName=...&category=...&pageNumber=1&pageSize=10`:
     - Trả về đúng cấu trúc phân trang: `{ "totalRecords", "pageNumber", "pageSize", "totalPages", "data": [...] }`.
  2. `GET /api/inventory/product/{id}`: Chi tiết sản phẩm.
  3. `POST /api/inventory/product` (`Admin, ThuKho`): Thêm sản phẩm, trả `201 Created`.
  4. `PUT /api/inventory/product/{id}` (`Admin, ThuKho`): Sửa sản phẩm, trả `204 No Content`.
  5. `DELETE /api/inventory/product/{id}` (`Admin, ThuKho`): Xóa sản phẩm, trả `204 No Content`.
  6. `POST /api/inventory/product/upload` (`Admin, ThuKho`, `multipart/form-data`): Lưu file ảnh (`.jpg, .jpeg, .png, .webp`, tối đa 5MB) vào thư mục `uploads/images/`, trả `{ "url": "/images/...", "message": "Upload ảnh thành công!" }`.
  7. `GET /api/inventory/stock`: Trả danh sách `[{ "id", "name", "category", "stock", "minStock", "isLowStock": (stock < minStock) }]`.
  8. `GET /api/inventory/history?limit=200`: Join `stock_transactions` và `products`, sắp xếp `transaction_date DESC`, trả danh sách gồm:
     `{ "id", "productId", "productName", "quantity", "unitPrice", "totalAmount", "supplierId", "type", "transactionType", "date", "transactionDate", "note" }`.
  9. `POST /api/inventory/import` (`Admin, ThuKho` - `@Transactional`):
     - Nhận `{ "code", "note", "supplierId", "items": [{ "productId", "quantity", "price" }] }`.
     - Lưu `ImportReceipt`, cộng `product.stock`, lưu `StockTransaction` (`IMPORT`). Trả `{ "message": "Nhập kho thành công!", "receiptId": ... }`.
  10. `POST /api/inventory/export` (`Admin, ThuKho` - `@Transactional`):
      - Nhận `{ "code", "note", "reason", "items": [{ "productId", "quantity", "price" }] }`.
      - Kiểm tra nếu `product.stock < item.quantity` -> Ném lỗi `400 Bad Request` kèm thông báo `"Không đủ hàng: '<Tên SP>'. Tồn: <stock>, Yêu cầu: <quantity>."` và Rollback.
      - Trừ `product.stock`, lưu `StockTransaction` (`EXPORT`). Trả `{ "message": "Xuất kho thành công!", "receiptId": ... }`.

---

### 2.5. Service 4: `supplier-service` (Port `5003` - Database: `supplier_db`)
- **Bảng `suppliers` (Entity `SupplierInfo`):**
  - `id` (INT PK), `name` (VARCHAR(200) NOT NULL, Indexed), `contact_person` (VARCHAR(50)), `phone` (VARCHAR(20) - Regex `^(0\d{9}|\+84\d{9})$`), `email` (VARCHAR(150)), `address` (VARCHAR(300)), `is_active` (BOOLEAN DEFAULT true), `created_at` (DATETIME(6)), `total_transactions` (INT DEFAULT 0), `total_import_value` (DECIMAL(18,2) DEFAULT 0), `reliability_score` (DECIMAL(18,2) DEFAULT 0), `last_transaction_at` (DATETIME(6) NULL).
- **Seed Data:** Seed sẵn 2 nhà cung cấp mẫu (`Công ty ABC` - Hà Nội, `Công ty XYZ` - TP HCM).
- **API Endpoints:**
  - `GET /api/supplier?search=...`: Tìm kiếm và sắp xếp theo `name`.
  - `GET /api/supplier/{id}`: Chi tiết nhà cung cấp.
  - `POST /api/supplier` (`Admin, ThuKho`): Kiểm tra trùng tên, lưu mới, trả `201 Created`.
  - `PUT /api/supplier/{id}` (`Admin, ThuKho`): Cập nhật, trả `200 OK` kèm object supplier.
  - `DELETE /api/supplier/{id}` (`Admin, ThuKho`): Xóa, trả `204 No Content`.
  - `GET /api/supplier/analytics?limit=2000` (`Admin, ThuKho, KeToan`): Dùng `RestClient` gọi sang `inventory-service` (`GET /api/inventory/history?limit=...` kèm Bearer Token hiện tại) để tính toán `totalTransactions`, `totalImportValue`, `reliabilityScore` và cập nhật vào DB.

---

### 2.6. Service 5: `report-service` (Port `5004` - Stateless / Gọi `inventory-service`)
- **Quyền truy cập:** `@PreAuthorize("hasAnyAuthority('Admin', 'KeToan')")`.
- **Giao tiếp:** Dùng `RestClient` gọi `inventory-service` (`/api/inventory/stock` và `/api/inventory/history`) có đính kèm Bearer Token của request gốc.
- **API Endpoints:**
  1. `GET /api/report/stock`: Tổng hợp số lượng sản phẩm, tổng tồn kho, số sản phẩm sắp hết hàng (`isLowStock`).
  2. `GET /api/report/transactions?from=...&to=...&type=...&limit=200`: Lọc lịch sử giao dịch theo ngày và loại.
  3. `GET /api/report/business?from=...&to=...&top=10`: Tính doanh thu xuất kho (`EXPORT`), chi phí nhập kho (`IMPORT`), lợi nhuận gộp và Top sản phẩm xuất nhiều nhất.
  4. `GET /api/report/export/excel?from=...&to=...&reportType=inventory|inward|outward&format=xlsx|csv`: Dùng **Apache POI** (`XSSFWorkbook`) tạo file `.xlsx` hoặc xuất `.csv` trả về dạng `byte[]` với `Content-Disposition: attachment`.

---

### 2.7. Service 6: `ai-service` (Port `5005` - Stateless / Gọi `inventory-service` & Groq API)
- **Giao tiếp:**
  1. Dùng `RestClient` gọi `GET /api/inventory/stock` (truyền tiếp Bearer Token từ Header hoặc Cookie `access_token`).
  2. Dùng `RestClient` gọi `POST https://api.groq.com/openai/v1/chat/completions` với model `llama-3.1-8b-instant` và `Groq__ApiKey`.
- **API Endpoints:**
  - `POST /api/chat` & `POST /api/chat/ask`: Nhận `{ "message": "..." }`, trả `{ "answer": "..." }`.
  - `POST /api/chat/summary`: Tóm tắt tình hình kho 5-7 dòng, trả `{ "answer": "..." }`.