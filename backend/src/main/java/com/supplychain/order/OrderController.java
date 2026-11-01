package com.supplychain.order;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.order.OrderDtos.OrderCreateRequest;
import com.supplychain.order.OrderDtos.OrderDetailResponse;
import com.supplychain.order.OrderDtos.OrderSummaryResponse;
import com.supplychain.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderDetailResponse place(
            @Valid @RequestBody OrderCreateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return orderService.place(request, idempotencyKey, principal.id());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OrderSummaryResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String q,
            @ParameterObject PageParams params) {
        return orderService.list(null, status, warehouseId, q, params);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<OrderSummaryResponse> myOrders(
            @ParameterObject PageParams params,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return orderService.listForCustomer(principal.id(), params);
    }

    @PostMapping("/{id}/reallocate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run allocation again for a pending order, for example after restocking.")
    public OrderDetailResponse reallocate(@PathVariable long id) {
        return orderService.reallocate(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order that hasn't left the warehouse, and release its reserved stock.")
    public OrderDetailResponse cancel(
            @PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return orderService.cancel(id, principal);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse get(
            @PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return orderService.get(id, principal);
    }
}
