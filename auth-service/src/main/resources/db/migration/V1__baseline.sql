-- =====================================================================
-- SyncStock - Auth Service Migration V1: Baseline Schema
-- Matching Hibernate entity: User (table: users)
-- =====================================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `is_active` BIT(1) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_username` UNIQUE (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

