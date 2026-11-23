package com.example.app.product;

import com.example.app.common.ApiResponse;
import com.example.app.common.CurrentUserId;
import com.example.app.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    PageResponse<ProductDtos.ProductSummary> search(@CurrentUserId Long userId, @RequestParam String q,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(userId, q, cursor, size);
    }

    @GetMapping("/{id}")
    ApiResponse<ProductDtos.ProductDetail> detail(@CurrentUserId Long userId, @PathVariable Long id) {
        return new ApiResponse<>(service.detail(userId, id));
    }
}
