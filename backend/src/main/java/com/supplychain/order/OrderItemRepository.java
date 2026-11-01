package com.supplychain.order;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    record UnitCount(long orderId, long units) {}

    @Query("SELECT i FROM OrderItem i JOIN FETCH i.product WHERE i.order.id = :orderId ORDER BY i.id")
    List<OrderItem> findWithProductByOrderId(@Param("orderId") long orderId);

    @Query("""
            SELECT new com.supplychain.order.OrderItemRepository$UnitCount(i.order.id, SUM(i.quantity))
            FROM OrderItem i
            WHERE i.order.id IN :orderIds
            GROUP BY i.order.id""")
    List<UnitCount> countUnits(@Param("orderIds") Collection<Long> orderIds);
}
