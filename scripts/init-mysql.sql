-- =====================================================================
-- SyncStock - MySQL 8.0 Database Initialization Script
-- Auto-executed by Docker MySQL entrypoint (/docker-entrypoint-initdb.d/)
-- =====================================================================

-- 1. Database cho auth-service
CREATE DATABASE IF NOT EXISTS `auth_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Database cho inventory-service
CREATE DATABASE IF NOT EXISTS `warehouse_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 3. Database cho supplier-service
CREATE DATABASE IF NOT EXISTS `supplier_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Cấp quyền truy cập cho user root từ bất kỳ host nào (%)
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `warehouse_db`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `supplier_db`.* TO 'root'@'%';

FLUSH PRIVILEGES;
