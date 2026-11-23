package com.example.app.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "nutrient_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NutrientReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer nutrientId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 1)
    private String gender;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int ageMin;
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int ageMax;
    @Column(precision = 12, scale = 4)
    private BigDecimal rni;
    @Column(precision = 12, scale = 4)
    private BigDecimal ul;
}
