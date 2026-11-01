package com.supplychain.analytics;

import com.supplychain.analytics.AnalyticsDtos.KpiResponse;
import com.supplychain.analytics.AnalyticsDtos.LabelledCountResponse;
import com.supplychain.analytics.AnalyticsDtos.TopProductResponse;
import com.supplychain.analytics.AnalyticsDtos.WarehouseComparisonResponse;
import com.supplychain.analytics.AnalyticsDtos.WarehouseNameResponse;
import com.supplychain.inventory.InventoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Live aggregates, always computed from the operational tables. Plain SQL suits grouped reporting
 * better than JPA entities, and every figure on a dashboard traces back to one query here.
 *
 * <p>Timestamps are stored in UTC and reporting days are Indian days, hence the 330-minute shift.
 * Bounds are passed as UTC LocalDateTimes because the driver would otherwise convert an Instant using
 * the machine's time zone.
 */
@Repository
public class AnalyticsRepository {

    private static final String IST_DAY = "DATE(DATE_ADD(%s, INTERVAL 330 MINUTE))";
    private static final String ACTIVE_SHIPMENTS = "('PENDING','PACKED','DISPATCHED','IN_TRANSIT')";
    /** The same definition as the inventory screen's low-stock filter, so the two can't disagree. */
    private static final String LOW_STOCK = """
            i.available_quantity < i.reorder_level
            AND EXISTS (SELECT 1 FROM products p WHERE p.id = i.product_id AND p.status = 'ACTIVE')
            AND EXISTS (SELECT 1 FROM warehouses w WHERE w.id = i.warehouse_id AND w.status = 'ACTIVE')""";

    public record StockTotals(long available, long reserved) {}

    public record OrderTotals(long total, long cancelled, long allocated, BigDecimal value) {}

    public record MovementRow(LocalDate date, long received, long shipped) {}

    public record ProductStockRow(long productId, String name, long warehouseId, long available) {}

    public record DispatchTotals(long active, long delivered, long pending, long onTime) {}

    private final JdbcClient jdbc;

    public AnalyticsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public KpiResponse kpis() {
        return jdbc.sql("""
                SELECT
                  (SELECT COUNT(*) FROM orders) AS total_orders,
                  (SELECT COUNT(*) FROM orders WHERE status = 'PENDING') AS pending_orders,
                  (SELECT COUNT(*) FROM products WHERE status = 'ACTIVE') AS total_products,
                  (SELECT COUNT(*) FROM warehouses WHERE status = 'ACTIVE') AS total_warehouses,
                  (SELECT COALESCE(SUM(i.available_quantity + i.reserved_quantity), 0)
                     FROM inventory i JOIN warehouses w ON w.id = i.warehouse_id
                     WHERE w.status = 'ACTIVE') AS total_inventory,
                  (SELECT COUNT(*) FROM dispatches WHERE status IN %s) AS active_dispatches,
                  (SELECT COUNT(*) FROM orders WHERE status = 'DELIVERED') AS delivered_orders,
                  (SELECT COUNT(*) FROM inventory i WHERE %s) AS low_stock
                """.formatted(ACTIVE_SHIPMENTS, LOW_STOCK))
                .query((rs, row) -> new KpiResponse(
                        rs.getLong("total_orders"),
                        rs.getLong("pending_orders"),
                        rs.getLong("total_products"),
                        rs.getLong("total_warehouses"),
                        rs.getLong("total_inventory"),
                        rs.getLong("active_dispatches"),
                        rs.getLong("delivered_orders"),
                        rs.getLong("low_stock")))
                .single();
    }

    public long lowStockCount() {
        return jdbc.sql("SELECT COUNT(*) FROM inventory i WHERE " + LOW_STOCK).query(Long.class).single();
    }

    public long pendingOrderCount() {
        return jdbc.sql("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'").query(Long.class).single();
    }

