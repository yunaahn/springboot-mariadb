package com.example.app.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nutrients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nutrient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String nameKo;
    private String unit;
    private String category;

    public Nutrient(String code, String nameKo, String unit) {
        this.code = code;
        this.nameKo = nameKo;
        this.unit = unit;
    }
}
