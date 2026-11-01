package com.supplychain.warehouse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    @Query("""
            SELECT w FROM Warehouse w
            WHERE (:q IS NULL OR LOWER(w.name) LIKE :q ESCAPE '!' OR LOWER(w.city) LIKE :q ESCAPE '!')
              AND (:status IS NULL OR w.status = :status)""")
    Page<Warehouse> search(@Param("q") String q, @Param("status") WarehouseStatus status, Pageable pageable);
}
