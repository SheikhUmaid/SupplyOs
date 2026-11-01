package com.supplychain.seed;

import com.supplychain.category.Category;
import com.supplychain.category.CategoryRepository;
import com.supplychain.inventory.InventoryDtos.InventoryCreateRequest;
import com.supplychain.inventory.InventoryService;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.product.ProductStatus;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseRepository;
import com.supplychain.warehouse.WarehouseStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demonstration warehouses, categories, products and stock, for an empty development database. Stock
 * goes through InventoryService so that it appears in the ledger like any other stock.
 */
@Component
@Profile("dev")
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private record WarehouseSeed(
            String name, String address, String city, String postalCode, String latitude, String longitude,
            String contact, String baseCost, String costPerKm, int handlingHours) {}

    /** `stock` lists units in Hubli, Bengaluru and Mysuru, in that order. */
    private record ProductSeed(
            String name, String sku, String category, String price, String image, String description,
            int reorderLevel, int[] stock) {}

    private static final List<WarehouseSeed> WAREHOUSES = List.of(
            new WarehouseSeed("Hubli Warehouse", "Gokul Road Industrial Area", "Hubli", "580030",
                    "15.364700", "75.124000", "+91 8362000001", "120.00", "14.00", 12),
            new WarehouseSeed("Bengaluru Warehouse", "Peenya Industrial Area", "Bengaluru", "560058",
                    "12.971600", "77.594600", "+91 8022000002", "180.00", "16.00", 6),
            new WarehouseSeed("Mysuru Warehouse", "Hebbal Industrial Area", "Mysuru", "570016",
                    "12.295800", "76.639400", "+91 8212000003", "100.00", "13.00", 18));

    private static final Map<String, String> CATEGORIES = Map.of(
            "Phones & Tablets", "Smartphones and tablets",
            "Computers", "Laptops and monitors",
            "Audio", "Headphones and speakers",
            "Accessories", "Keyboards, mice and power",
            "Smart Devices", "Wearables, cameras and networking");

    private static final List<ProductSeed> PRODUCTS = List.of(
            // These two are the required demonstration scenario: Mysuru is short of smartphones.
            new ProductSeed("Smartphone", "PH-1001", "Phones & Tablets", "24999.00", "smartphone",
                    "6.5-inch display, 128 GB storage and a 5000 mAh battery.", 8, new int[] {15, 50, 5}),
            new ProductSeed("Wireless Headphones", "AU-2001", "Audio", "3499.00", "headphones",
                    "Over-ear Bluetooth headphones with 30 hours of battery life.", 8, new int[] {10, 30, 20}),
            new ProductSeed("Tablet", "PH-1002", "Phones & Tablets", "18999.00", "tablet",
                    "10.4-inch display with 64 GB storage.", 5, new int[] {9, 22, 7}),
            new ProductSeed("Laptop", "CO-3001", "Computers", "54990.00", "laptop",
                    "14-inch laptop with 16 GB RAM and a 512 GB SSD.", 6, new int[] {10, 20, 12}),
            // No single warehouse can fill an order of 6, though 12 exist in total. This shows no splitting.
            new ProductSeed("27-inch Monitor", "CO-3002", "Computers", "16499.00", "monitor",
                    "27-inch QHD IPS monitor.", 5, new int[] {4, 4, 4}),
            new ProductSeed("Mechanical Keyboard", "AC-4001", "Accessories", "4299.00", "keyboard",
                    "Tenkeyless mechanical keyboard with tactile switches.", 8, new int[] {12, 18, 30}),
            new ProductSeed("Wireless Mouse", "AC-4002", "Accessories", "899.00", "mouse",
                    "Ergonomic wireless mouse with a silent click.", 10, new int[] {25, 6, 14}),
            new ProductSeed("Power Bank", "AC-4003", "Accessories", "1599.00", "battery-charging",
                    "20000 mAh power bank with fast charging.", 10, new int[] {40, 35, 3}),
            new ProductSeed("Bluetooth Speaker", "AU-2002", "Audio", "2799.00", "speaker",
                    "Portable water-resistant speaker.", 6, new int[] {2, 16, 11}),
            new ProductSeed("Smart Watch", "SD-5001", "Smart Devices", "6999.00", "watch",
                    "Fitness tracking with a 7-day battery.", 6, new int[] {14, 3, 9}),
            new ProductSeed("Webcam", "SD-5002", "Smart Devices", "3299.00", "camera",
                    "1080p webcam with a built-in microphone.", 5, new int[] {8, 12, 6}),
            new ProductSeed("Wi-Fi Router", "SD-5003", "Smart Devices", "2499.00", "router",
                    "Dual-band Wi-Fi 6 router.", 5, new int[] {0, 15, 7}));

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final WarehouseRepository warehouses;
    private final InventoryService inventoryService;
    private final Clock clock;

    public CatalogSeeder(
            ProductRepository products,
            CategoryRepository categories,
            WarehouseRepository warehouses,
            InventoryService inventoryService,
            Clock clock) {
        this.products = products;
        this.categories = categories;
        this.warehouses = warehouses;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0 || warehouses.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        List<Warehouse> savedWarehouses = WAREHOUSES.stream()
                .map(seed -> warehouses.save(new Warehouse(
                        seed.name(), seed.address(), seed.city(), "Karnataka", seed.postalCode(),
                        new BigDecimal(seed.latitude()), new BigDecimal(seed.longitude()), seed.contact(),
                        new BigDecimal(seed.baseCost()), new BigDecimal(seed.costPerKm()), seed.handlingHours(),
                        WarehouseStatus.ACTIVE, now)))
                .toList();
        Map<String, Category> savedCategories = CATEGORIES.entrySet().stream()
                .map(entry -> categories.save(new Category(entry.getKey(), entry.getValue())))
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        for (ProductSeed seed : PRODUCTS) {
            Product product = products.save(new Product(
                    savedCategories.get(seed.category()), seed.name(), seed.sku(), seed.description(),
                    new BigDecimal(seed.price()), "/products/" + seed.image() + ".svg", ProductStatus.ACTIVE, now, now));
            for (int i = 0; i < savedWarehouses.size(); i++) {
                inventoryService.create(
                        new InventoryCreateRequest(
                                product.getId(), savedWarehouses.get(i).getId(), seed.stock()[i], seed.reorderLevel()),
                        null);
            }
        }
        log.info("Created the demonstration catalogue: {} warehouses, {} products.", WAREHOUSES.size(), PRODUCTS.size());
    }
}
