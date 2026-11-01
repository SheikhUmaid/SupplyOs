package com.supplychain.allocation;

import com.supplychain.allocation.AllocationDtos.AllocationResponse;
import com.supplychain.allocation.AllocationDtos.AllocationSettingsResponse;
import com.supplychain.allocation.AllocationDtos.AllocationSummaryResponse;
import com.supplychain.allocation.AllocationDtos.EvaluationResponse;
import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Allocation")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/api/allocations")
    public PageResponse<AllocationSummaryResponse> list(
            @RequestParam(required = false) AllocationOutcome outcome, @ParameterObject PageParams params) {
        return allocationService.list(outcome, params);
    }

    @GetMapping("/api/allocations/{orderId}")
    public AllocationResponse forOrder(@PathVariable long orderId) {
        return allocationService.latestForOrder(orderId);
    }

    @GetMapping("/api/allocations/{orderId}/evaluations")
    public List<EvaluationResponse> evaluations(@PathVariable long orderId) {
        return allocationService.latestEvaluations(orderId);
    }

    @GetMapping("/api/settings/allocation")
    public AllocationSettingsResponse settings() {
        return allocationService.settings();
    }
}
