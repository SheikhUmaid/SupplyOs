package com.supplychain.product;

import com.supplychain.category.Category;
import com.supplychain.category.CategoryRepository;
import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.common.SearchTerm;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.inventory.InventoryRepository.StockTotals;
import com.supplychain.product.ProductDtos.ProductRequest;
import com.supplychain.product.ProductDtos.ProductResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final String SKU_TAKEN = "A product with this SKU already exists.";
    private static final String NOT_FOUND = "This product doesn't exist.";
    private static final Map<String, String> SORTABLE =
            Map.of("name", "name", "sku", "sku", "price", "price", "createdAt", "createdAt");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryRepository inventory;
    private final Clock clock;

    public ProductService(
            ProductRepository products, CategoryRepository categories, InventoryRepository inventory, Clock clock) {
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
        this.clock = clock;
    }

    /** Non-administrators only ever see active products, whatever status they ask for. */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(
            String q, Long categoryId, ProductStatus status, PageParams params, boolean admin) {
        ProductStatus effectiveStatus = admin ? status : ProductStatus.ACTIVE;
        Page<Product> page = products.search(
                SearchTerm.contains(q), categoryId, effectiveStatus, params.toPageable(SORTABLE, Sort.by("name")));
        Map<Long, StockTotals> totals = stockTotals(page.getContent());
        return PageResponse.from(page.map(product -> toResponse(product, totals.get(product.getId()))));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(long id, boolean admin) {
        Product product = find(id);
        if (!admin && product.getStatus() != ProductStatus.ACTIVE) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND);
        }
        return toResponse(product, stockTotals(List.of(product)).get(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        if (products.existsBySkuIgnoreCase(sku)) {
            throw new ApiException(HttpStatus.CONFLICT, SKU_TAKEN);
        }
        Instant now = clock.instant();
        Product product = new Product(
                findCategory(request.categoryId()),
                request.name().trim(),
                sku,
                request.description(),
                request.price(),
                request.imageUrl(),
                request.status() == null ? ProductStatus.ACTIVE : request.status(),
                now,
                now);
        return save(product);
    }

    @Transactional
    public ProductResponse update(long id, ProductRequest request) {
        Product product = find(id);
        String sku = request.sku().trim();
        if (products.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new ApiException(HttpStatus.CONFLICT, SKU_TAKEN);
        }
        product.setCategory(findCategory(request.categoryId()));
        product.setName(request.name().trim());
        product.setSku(sku);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl());
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        product.setUpdatedAt(clock.instant());
        return save(product);
    }

    /** Products are never physically deleted, because past orders refer to them. */
    @Transactional
    public void deactivate(long id) {
        Product product = find(id);
        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedAt(clock.instant());
    }

    private ProductResponse save(Product product) {
        try {
            Product saved = products.saveAndFlush(product);
            return toResponse(saved, stockTotals(List.of(saved)).get(saved.getId()));
        } catch (DataIntegrityViolationException ex) {
            // Two simultaneous saves both passed the check; the unique key caught one.
            throw new ApiException(HttpStatus.CONFLICT, SKU_TAKEN);
        }
    }

    private Product find(long id) {
        return products.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
    }

    private Category findCategory(long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This category doesn't exist."));
    }

    private Map<Long, StockTotals> stockTotals(List<Product> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        return inventory.totalsForProducts(page.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(StockTotals::productId, Function.identity()));
    }

    private static ProductResponse toResponse(Product product, StockTotals totals) {
        Availability availability =
                totals == null ? Availability.OUT : Availability.of(totals.available(), totals.reorderLevel());
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getStatus(),
                availability,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
