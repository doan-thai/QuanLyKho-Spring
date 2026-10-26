# TIẾN ĐỘ DỰ ÁN SYNCSTOCK (SPRING CLOUD MICROSERVICES + MYSQL)

## 1. Trạng thái hiện tại (Current Status)
- **Kiến trúc:** Maven Multi-Module (1 Parent + `common-lib` + 6 Microservices) + MySQL 8.0 + RabbitMQ + Docker Compose.
- **Frontend (`frontend/`):** Giữ nguyên 100%, tích hợp hoàn hảo qua API Gateway (Port 5000).
- **Task đang thực hiện:** Đã hoàn thành 100% toàn bộ **7 Phases** của dự án. Hệ thống đã được đóng gói Dockerfile đa tầng tối ưu (Java 21 Alpine) và cấu hình Docker Compose đồng bộ, sẵn sàng triển khai sản xuất hoặc chạy local.

## 2. Nhật ký Kỹ thuật & Lưu ý Bàn giao (Technical Gotchas)
- **MySQL Init Script:** File `scripts/init-mysql.sql` sẽ tạo sẵn 3 DB (`auth_db`, `warehouse_db`, `supplier_db`). Chuỗi kết nối MySQL nhớ thêm `?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`.
- **Biến môi trường:** Hỗ trợ đọc cả định dạng chuẩn Spring (`SPRING_DATASOURCE_URL`, `GROQ_API_KEY`) để chạy mượt trên `docker-compose.yml`.
- **Maven Wrapper:** Đã tích hợp `mvnw` và `mvnw.cmd` (Maven 3.9.16) tại thư mục gốc, biên dịch đồng bộ Java 21 trên toàn bộ 8 modules.
- **Gateway & CORS:** `api-gateway` sử dụng `spring-cloud-starter-gateway-mvc` với Virtual Threads, bộ lọc `CorsFilter` mức ưu tiên cao nhất `HIGHEST_PRECEDENCE` hỗ trợ credentials cho `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:4173` và biến môi trường `CORS_ALLOWED_ORIGINS`.
- **Auth Service & Bảo mật:**
  - `POST /api/auth/register`: Nếu DB chưa có Admin nào, tài khoản đầu tiên bắt buộc nhận role `"Admin"`. Khi đã có Admin, tự đăng ký chỉ cho phép role `"ThuKho"`.
  - `POST /api/auth/login`: Kiểm tra `isActive` (khóa trả 401). Sai mật khẩu tăng biến đếm `FailedLoginCount` trong `HttpSession` và trả 401 kèm `failedLoginCountInSession`. Khi thành công, reset biến đếm, cấp JWT đầy đủ claims (`sub`, `unique_name`, `userId`, `username`, `role`), đính kèm HttpOnly Cookie `access_token` (maxAge 60 phút) và trả JSON `{ token, username, role, failedLoginCountInSession: 0 }`.
  - `UserManagementController` (`/api/auth/users`): Chỉ dành cho `"Admin"`, cung cấp đầy đủ API lấy danh sách (lọc role, sắp xếp username), tạo mới (`ThuKho`/`KeToan`), đổi role, khóa/mở khóa trạng thái, và xóa user (chặn không cho Admin tự xóa chính mình).
- **Inventory Service & Tương thích Frontend:**
  - `Product` Entity áp dụng `@Version` (Optimistic Locking) ngăn ngừa Race Condition âm kho khi nhiều client xuất hàng đồng thời.
  - `DataSeeder` (`CommandLineRunner`): Tự động nạp 3 danh mục (`Điện tử`, `Gia dụng`, `Thực phẩm`) và 20 sản phẩm mẫu vào MySQL `warehouse_db` khi bảng trống.
  - `GET /api/inventory/product`: Hỗ trợ phân trang và lọc `searchName`, `category`, trả về đúng chuẩn JSON contract `{ totalRecords, pageNumber, pageSize, totalPages, data }`.
  - `POST /api/inventory/product/upload`: Kiểm tra định dạng (`.jpg, .jpeg, .png, .webp`), MIME type và kích thước tối đa 5MB, lưu vào `uploads/images/`, đồng thời `WebMvcConfig` phục vụ static resource tại `/images/**`.
  - `GET /api/inventory/stock`: Trả về danh sách kèm trường `isLowStock` (`stock < minStock`).
  - `GET /api/inventory/history?limit=200`: JPQL Join `stock_transactions` và `products`, trả về đầy đủ cả cặp trường `type`/`transactionType` và `date`/`transactionDate` cùng `productName`, `unitPrice`, `totalAmount`, `supplierId`, `note`.
  - `POST /api/inventory/import` & `POST /api/inventory/export`: Dùng `@Transactional`. Với xuất kho, nếu `product.stock < item.quantity` lập tức ném `ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đủ hàng: '<Tên SP>'. Tồn: <stock>, Yêu cầu: <quantity>.")` và rollback toàn bộ giao dịch.
