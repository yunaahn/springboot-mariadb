package com.example.app.product;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 공용 제품과 내가 직접 등록한 제품만 검색한다. pattern의 이스케이프 문자는 '!'. */
    @Query("""
            select p from Product p
            where p.id > :afterId and p.name like :pattern escape '!'
              and (p.source = com.example.app.product.ProductSource.MFDS or p.createdBy = :userId)
            order by p.id
            """)
    List<Product> search(Long userId, String pattern, long afterId, Limit limit);
}
