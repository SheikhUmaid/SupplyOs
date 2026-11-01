package com.supplychain.dispatch;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.dispatch.DispatchDtos.DispatchResponse;
import com.supplychain.dispatch.DispatchDtos.DispatchStatusRequest;
import com.supplychain.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatches")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping
    public PageResponse<DispatchResponse> list(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String q,
            @ParameterObject PageParams params) {
        return dispatchService.list(status, warehouseId, q, params);
    }

    @GetMapping("/{id}")
    public DispatchResponse get(@PathVariable long id) {
        return dispatchService.get(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move a shipment one step forward. Any other change is refused with a 409.")
    public DispatchResponse updateStatus(
            @PathVariable long id,
            @Valid @RequestBody DispatchStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return dispatchService.updateStatus(id, request, principal.id());
    }
}
