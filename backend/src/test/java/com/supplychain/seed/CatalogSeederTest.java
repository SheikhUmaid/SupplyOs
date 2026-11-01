package com.supplychain.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.supplychain.category.CategoryRepository;
import com.supplychain.product.ProductRepository;
import com.supplychain.warehouse.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** The catalogue alone: with no customer password configured, no history is created. */
@SpringBootTest(properties = {"app.seed.enabled=false", "SEED_CUSTOMER_PASSWORD="})
@ActiveProfiles({"test", "dev"})
@Transactional
class CatalogSeederTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private WarehouseRepository warehouses;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CatalogSeeder seeder;

    @BeforeEach
    void seed() {
        seeder.seed();
    }

    private Integer stock(String sku, String warehouse) {
        return jdbc.queryForObject("""
                SELECT i.available_quantity FROM inventory i
                JOIN products p ON i.product_id = p.id
                JOIN warehouses w ON i.warehouse_id = w.id
                WHERE p.sku = ? AND w.name = ?""", Integer.class, sku, warehouse);
    }

    @Test
    void createsTheRequiredCatalogue() {
        assertThat(warehouses.count()).isEqualTo(3);
        assertThat(categories.count()).isEqualTo(5);
        assertThat(products.count()).isBetween(10L, 15L);
    }

    @Test
    void matchesTheSpecificationsDemonstrationStock() {
        assertThat(List.of(
                        stock("PH-1001", "Hubli Warehouse"),
                        stock("PH-1001", "Bengaluru Warehouse"),
                        stock("PH-1001", "Mysuru Warehouse")))
                .containsExactly(15, 50, 5);
        assertThat(List.of(
                        stock("AU-2001", "Hubli Warehouse"),
                        stock("AU-2001", "Bengaluru Warehouse"),
                        stock("AU-2001", "Mysuru Warehouse")))
                .containsExactly(10, 30, 20);
    }

    @Test
    void includesAProductNoSingleWarehouseCanSupplyInBulk() {
        assertThat(stock("CO-3002", "Hubli Warehouse")).isEqualTo(4);
        assertThat(stock("CO-3002", "Bengaluru Warehouse")).isEqualTo(4);
        assertThat(stock("CO-3002", "Mysuru Warehouse")).isEqualTo(4);
    }

    @Test
    void includesLowStockScenarios() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM inventory WHERE available_quantity < reorder_level", Long.class))
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void recordsOpeningStockInTheLedgerAsASystemChange() {
        Long mismatches = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT i.id FROM inventory i
                    LEFT JOIN inventory_transactions t ON t.inventory_id = i.id
                    GROUP BY i.id, i.available_quantity
                    HAVING i.available_quantity <> COALESCE(SUM(t.available_delta), 0)) x""", Long.class);
        assertThat(mismatches).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM inventory_transactions WHERE performed_by IS NOT NULL", Long.class))
                .isZero();
    }

    @Test
    void createsNoHistoryWithoutACustomerPassword() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isZero();
    }

    @Test
    void doesNothingOnASecondRun() {
        long before = products.count();
        seeder.seed();
        assertThat(products.count()).isEqualTo(before);
        assertThat(warehouses.count()).isEqualTo(3);
    }
}
