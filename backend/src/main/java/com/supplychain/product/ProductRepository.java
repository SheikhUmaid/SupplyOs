package com.supplychain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, long id);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)""",
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)""")
    Page<Product> search(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status,
            Pageable pageable);
}