- **Supplier Service & Tính toán Analytics (Phase 5):**
  - Entity `SupplierInfo` (bảng `suppliers`): Kiểm tra Regex số điện thoại Việt Nam `^(0\d{9}|\+84\d{9})$`, chặn trùng tên nhà cung cấp (`existsByNameIgnoreCase` / `existsByNameIgnoreCaseAndIdNot`), tự động seed 2 nhà cung cấp mẫu (`Công ty ABC` - Hà Nội, `Công ty XYZ` - TP HCM) qua `DataSeeder`.
  - CRUD `/api/supplier`: GET danh sách (hỗ trợ lọc từ khóa `name`, `contactPerson`, `phone`, `email`) và xem chi tiết cho người dùng đã đăng nhập; Thêm mới (`POST`), cập nhật (`PUT`) và xóa (`DELETE`) được kiểm soát chặt chẽ chỉ dành cho quyền `Admin` và `ThuKho`.
  - Endpoint Analytics `GET /api/supplier/analytics?limit=2000` (dành cho `Admin`, `ThuKho`, `KeToan`):
    - Dùng Spring 6.1 `RestClient` trong `InventoryAnalyticsClient` gọi sang `inventory-service` (`GET /api/inventory/history?limit=...`), tự động trích xuất và chuyển tiếp (forward) Bearer Token từ Header `Authorization` hoặc Cookie `access_token` của request gốc.
    - Lọc giao dịch `IMPORT` theo `supplierId`, nhóm dữ liệu và cập nhật `totalTransactions`, `totalImportValue`, `lastTransactionAt`.
    - Chấm điểm `reliabilityScore` (thang điểm 0 - 100, làm tròn 2 chữ số thập phân) dựa trên:
      - Tần suất giao dịch `activityScore = min(50, count * 2)`
      - Giá trị nhập kho `valueScore = min(30, totalValue / 100,000,000 * 30)`
      - Độ gần đây của giao dịch `recencyScore` (<= 30 ngày: 20đ, <= 90 ngày: 12đ, > 90 ngày: 6đ)
      - Trọng số trạng thái hợp tác (`isActive == false` thì giảm 50% điểm; không có giao dịch thì nhận 50đ nếu Active hoặc 25đ nếu Inactive).
    - Cập nhật kết quả vào MySQL `supplier_db` và trả về danh sách đối tác.
- **Report Service & Xuất Báo cáo Excel/CSV (Phase 6 - Port 5004):**
  - Quyền truy cập: Kiểm soát nghiêm ngặt chỉ cho phép `Admin` và `KeToan` (`@PreAuthorize("hasAnyAuthority('Admin', 'KeToan')")`).
  - Cơ chế tự động forward Bearer Token: `RestClientConfig` đăng ký `ClientHttpRequestInterceptor` tự động trích xuất token từ Header `Authorization` hoặc Cookie `access_token` từ request gốc để gửi sang `inventory-service`.
  - `GET /api/report/stock`: Tổng hợp `totalProducts`, `totalStock`, `lowStockCount` (và alias `alertsCount`), kèm danh sách đầy đủ sản phẩm.
  - `GET /api/report/transactions?from=...&to=...&type=...&limit=200`: Lọc giao dịch theo khoảng ngày và loại (`IMPORT`/`EXPORT`), giữ nguyên cấu trúc JSON tương thích Frontend React.
  - `GET /api/report/business?from=...&to=...&top=10`: Tính doanh thu bán hàng `totalRevenue`, chi phí nhập kho `totalCost`, lợi nhuận gộp `grossProfit` (`totalRevenue - totalCost`), số lượt nhập/xuất, tổng sản lượng và Top sản phẩm bán chạy nhất (`topSellingProducts`).
  - `GET /api/report/export/excel?from=...&to=...&reportType=inventory|inward|outward&format=xlsx|csv`:
    - Với `.xlsx`: Dùng Apache POI `XSSFWorkbook` tạo bảng tính chuyên nghiệp, định dạng header màu xanh, in đậm, căn giữa, kẻ viền và tự động giãn độ rộng cột (`autoSizeColumn`).
    - Với `.csv`: Xuất dữ liệu kèm ký tự UTF-8 BOM (`\uEF\uBB\uBF`) giúp mở trên Microsoft Excel Windows không bao giờ bị lỗi font tiếng Việt.
