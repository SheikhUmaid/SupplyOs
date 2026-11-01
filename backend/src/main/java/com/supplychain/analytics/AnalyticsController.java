package com.supplychain.analytics;

import com.supplychain.analytics.AnalyticsDtos.AlertsResponse;
import com.supplychain.analytics.AnalyticsDtos.DashboardResponse;
import com.supplychain.analytics.AnalyticsDtos.DispatchAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.InventoryAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.OrderAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.WarehouseAnalyticsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return analyticsService.dashboard();
    }

    @GetMapping("/alerts")
    public AlertsResponse alerts() {
        return analyticsService.alerts();
    }

    @GetMapping("/orders")
    public OrderAnalyticsResponse orders(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.orders(days);
    }

    @GetMapping("/warehouses")
    public WarehouseAnalyticsResponse warehouses(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.warehouses(days);
    }

    @GetMapping("/inventory")
    public InventoryAnalyticsResponse inventory(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.inventory(days);
    }

    @GetMapping("/dispatches")
    public DispatchAnalyticsResponse dispatches(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.dispatches(days);
    }
}
