package com.supplychain;

import com.supplychain.category.Category;
import com.supplychain.category.CategoryRepository;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.product.ProductStatus;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseRepository;
import com.supplychain.warehouse.WarehouseStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;

/** Creates catalogue rows directly, so a test only exercises the API it is about. */
@TestComponent
public class TestCatalog {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final InventoryRepository inventory;

    @PersistenceContext
    private EntityManager entityManager;

    public TestCatalog(
            CategoryRepository categories,
            ProductRepository products,
            WarehouseRepository warehouses,
            InventoryRepository inventory) {
        this.categories = categories;
        this.products = products;
        this.warehouses = warehouses;
        this.inventory = inventory;
    }

    public static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public Category category() {
        return categories.saveAndFlush(new Category(unique("Category"), null));
    }

    public Product product(String name) {
        return product(name, category(), ProductStatus.ACTIVE);
    }

    public Product product(String name, Category category, ProductStatus status) {
        Instant now = Instant.now();
        return products.saveAndFlush(new Product(
                category, name, unique("SKU"), null, new BigDecimal("999.50"), null, status, now, now));
    }

    public Warehouse warehouse(String name) {
        return warehouse(name, WarehouseStatus.ACTIVE);
    }

    public Warehouse warehouse(String name, WarehouseStatus status) {
        return warehouses.saveAndFlush(new Warehouse(
                unique(name),
                "1 Test Road",
                "Hubli",
                "Karnataka",
                "580020",
                new BigDecimal("15.364700"),
                new BigDecimal("75.124000"),
                "9876543210",
                new BigDecimal("120.00"),
                new BigDecimal("14.00"),
                12,
                status,
                Instant.now()));
    }

    public Warehouse warehouse(
            String name, String latitude, String longitude, String baseCost, String costPerKm, int handlingHours,
            WarehouseStatus status) {
        return warehouses.saveAndFlush(new Warehouse(
                unique(name),
                "1 Test Road",
                name,
                "Karnataka",
                "580020",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                "9876543210",
                new BigDecimal(baseCost),
                new BigDecimal(costPerKm),
                handlingHours,
                status,
                Instant.now()));
    }

    /** Makes the next read see rows changed directly through SQL rather than this test's cached copies. */
    public void clearSession() {
        entityManager.clear();
    }

    public Warehouse save(Warehouse warehouse) {
        return warehouses.saveAndFlush(warehouse);
    }

    public Inventory stock(Product product, Warehouse warehouse, int available, int reserved, int reorderLevel) {
        return inventory.saveAndFlush(
                new Inventory(product, warehouse, available, reserved, reorderLevel, Instant.now()));
    }
}
