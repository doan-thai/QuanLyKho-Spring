-- =====================================================================
-- SyncStock - Supplier Service Migration V1: Baseline Schema
-- Matching Hibernate entity: SupplierInfo (table: suppliers)
-- and ProcessedEvent (table: processed_events)
-- =====================================================================

CREATE TABLE IF NOT EXISTS `suppliers` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL,
    `contact_person` VARCHAR(50) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(150) DEFAULT NULL,
    `address` VARCHAR(300) DEFAULT NULL,
    `is_active` BIT(1) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `total_transactions` INT NOT NULL,
    `total_import_value` DECIMAL(18,2) NOT NULL,
    `reliability_score` DECIMAL(18,2) NOT NULL,
    `last_transaction_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_suppliers_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `processed_events` (
    `receipt_id` INT NOT NULL,
    `processed_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
