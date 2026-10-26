package com.syncstock.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String unit;

    @Builder.Default
    @Column(nullable = false)
    private Integer stock = 0;

    @Builder.Default
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 10;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Version
    @Column(name = "version")
    private Long version;
}
