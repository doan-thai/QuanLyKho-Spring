package com.syncstock.supplier.repository;

import com.syncstock.supplier.entity.SupplierInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierInfo, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    List<SupplierInfo> findAllByOrderByNameAsc();

    Page<SupplierInfo> findAllByOrderByNameAsc(Pageable pageable);

    @Query("SELECT s FROM SupplierInfo s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.phone IS NOT NULL AND LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY s.name ASC")
    List<SupplierInfo> searchSuppliers(@Param("keyword") String keyword);

    @Query(value = "SELECT s FROM SupplierInfo s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.phone IS NOT NULL AND LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY s.name ASC",
           countQuery = "SELECT COUNT(s) FROM SupplierInfo s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(s.contactPerson IS NOT NULL AND LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.phone IS NOT NULL AND LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(s.email IS NOT NULL AND LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SupplierInfo> searchSuppliers(@Param("keyword") String keyword, Pageable pageable);
}
