package com.example.app.product;

import com.example.app.common.ApiResponse;
import com.example.app.common.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 영양제를 직접 등록할 때 고를 성분 목록. */
@RestController
@RequestMapping("/api/v1/nutrients")
@RequiredArgsConstructor
public class NutrientController {

    private final NutrientRepository nutrients;

    public record NutrientResponse(String code, String name, String unit) {
    }

    @GetMapping
    ApiResponse<List<NutrientResponse>> list(@CurrentUserId Long userId) {
        return new ApiResponse<>(nutrients.findAll(Sort.by("id")).stream()
                .map(n -> new NutrientResponse(n.getCode(), n.getNameKo(), n.getUnit())).toList());
    }
}
