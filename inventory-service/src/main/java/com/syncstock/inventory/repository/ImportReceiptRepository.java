package com.syncstock.inventory.repository;

import com.syncstock.inventory.entity.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Integer> {
    List<ImportReceipt> findAllByOrderByImportDateDesc();
}
