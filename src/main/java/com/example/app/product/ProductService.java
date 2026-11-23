package com.example.app.product;

import com.example.app.common.ApiException;
import com.example.app.common.Cursor;
import com.example.app.common.PageResponse;
import com.example.app.product.ProductDtos.NewNutrient;
import com.example.app.product.ProductDtos.NewProduct;
import com.example.app.product.ProductDtos.NutrientAmount;
import com.example.app.product.ProductDtos.ProductDetail;
import com.example.app.product.ProductDtos.ProductSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository products;
    private final NutrientRepository nutrients;
    private final ProductNutrientRepository productNutrients;

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> search(Long userId, String q, String cursor, int size) {
        if (q.isBlank() || q.length() > 100 || size < 1 || size > 100) {
            throw ApiException.badRequest("검색어(1~100자)와 size(1~100)를 확인해 주세요.");
        }
        // 인덱스를 쓰도록 접두사로만 검색한다.
        String pattern = q.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return Cursor.page(products.search(userId, pattern, Cursor.decode(cursor, 0), Limit.of(size + 1)),
                size, Product::getId, ProductSummary::of);
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(Long userId, Long productId) {
        Product p = getAccessible(userId, productId);
        List<ProductNutrient> amounts = productNutrients.findByProductIdIn(List.of(productId));
        Map<Integer, Nutrient> byId = nutrients.findAllById(nutrientIds(amounts)).stream()
                .collect(Collectors.toMap(Nutrient::getId, Function.identity()));
        return new ProductDetail(p.getId(), p.getName(), p.getManufacturer(), p.getImageUrl(), p.getServingUnit(),
                amounts.stream().map(a -> {
                    Nutrient n = byId.get(a.getNutrientId());
                    return new NutrientAmount(n.getCode(), n.getNameKo(), a.getAmount(), n.getUnit());
                }).toList());
    }

    /** 사용자가 직접 등록한 제품은 본인만 쓸 수 있다. */
    public Product getAccessible(Long userId, Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> ApiException.notFound("제품을 찾을 수 없어요."));
        if (product.getSource() == ProductSource.USER && !userId.equals(product.getCreatedBy())) {
            throw ApiException.forbidden();
        }
        return product;
    }

    public Product createByUser(Long userId, NewProduct request) {
        List<NewNutrient> items = request.nutrients() == null ? List.of() : request.nutrients();
        Set<String> codes = items.stream().map(NewNutrient::code).collect(Collectors.toSet());
        if (codes.size() != items.size()) {
            throw ApiException.badRequest("같은 성분이 두 번 들어 있어요.");
        }
        Map<String, Nutrient> byCode = nutrients.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(Nutrient::getCode, Function.identity()));
        for (NewNutrient item : items) {
            Nutrient nutrient = byCode.get(item.code());
            if (nutrient == null) {
                throw ApiException.badRequest("알 수 없는 성분이에요: " + item.code());
            }
            if (item.unit() != null && !item.unit().equals(nutrient.getUnit())) {
                throw ApiException.badRequest(nutrient.getNameKo() + " 단위는 " + nutrient.getUnit() + "로 입력해 주세요.");
            }
        }
        Product product = products.save(new Product(request.name().strip(), request.manufacturer(),
                ProductSource.USER, userId));
        productNutrients.saveAll(items.stream()
                .map(i -> new ProductNutrient(product.getId(), byCode.get(i.code()).getId(), i.amount())).toList());
        return product;
    }

    /** productId의 성분 중 others에도 들어 있는 성분 코드. */
    @Transactional(readOnly = true)
    public List<String> duplicateNutrients(Long productId, Collection<Long> others) {
        Set<Integer> mine = nutrientIds(productNutrients.findByProductIdIn(List.of(productId)));
        mine.retainAll(nutrientIds(productNutrients.findByProductIdIn(others)));
        return nutrients.findAllById(mine).stream().map(Nutrient::getCode).sorted().toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Product> findAll(Collection<Long> ids) {
        return products.findAllById(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private static Set<Integer> nutrientIds(List<ProductNutrient> amounts) {
        return amounts.stream().map(ProductNutrient::getNutrientId).collect(Collectors.toCollection(TreeSet::new));
    }
}
