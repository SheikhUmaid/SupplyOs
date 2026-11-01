package com.supplychain.tracking;

import com.supplychain.security.AuthenticatedUser;
import com.supplychain.tracking.TrackingDtos.TrackingResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/order/{orderId}")
    public TrackingResponse forOrder(
            @PathVariable long orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return trackingService.forOrder(orderId, principal);
    }

    @GetMapping("/dispatch/{dispatchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TrackingResponse forDispatch(
            @PathVariable long dispatchId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return trackingService.forDispatch(dispatchId, principal);
    }
}
