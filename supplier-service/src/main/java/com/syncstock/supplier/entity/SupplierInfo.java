package com.syncstock.supplier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "suppliers",
        indexes = {
                @Index(name = "idx_suppliers_name", columnList = "name")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên nhà cung cấp là bắt buộc")
    @Size(max = 200, message = "Tên nhà cung cấp không được vượt quá 200 ký tự")
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Size(max = 50, message = "Tên người liên hệ không được vượt quá 50 ký tự")
    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không đúng định dạng (vd: 0901234567 hoặc +84901234567)")
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    @Column(name = "email", length = 150)
    private String email;

    @Size(max = 300, message = "Địa chỉ không được vượt quá 300 ký tự")
    @Column(name = "address", length = 300)
    private String address;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions = 0;

    @Builder.Default
    @Column(name = "total_import_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalImportValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "reliability_score", precision = 18, scale = 2, nullable = false)
    private BigDecimal reliabilityScore = BigDecimal.ZERO;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.totalTransactions == null) {
            this.totalTransactions = 0;
        }
        if (this.totalImportValue == null) {
            this.totalImportValue = BigDecimal.ZERO;
        }
        if (this.reliabilityScore == null) {
            this.reliabilityScore = BigDecimal.ZERO;
        }
    }
}
