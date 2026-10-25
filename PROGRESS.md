# TIẾN ĐỘ DỰ ÁN SYNCSTOCK (SPRING CLOUD MICROSERVICES + MYSQL)

## 1. Trạng thái hiện tại (Current Status)
- **Kiến trúc:** Maven Multi-Module (1 Parent + `common-lib` + 6 Microservices) + MySQL 8.0.
- **Frontend (`frontend/`):** Giữ nguyên 100%.
- **Task đang thực hiện:** **Phase 1** (Khởi tạo Parent POM, `common-lib` và cấu hình MySQL Docker).

## 2. Nhật ký Kỹ thuật & Lưu ý Bàn giao (Technical Gotchas)
- **MySQL Init Script:** File `scripts/init-mysql.sql` sẽ tạo sẵn 3 DB (`auth_db`, `warehouse_db`, `supplier_db`). Chuỗi kết nối MySQL nhớ thêm `?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`.
- **Biến môi trường:** Hỗ trợ đọc cả định dạng chuẩn Spring (`SPRING_DATASOURCE_URL`, `GROQ_API_KEY`) để chạy mượt trên `docker-compose.yml`.

## 3. Danh sách Công việc (Task Checklist)

### PHASE 1: Khởi tạo Maven Multi-Module, `common-lib` & Hạ tầng MySQL
- [ ] **Task 1.1:** Tạo file `pom.xml` cha ở thư mục gốc khai báo các modules: `common-lib`, `api-gateway`, `auth-service`, `inventory-service`, `supplier-service`, `report-service`, `ai-service`.
- [ ] **Task 1.2:** Tạo file `scripts/init-mysql.sql` tạo 3 databases (`auth_db`, `warehouse_db`, `supplier_db`).
- [ ] **Task 1.3:** Xây dựng module `common-lib` gồm: `JwtUtils`, `JwtAuthenticationFilter` (đọc Header + Cookie `access_token`), `GlobalExceptionHandler` và các hằng số cấu hình dùng chung.

### PHASE 2: Xây dựng `api-gateway` (Port 5000)
- [ ] **Task 2.1:** Tạo module `api-gateway` với `spring-cloud-starter-gateway-mvc`.
- [ ] **Task 2.2:** Cấu hình `application.yml` định tuyến 5 cụm API (`/api/auth/**`, `/api/inventory/**`, `/api/supplier/**`, `/api/report/**`, `/api/chat/**`, `/images/**`) và cấu hình Global CORS cho Frontend React (`localhost:5173`).

### PHASE 3: Xây dựng `auth-service` (Port 5001 - `auth_db`)
- [ ] **Task 3.1:** Tạo module `auth-service` kết nối MySQL `auth_db`, tạo `User` Entity, `UserRepository` và các DTOs (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `CreateManagedUserRequest`,...).
- [ ] **Task 3.2:** Viết `AuthController` (`/api/auth/register`, `/api/auth/login` có đếm lỗi Session & gắn Cookie `access_token`) và `UserManagementController` (`/api/auth/users` CRUD cho Admin).

### PHASE 4: Xây dựng `inventory-service` (Port 5002 - `warehouse_db`)
- [ ] **Task 4.1:** Tạo module `inventory-service` kết nối MySQL `warehouse_db`, tạo 5 Entities (`Category`, `Product` có `@Version`, `ImportReceipt`, `ExportReceipt`, `StockTransaction`) và các Repositories.
- [ ] **Task 4.2:** Viết `DataSeeder` tự động nạp 3 danh mục và 20 sản phẩm mẫu vào `warehouse_db` khi khởi chạy lần đầu.
- [ ] **Task 4.3:** Viết `ProductController` (`/api/inventory/product` phân trang, CRUD, upload ảnh `/api/inventory/product/upload` và cấu hình static resource `/images/**`).
- [ ] **Task 4.4:** Viết `InventoryController` (`/api/inventory/stock`, `/api/inventory/history`, `/api/inventory/import`, `/api/inventory/export` có `@Transactional` và kiểm tra âm kho).

### PHASE 5: Xây dựng `supplier-service` (Port 5003 - `supplier_db`)
- [ ] **Task 5.1:** Tạo module `supplier-service` kết nối MySQL `supplier_db`, tạo `SupplierInfo` Entity, `SupplierRepository`, DTOs và seed 2 nhà cung cấp mẫu.
- [ ] **Task 5.2:** Viết `InventoryAnalyticsClient` (gọi `inventory-service`) và `SupplierController` (`/api/supplier` CRUD + `/api/supplier/analytics`).

### PHASE 6: Xây dựng `report-service` (Port 5004) & `ai-service` (Port 5005)
- [ ] **Task 6.1:** Tạo module `report-service`, viết `InventoryReportClient` và `ReportController` (`/api/report/stock`, `/api/report/transactions`, `/api/report/business`, `/api/report/export/excel` dùng Apache POI).
- [ ] **Task 6.2:** Tạo module `ai-service`, viết `InventoryClient`, `AiService` (gọi Groq API) và `ChatController` (`/api/chat/ask`, `/api/chat/summary`).

### PHASE 7: Đóng gói Docker & Cập nhật `docker-compose.yml`
- [ ] **Task 7.1:** Tạo `Dockerfile` đa tầng (Multi-stage build với Maven & Eclipse Temurin JRE 21) cho 6 services.
- [ ] **Task 7.2:** Cập nhật `docker-compose.yml` thay thế SQL Server bằng `mysql:8.0` (mount `scripts/init-mysql.sql` vào `/docker-entrypoint-initdb.d/`) và cấu hình biến môi trường cho cả 6 services.