- **AI Service & Trợ lý Kho Groq LLM (Phase 6 - Port 5005):**
  - Quyền truy cập: Yêu cầu xác thực JWT cho tất cả nhân sự đăng nhập hệ thống (`Admin`, `ThuKho`, `KeToan`).
  - Tích hợp Groq API: Sử dụng mô hình `llama-3.1-8b-instant` qua RestClient với cấu hình linh hoạt qua biến môi trường (`GROQ_API_KEY` / `Groq__ApiKey`, `GROQ_MODEL`, `GROQ_BASE_URL`).
  - Dữ liệu ngữ cảnh thời gian thực: Tự động gọi `inventory-service` (`GET /api/inventory/stock` kèm Bearer Token forwarded) để nạp dữ liệu tồn kho thực tế, số lượng và các cảnh báo thiếu hàng vào System Prompt tiếng Việt của AI.
  - Cung cấp đầy đủ 3 endpoints:
    - `POST /api/chat` & `POST /api/chat/ask`: Nhận `{ "message": "..." }`, giải đáp thắc mắc về tồn kho và trả lời `{ "answer": "..." }`.
    - `POST /api/chat/summary`: Tự động phân tích và tạo tóm tắt tình hình kho 5 - 7 dòng súc tích, kèm cảnh báo mặt hàng thiếu và khuyến nghị điều phối.
  - Khả năng chịu lỗi cao (Intelligent Fallback): Khi chưa cấu hình API key hoặc mất kết nối mạng ngoài tới Groq, service tự động phân tích câu hỏi bằng engine quy tắc cục bộ dựa trên dữ liệu kho thực tế để trả lời, đảm bảo hệ thống không bao giờ gặp lỗi 500.
- **Đóng gói Docker & Orchestration (Phase 7):**
  - Dockerfile Multi-stage Build: Tạo 6 `Dockerfile` độc lập tại từng thư mục dịch vụ (`api-gateway`, `auth-service`, `inventory-service`, `supplier-service`, `report-service`, `ai-service`). Tầng builder dùng `maven:3.9.9-eclipse-temurin-21-alpine` tối ưu layer caching (copy các file `pom.xml` trước để giải quyết reactor module), tầng runtime dùng `eclipse-temurin:21-jre-alpine` chạy dưới user bảo mật `appuser`, phân quyền thư mục lưu trữ ảnh `uploads/images` cho `inventory-service`.
  - Docker Compose hoàn chỉnh (`docker-compose.yml`):
    - Khởi chạy `mysql:8.0` mount script `scripts/init-mysql.sql` vào `/docker-entrypoint-initdb.d/` tự động khởi tạo 3 cơ sở dữ liệu (`auth_db`, `warehouse_db`, `supplier_db`), có healthcheck `mysqladmin ping`.
    - Khởi chạy `rabbitmq:3-management` phục vụ message broker, có healthcheck `rabbitmq-diagnostics ping`.
    - Khởi chạy 6 microservices ánh xạ chính xác cổng host 5000 -> 5005 vào cổng nội bộ 8080 của container (`SERVER_PORT=8080`), cấu hình đầy đủ biến môi trường kết nối MySQL, mạng nội bộ `syncstock-network` (`http://auth-service:8080`, `http://inventory-service:8080`,...), và đọc `Groq__ApiKey` từ file `.env`.
    - Tích hợp healthcheck Spring Boot Actuator qua `wget` kiểm tra `/actuator/health` cho tất cả microservices.
  - Tài liệu hướng dẫn (`README.md`): Cung cấp tài liệu hoàn chỉnh gồm sơ đồ kiến trúc Mermaid, ma trận phân quyền, hướng dẫn chi tiết cách chạy bằng Docker Compose và cách chạy local kết hợp Frontend React (`frontend/`).

## 3. Danh sách Công việc (Task Checklist)