    public Map<LocalDate, Long> ordersPerDay(Instant from) {
        Map<LocalDate, Long> counts = new HashMap<>();
        String day = IST_DAY.formatted("created_at");
        jdbc.sql("SELECT " + day + " AS day, COUNT(*) AS total FROM orders WHERE created_at >= :from GROUP BY " + day)
                .param("from", utc(from))
                .query((rs, row) -> counts.put(rs.getObject("day", LocalDate.class), rs.getLong("total")))
                .list();
        return counts;
    }

    /** Allocation and delivery figures cover orders placed since `from`; stock is always current. */
    public List<WarehouseComparisonResponse> warehouseComparison(Instant from) {
        return jdbc.sql("""
                SELECT w.id, w.name, w.status,
                  COALESCE((SELECT SUM(i.available_quantity) FROM inventory i WHERE i.warehouse_id = w.id), 0) AS available,
                  COALESCE((SELECT SUM(i.reserved_quantity) FROM inventory i WHERE i.warehouse_id = w.id), 0) AS reserved,
                  (SELECT COUNT(*) FROM orders o WHERE o.selected_warehouse_id = w.id
                     AND o.status <> 'CANCELLED' AND o.created_at >= :from) AS allocated,
                  (SELECT COUNT(*) FROM orders o WHERE o.selected_warehouse_id = w.id
                     AND o.status = 'DELIVERED' AND o.created_at >= :from) AS delivered
                FROM warehouses w
                ORDER BY w.id""")
                .param("from", utc(from))
                .query((rs, row) -> {
                    long allocated = rs.getLong("allocated");
                    long delivered = rs.getLong("delivered");
                    return new WarehouseComparisonResponse(
                            rs.getLong("id"),
                            rs.getString("name"),
                            "ACTIVE".equals(rs.getString("status")),
                            rs.getLong("available"),
                            rs.getLong("reserved"),
                            allocated,
                            delivered,
                            allocated == 0 ? null : (double) delivered / allocated);
                })
                .list();
    }

    public OrderTotals orderTotals(Instant from) {
        return jdbc.sql("""
                SELECT COUNT(*) AS total,
                  COALESCE(SUM(status = 'CANCELLED'), 0) AS cancelled,
                  COALESCE(SUM(selected_warehouse_id IS NOT NULL), 0) AS allocated,
                  COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END), 0) AS value
                FROM orders WHERE created_at >= :from""")
                .param("from", utc(from))
                .query((rs, row) -> new OrderTotals(
                        rs.getLong("total"), rs.getLong("cancelled"), rs.getLong("allocated"), rs.getBigDecimal("value")))
                .single();
    }

    public Double averageDeliveryHours(Instant from) {
        return jdbc.sql("""
                SELECT AVG(TIMESTAMPDIFF(SECOND, o.created_at, d.delivery_date)) / 3600
                FROM dispatches d JOIN orders o ON o.id = d.order_id
                WHERE d.delivery_date IS NOT NULL AND o.created_at >= :from""")
                .param("from", utc(from))
                .query((rs, row) -> {
                    BigDecimal hours = rs.getBigDecimal(1);
                    return hours == null ? -1d : hours.doubleValue();
                })
                .optional()
                .filter(hours -> hours >= 0)
                .orElse(null);
    }

    public List<LabelledCountResponse> ordersByStatus(Instant from) {
        return jdbc.sql("""
                SELECT status, COUNT(*) AS total FROM orders WHERE created_at >= :from
                GROUP BY status ORDER BY total DESC, status""")
                .param("from", utc(from))
                .query((rs, row) -> new LabelledCountResponse(rs.getString("status"), rs.getLong("total")))
                .list();
    }

