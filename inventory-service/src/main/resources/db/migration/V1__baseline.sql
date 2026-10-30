-- =====================================================================
-- SyncStock - Inventory Service Migration V1: Baseline Schema
-- Matching Hibernate entities: Category, Product, ImportReceipt,
-- ExportReceipt, StockTransaction
-- =====================================================================

CREATE TABLE IF NOT EXISTS `categories` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `products` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `category` VARCHAR(100) NOT NULL,
    `image_url` VARCHAR(500) DEFAULT NULL,
    `min_stock` INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `stock` INT NOT NULL,
    `unit` VARCHAR(50) NOT NULL,
    `version` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `import_receipts` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) DEFAULT NULL,
    `import_date` DATETIME(6) NOT NULL,
    `note` VARCHAR(500) DEFAULT NULL,
    `supplier_id` INT DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `export_receipts` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) DEFAULT NULL,
    `export_date` DATETIME(6) NOT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stock_transactions` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `note` VARCHAR(500) DEFAULT NULL,
    `product_id` INT NOT NULL,
    `quantity` INT NOT NULL,
    `supplier_id` INT DEFAULT NULL,
    `transaction_date` DATETIME(6) NOT NULL,
    `transaction_type` VARCHAR(20) NOT NULL,
    `unit_price` DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

