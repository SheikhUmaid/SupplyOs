package com.supplychain.product;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.product.ProductDtos.ProductRequest;
import com.supplychain.product.ProductDtos.ProductResponse;
import com.supplychain.security.AuthenticatedUser;
import com.supplychain.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products. Only administrators see inactive products or can filter by status.")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            @ParameterObject PageParams params,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return productService.list(q, categoryId, status, params, isAdmin(principal));
    }

    @GetMapping("/{id}")
    public ProductResponse get(
            @PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return productService.get(id, isAdmin(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a product. Products are never physically deleted.")
    public void deactivate(@PathVariable long id) {
        productService.deactivate(id);
    }

    private static boolean isAdmin(AuthenticatedUser principal) {
        return principal != null && principal.role() == UserRole.ADMIN;
    }
}
