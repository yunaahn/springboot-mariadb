package com.example.app.product;

import jakarta.persistence.*;
import java.time.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String manufacturer;
    private String mfdsReportNo;
    private String imageUrl;
    private String servingUnit = "정";
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ProductSource source;
    private Long createdBy;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public Product(String name, String manufacturer, ProductSource source, Long createdBy) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.source = source;
        this.createdBy = createdBy;
    }
}
