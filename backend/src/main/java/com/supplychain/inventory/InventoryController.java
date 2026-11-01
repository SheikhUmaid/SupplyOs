package com.supplychain.inventory;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.inventory.InventoryDtos.InventoryCreateRequest;
import com.supplychain.inventory.InventoryDtos.InventoryResponse;
import com.supplychain.inventory.InventoryDtos.InventoryTransactionResponse;
import com.supplychain.inventory.InventoryDtos.InventoryUpdateRequest;
import com.supplychain.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public PageResponse<InventoryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @ParameterObject PageParams params) {
        return inventoryService.list(q, warehouseId, lowStockOnly, params);
    }

    @GetMapping("/low-stock")
    public PageResponse<InventoryResponse> lowStock(@ParameterObject PageParams params) {
        return inventoryService.list(null, null, true, params);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public PageResponse<InventoryResponse> forWarehouse(
            @PathVariable long warehouseId, @ParameterObject PageParams params) {
        return inventoryService.list(null, warehouseId, false, params);
    }

    @GetMapping("/transactions")
    public PageResponse<InventoryTransactionResponse> transactions(
            @RequestParam(required = false) Long inventoryId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) String q,
            @ParameterObject PageParams params) {
        return inventoryService.listTransactions(inventoryId, warehouseId, type, q, params);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(
            @Valid @RequestBody InventoryCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return inventoryService.create(request, principal.id());
    }

    @PutMapping("/{id}")
    public InventoryResponse update(
            @PathVariable long id,
            @Valid @RequestBody InventoryUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return inventoryService.update(id, request, principal.id());
    }
}
