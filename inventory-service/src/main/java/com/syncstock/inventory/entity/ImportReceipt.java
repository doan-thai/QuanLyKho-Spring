package com.syncstock.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String code;

    @Column(length = 500)
    private String note;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;
}
