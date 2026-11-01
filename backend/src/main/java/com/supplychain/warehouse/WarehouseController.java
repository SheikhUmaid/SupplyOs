package com.supplychain.warehouse;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.warehouse.WarehouseDtos.WarehouseDetailResponse;
import com.supplychain.warehouse.WarehouseDtos.WarehouseRequest;
import com.supplychain.warehouse.WarehouseDtos.WarehouseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/warehouses")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public PageResponse<WarehouseResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) WarehouseStatus status,
            @ParameterObject PageParams params) {
        return warehouseService.list(q, status, params);
    }

    @GetMapping("/{id}")
    public WarehouseDetailResponse get(@PathVariable long id) {
        return warehouseService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody WarehouseRequest request) {
        return warehouseService.create(request);
    }

    @PutMapping("/{id}")
    public WarehouseResponse update(@PathVariable long id, @Valid @RequestBody WarehouseRequest request) {
        return warehouseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a warehouse, so new orders no longer consider it.")
    public void deactivate(@PathVariable long id) {
        warehouseService.deactivate(id);
    }
}
