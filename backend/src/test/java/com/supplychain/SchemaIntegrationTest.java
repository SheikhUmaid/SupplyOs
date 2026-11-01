package com.supplychain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** The database itself must refuse bad data, even if application code has a bug. */
@IntegrationTest
class SchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long productId;
    private long warehouseId;

    @BeforeEach
    void insertReferenceData() {
        jdbc.update("INSERT INTO categories (name) VALUES ('Schema test category')");
        long categoryId = lastId();
        jdbc.update("""
                INSERT INTO products (category_id, name, sku, price, status, created_at, updated_at)
                VALUES (?, 'Phone', 'SCHEMA-SKU', 100, 'ACTIVE', NOW(6), NOW(6))""", categoryId);
        productId = lastId();
        warehouseId = insertWarehouse("Schema warehouse");
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertWarehouse(String name) {
        jdbc.update("""
                INSERT INTO warehouses (name, address, city, state, postal_code, latitude, longitude,
                    contact_number, base_delivery_cost, cost_per_km, handling_hours, status, created_at)
                VALUES (?, 'Addr', 'Hubli', 'Karnataka', '580020', 15.36, 75.12, '9876543210', 100, 10, 12, 'ACTIVE', NOW(6))""",
                name);
        return lastId();
    }

    private void insertInventory(long product, long warehouse, int available, int reserved) {
        jdbc.update("""
                INSERT INTO inventory (product_id, warehouse_id, available_quantity, reserved_quantity, reorder_level, last_updated)
                VALUES (?, ?, ?, ?, 0, NOW(6))""", product, warehouse, available, reserved);
    }

    @Test
    void allTwelveRequiredTablesExist() {
        var tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        assertThat(tables).contains(
                "users", "categories", "products", "warehouses", "inventory", "inventory_transactions",
                "orders", "order_items", "warehouse_allocations", "allocation_evaluations",
                "dispatches", "delivery_tracking");
    }

    @Test
    void negativeAvailableStockIsRefused() {
        assertThatThrownBy(() -> insertInventory(productId, warehouseId, -1, 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_inventory_available");
    }

    @Test
    void stockCannotBeUpdatedBelowZero() {
        insertInventory(productId, warehouseId, 3, 0);
        assertThatThrownBy(() -> jdbc.update("UPDATE inventory SET available_quantity = available_quantity - 7"))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT available_quantity FROM inventory", Integer.class)).isEqualTo(3);
    }

    @Test
    void negativeReservedStockIsRefused() {
        assertThatThrownBy(() -> insertInventory(productId, warehouseId, 1, -1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_inventory_reserved");
    }

    @Test
    void aProductCanHaveOnlyOneStockRowPerWarehouse() {
        insertInventory(productId, warehouseId, 1, 0);
        assertThatThrownBy(() -> insertInventory(productId, warehouseId, 2, 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_inventory_product_warehouse");
    }

    @Test
    void aProductMayBeStockedInSeveralWarehouses() {
        insertInventory(productId, warehouseId, 1, 0);
        insertInventory(productId, insertWarehouse("Second warehouse"), 2, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory WHERE product_id = ?", Long.class, productId))
                .isEqualTo(2);
    }

    @Test
    void skuMustBeUnique() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO products (category_id, name, sku, price, status, created_at, updated_at)
                SELECT category_id, 'Copy', sku, 1, 'ACTIVE', NOW(6), NOW(6) FROM products WHERE id = ?""", productId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_products_sku");
    }

    @Test
    void aProductOnAnOrderCannotBeDeleted() {
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role, created_at)
                VALUES ('C', 'schema@example.com', 'x', 'CUSTOMER', NOW(6))""");
        long customerId = lastId();
        long orderId = insertOrder(customerId, "ORD-SCHEMA-1", "key-1");
        jdbc.update("""
                INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
                VALUES (?, ?, 1, 100, 100)""", orderId, productId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM products WHERE id = ?", productId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void anIdempotencyKeyCanOnlyBeUsedOncePerCustomer() {
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role, created_at)
                VALUES ('C', 'schema@example.com', 'x', 'CUSTOMER', NOW(6))""");
        long customerId = lastId();
        insertOrder(customerId, "ORD-SCHEMA-1", "same-key");

        assertThatThrownBy(() -> insertOrder(customerId, "ORD-SCHEMA-2", "same-key"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_orders_customer_idempotency");
    }

    @Test
    void anOrderCanHaveOnlyOneDispatch() {
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role, created_at)
                VALUES ('C', 'schema@example.com', 'x', 'CUSTOMER', NOW(6))""");
        long orderId = insertOrder(lastId(), "ORD-SCHEMA-1", "key-1");
        insertDispatch(orderId, "TRK-1");

        assertThatThrownBy(() -> insertDispatch(orderId, "TRK-2"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uk_dispatches_order");
    }

    @Test
    void anAllocationCannotClaimSuccessWithoutAWarehouse() {
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role, created_at)
                VALUES ('C', 'schema@example.com', 'x', 'CUSTOMER', NOW(6))""");
        long orderId = insertOrder(lastId(), "ORD-SCHEMA-1", "key-1");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO warehouse_allocations (order_id, attempt_no, outcome, selected_warehouse_id,
                    weight_distance, weight_cost, weight_inventory, weight_priority, explanation, allocated_at)
                VALUES (?, 1, 'ALLOCATED', NULL, 0.4, 0.3, 0.2, 0.1, 'x', NOW(6))""", orderId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_allocations_selection");
    }

    @Test
    void anIneligibleWarehouseCannotBeRecordedAsSelected() {
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, role, created_at)
                VALUES ('C', 'schema@example.com', 'x', 'CUSTOMER', NOW(6))""");
        long orderId = insertOrder(lastId(), "ORD-SCHEMA-1", "key-1");
        jdbc.update("""
                INSERT INTO warehouse_allocations (order_id, attempt_no, outcome, selected_warehouse_id,
                    weight_distance, weight_cost, weight_inventory, weight_priority, explanation, allocated_at)
                VALUES (?, 1, 'ALLOCATED', ?, 0.4, 0.3, 0.2, 0.1, 'x', NOW(6))""", orderId, warehouseId);
        long allocationId = lastId();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO allocation_evaluations (allocation_id, warehouse_id, eligible, distance_km,
                    delivery_cost, eta_hours, decision)
                VALUES (?, ?, FALSE, 1, 1, 1, 'SELECTED')""", allocationId, warehouseId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_evaluations_selected_eligible");
    }

    private void insertDispatch(long orderId, String tracking) {
        jdbc.update("""
                INSERT INTO dispatches (order_id, warehouse_id, tracking_reference, status, estimated_delivery_date, created_at)
                VALUES (?, ?, ?, 'PENDING', CURDATE(), NOW(6))""", orderId, warehouseId, tracking);
    }

    private long insertOrder(long customerId, String number, String key) {
        jdbc.update("""
                INSERT INTO orders (order_number, customer_id, status, priority, payment_status, total_amount,
                    delivery_name, delivery_phone, delivery_address, delivery_city, delivery_state,
                    delivery_postal_code, delivery_latitude, delivery_longitude, idempotency_key, created_at)
                VALUES (?, ?, 'PENDING', 'STANDARD', 'UNPAID', 100, 'N', '9876543210', 'A', 'Hubli', 'Karnataka',
                    '580020', 15.36, 75.12, ?, NOW(6))""", number, customerId, key);
        return lastId();
    }
}
