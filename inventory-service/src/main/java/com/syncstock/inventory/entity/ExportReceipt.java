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
@Table(name = "export_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String code;

    @Column(length = 500)
    private String reason;

    @Column(name = "export_date", nullable = false)
    private LocalDateTime exportDate;
}