    public List<TopProductResponse> topProducts(Instant from, int limit) {
        return jdbc.sql("""
                SELECT p.id, p.name, p.sku, SUM(oi.quantity) AS units, SUM(oi.subtotal) AS revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN products p ON p.id = oi.product_id
                WHERE o.status <> 'CANCELLED' AND o.created_at >= :from
                GROUP BY p.id, p.name, p.sku
                ORDER BY units DESC, revenue DESC, p.id
                LIMIT :limit""")
                .param("from", utc(from))
                .param("limit", limit)
                .query((rs, row) -> new TopProductResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getLong("units"),
                        rs.getBigDecimal("revenue")))
                .list();
    }

    public StockTotals stockTotals() {
        return jdbc.sql("""
                SELECT COALESCE(SUM(i.available_quantity), 0) AS available,
                       COALESCE(SUM(i.reserved_quantity), 0) AS reserved
                FROM inventory i JOIN warehouses w ON w.id = i.warehouse_id
                WHERE w.status = 'ACTIVE'""")
                .query((rs, row) -> new StockTotals(rs.getLong("available"), rs.getLong("reserved")))
                .single();
    }

    public List<WarehouseNameResponse> activeWarehouses() {
        return jdbc.sql("SELECT id, name FROM warehouses WHERE status = 'ACTIVE' ORDER BY id")
                .query((rs, row) -> new WarehouseNameResponse(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    public List<ProductStockRow> productStock() {
        return jdbc.sql("""
                SELECT p.id, p.name, i.warehouse_id, i.available_quantity
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                JOIN warehouses w ON w.id = i.warehouse_id
                WHERE p.status = 'ACTIVE' AND w.status = 'ACTIVE'
                ORDER BY p.id, i.warehouse_id""")
                .query((rs, row) -> new ProductStockRow(
                        rs.getLong("id"), rs.getString("name"), rs.getLong("warehouse_id"), rs.getLong("available_quantity")))
                .list();
    }

    /**
     * "Received" is restocking, without the opening balance a new inventory record starts with, which
     * is not a delivery. "Shipped" is units that physically left when a shipment was dispatched.
     */
    public List<MovementRow> stockMovement(Instant from) {
        String day = IST_DAY.formatted("created_at");
        return jdbc.sql("""
                SELECT %1$s AS day,
                  COALESCE(SUM(CASE WHEN type = 'RESTOCK' AND COALESCE(note, '') <> :opening THEN available_delta ELSE 0 END), 0) AS received,
                  COALESCE(SUM(CASE WHEN type = 'SHIP' THEN -reserved_delta ELSE 0 END), 0) AS shipped
                FROM inventory_transactions
                WHERE created_at >= :from
                GROUP BY %1$s""".formatted(day))
                .param("from", utc(from))
                .param("opening", InventoryService.OPENING_STOCK_NOTE)
                .query((rs, row) -> new MovementRow(
                        rs.getObject("day", LocalDate.class), rs.getLong("received"), rs.getLong("shipped")))
                .list();
    }

    public DispatchTotals dispatchTotals(Instant from) {
        return jdbc.sql("""
                SELECT
                  COALESCE(SUM(d.status IN %s), 0) AS active,
                  COALESCE(SUM(d.status = 'DELIVERED'), 0) AS delivered,
                  COALESCE(SUM(d.status IN ('PENDING','PACKED')), 0) AS pending,
                  COALESCE(SUM(d.status = 'DELIVERED'
                    AND %s <= d.estimated_delivery_date), 0) AS on_time
                FROM dispatches d WHERE d.created_at >= :from"""
                .formatted(ACTIVE_SHIPMENTS, IST_DAY.formatted("d.delivery_date")))
                .param("from", utc(from))
                .query((rs, row) -> new DispatchTotals(
                        rs.getLong("active"), rs.getLong("delivered"), rs.getLong("pending"), rs.getLong("on_time")))
                .single();
    }

    public List<LabelledCountResponse> dispatchStatusDistribution(Instant from) {
        return jdbc.sql("""
                SELECT status, COUNT(*) AS total FROM dispatches WHERE created_at >= :from
                GROUP BY status ORDER BY total DESC, status""")
                .param("from", utc(from))
                .query((rs, row) -> new LabelledCountResponse(rs.getString("status"), rs.getLong("total")))
                .list();
    }
}
