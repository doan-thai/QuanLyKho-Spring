package com.syncstock.inventory.repository;

import com.syncstock.inventory.dto.StockTransactionResponse;
import com.syncstock.inventory.entity.StockTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Integer> {

    @Query("SELECT new com.syncstock.inventory.dto.StockTransactionResponse(" +
           "t.id, t.productId, p.name, t.quantity, t.unitPrice, t.supplierId, " +
           "t.transactionType, t.transactionDate, t.note) " +
           "FROM StockTransaction t LEFT JOIN Product p ON t.productId = p.id " +
           "ORDER BY t.transactionDate DESC")
    List<StockTransactionResponse> findTransactionHistory(Pageable pageable);

    List<StockTransaction> findByProductIdOrderByTransactionDateDesc(Integer productId);

    boolean existsByProductId(Integer productId);
}
