package com.syncstock.supplier.repository;

import com.syncstock.supplier.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Integer> {
    boolean existsByReceiptId(Integer receiptId);
}