### PHASE 1: Khởi tạo Maven Multi-Module, `common-lib` & Hạ tầng MySQL
- [x] **Task 1.1:** Tạo file `pom.xml` cha ở thư mục gốc khai báo các modules: `common-lib`, `api-gateway`, `auth-service`, `inventory-service`, `supplier-service`, `report-service`, `ai-service`.
- [x] **Task 1.2:** Tạo file `scripts/init-mysql.sql` tạo 3 databases (`auth_db`, `warehouse_db`, `supplier_db`).
- [x] **Task 1.3:** Xây dựng module `common-lib` gồm: `JwtUtils`, `JwtAuthenticationFilter` (đọc Header + Cookie `access_token`), `GlobalExceptionHandler` và các hằng số cấu hình dùng chung.

### PHASE 2: Xây dựng `api-gateway` (Port 5000)
- [x] **Task 2.1:** Tạo module `api-gateway` với `spring-cloud-starter-gateway-mvc`.
- [x] **Task 2.2:** Cấu hình `application.yml` định tuyến 5 cụm API (`/api/auth/**`, `/api/inventory/**`, `/api/supplier/**`, `/api/report/**`, `/api/chat/**`, `/images/**`) và cấu hình Global CORS cho Frontend React (`localhost:5173`).

### PHASE 3: Xây dựng `auth-service` (Port 5001 - `auth_db`)
- [x] **Task 3.1:** Tạo module `auth-service` kết nối MySQL `auth_db`, tạo `User` Entity, `UserRepository` và các DTOs (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `CreateManagedUserRequest`,...).
- [x] **Task 3.2:** Viết `AuthController` (`/api/auth/register`, `/api/auth/login` có đếm lỗi Session & gắn Cookie `access_token`) và `UserManagementController` (`/api/auth/users` CRUD cho Admin).

### PHASE 4: Xây dựng `inventory-service` (Port 5002 - `warehouse_db`)
- [x] **Task 4.1:** Tạo module `inventory-service` kết nối MySQL `warehouse_db`, tạo 5 Entities (`Category`, `Product` có `@Version`, `ImportReceipt`, `ExportReceipt`, `StockTransaction`) và các Repositories.
- [x] **Task 4.2:** Viết `DataSeeder` tự động nạp 3 danh mục và 20 sản phẩm mẫu vào `warehouse_db` khi khởi chạy lần đầu.
- [x] **Task 4.3:** Viết `ProductController` (`/api/inventory/product` phân trang, CRUD, upload ảnh `/api/inventory/product/upload` và cấu hình static resource `/images/**`).
- [x] **Task 4.4:** Viết `InventoryController` (`/api/inventory/stock`, `/api/inventory/history`, `/api/inventory/import`, `/api/inventory/export` có `@Transactional` và kiểm tra âm kho).

### PHASE 5: Xây dựng `supplier-service` (Port 5003 - `supplier_db`)
- [x] **Task 5.1:** Tạo module `supplier-service` kết nối MySQL `supplier_db`, tạo `SupplierInfo` Entity, `SupplierRepository`, DTOs và seed 2 nhà cung cấp mẫu.
- [x] **Task 5.2:** Viết `InventoryAnalyticsClient` (gọi `inventory-service`) và `SupplierController` (`/api/supplier` CRUD + `/api/supplier/analytics`).

### PHASE 6: Xây dựng `report-service` (Port 5004) & `ai-service` (Port 5005)
- [x] **Task 6.1:** Tạo module `report-service`, viết `InventoryReportClient` và `ReportController` (`/api/report/stock`, `/api/report/transactions`, `/api/report/business`, `/api/report/export/excel` dùng Apache POI).
- [x] **Task 6.2:** Tạo module `ai-service`, viết `InventoryClient`, `AiService` (gọi Groq API) và `ChatController` (`/api/chat`, `/api/chat/ask`, `/api/chat/summary`).

### PHASE 7: Đóng gói Docker & Cập nhật `docker-compose.yml`
- [x] **Task 7.1:** Tạo `Dockerfile` đa tầng (Multi-stage build với Maven & Eclipse Temurin JRE 21) cho 6 services.
- [x] **Task 7.2:** Cập nhật `docker-compose.yml` thay thế SQL Server bằng `mysql:8.0` (mount `scripts/init-mysql.sql` vào `/docker-entrypoint-initdb.d/`) và cấu hình biến môi trường cho cả 6 services.