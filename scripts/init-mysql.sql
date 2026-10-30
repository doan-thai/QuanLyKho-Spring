-- =====================================================================
-- SyncStock - MySQL 8.0 Database Initialization Script
-- Auto-executed by Docker MySQL entrypoint (/docker-entrypoint-initdb.d/)
-- Áp dụng nguyên tắc đặc quyền tối thiểu (Least Privilege):
-- Mỗi microservice sở hữu user và DB riêng biệt, không dùng root.
-- =====================================================================

-- 1. Database & User cho auth-service
CREATE DATABASE IF NOT EXISTS `auth_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_password';
ALTER USER 'auth_user'@'%' IDENTIFIED BY 'auth_password';
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'auth_user'@'%';

-- 2. Database & User cho inventory-service
CREATE DATABASE IF NOT EXISTS `warehouse_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'inventory_user'@'%' IDENTIFIED BY 'inventory_password';
ALTER USER 'inventory_user'@'%' IDENTIFIED BY 'inventory_password';
GRANT ALL PRIVILEGES ON `warehouse_db`.* TO 'inventory_user'@'%';

-- 3. Database & User cho supplier-service
CREATE DATABASE IF NOT EXISTS `supplier_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'supplier_user'@'%' IDENTIFIED BY 'supplier_password';
ALTER USER 'supplier_user'@'%' IDENTIFIED BY 'supplier_password';
GRANT ALL PRIVILEGES ON `supplier_db`.* TO 'supplier_user'@'%';

-- Cấp quyền truy cập cho user root từ bất kỳ host nào (%) phục vụ quản trị DB
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `warehouse_db`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `supplier_db`.* TO 'root'@'%';

FLUSH PRIVILEGES;
