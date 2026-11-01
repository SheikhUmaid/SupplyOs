package com.supplychain.analytics;

import com.supplychain.analytics.AnalyticsDtos.AlertsResponse;
import com.supplychain.analytics.AnalyticsDtos.DailyCountResponse;
import com.supplychain.analytics.AnalyticsDtos.DashboardResponse;
import com.supplychain.analytics.AnalyticsDtos.DispatchAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.InventoryAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.OrderAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.ProductAvailabilityResponse;
import com.supplychain.analytics.AnalyticsDtos.ProductStockResponse;
import com.supplychain.analytics.AnalyticsDtos.StockMovementDayResponse;
import com.supplychain.analytics.AnalyticsDtos.WarehouseAnalyticsResponse;
import com.supplychain.analytics.AnalyticsDtos.WarehouseComparisonResponse;
import com.supplychain.analytics.AnalyticsRepository.DispatchTotals;
import com.supplychain.analytics.AnalyticsRepository.MovementRow;
import com.supplychain.analytics.AnalyticsRepository.OrderTotals;
import com.supplychain.analytics.AnalyticsRepository.ProductStockRow;
import com.supplychain.analytics.AnalyticsRepository.StockTotals;
import com.supplychain.common.PageParams;
import com.supplychain.dispatch.DispatchService;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.InventoryDtos.InventoryResponse;
import com.supplychain.inventory.InventoryService;
import com.supplychain.order.OrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int ACTIVITY_DAYS = 14;
    private static final int MAX_DAYS = 366;
    private static final int RECENT_ROWS = 5;
    private static final int LOW_STOCK_ROWS = 10;
    private static final int TOP_PRODUCTS = 8;

    private final AnalyticsRepository analytics;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final DispatchService dispatchService;
    private final Clock clock;

    public AnalyticsService(
            AnalyticsRepository analytics,
            OrderService orderService,
            InventoryService inventoryService,
            DispatchService dispatchService,
            Clock clock) {
        this.analytics = analytics;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.dispatchService = dispatchService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Instant activityFrom = periodStart(ACTIVITY_DAYS);
        PageParams recent = new PageParams(0, RECENT_ROWS, null);
        return new DashboardResponse(
                analytics.kpis(),
                fillDays(ACTIVITY_DAYS, analytics.ordersPerDay(activityFrom)),
                analytics.warehouseComparison(Instant.EPOCH),
                orderService.list(null, null, null, null, recent),
                inventoryService.list(null, null, true, recent),
                dispatchService.list(null, null, null, recent));
    }

    @Transactional(readOnly = true)
    public AlertsResponse alerts() {
        return new AlertsResponse(analytics.pendingOrderCount(), analytics.lowStockCount());
    }

    @Transactional(readOnly = true)
    public OrderAnalyticsResponse orders(int days) {
        Instant from = periodStart(checkDays(days));
        OrderTotals totals = analytics.orderTotals(from);
        long sold = totals.total() - totals.cancelled();
        return new OrderAnalyticsResponse(
                days,
                totals.total(),
                sold == 0
                        ? BigDecimal.ZERO.setScale(2)
                        : totals.value().divide(BigDecimal.valueOf(sold), 2, RoundingMode.HALF_UP),
                totals.value(),
                rate(totals.allocated(), totals.total()),
                rate(totals.cancelled(), totals.total()),
                analytics.averageDeliveryHours(from),
                analytics.ordersByStatus(from),
                fillDays(days, analytics.ordersPerDay(from)),
                analytics.topProducts(from, TOP_PRODUCTS));
    }

    @Transactional(readOnly = true)
    public WarehouseAnalyticsResponse warehouses(int days) {
        List<WarehouseComparisonResponse> warehouses = analytics.warehouseComparison(periodStart(checkDays(days)));
        return new WarehouseAnalyticsResponse(days, warehouses);
    }

    @Transactional(readOnly = true)
    public InventoryAnalyticsResponse inventory(int days) {
        Instant from = periodStart(checkDays(days));
        StockTotals stock = analytics.stockTotals();
        List<InventoryResponse> lowStock =
                inventoryService.list(null, null, true, new PageParams(0, LOW_STOCK_ROWS, "available,asc")).content();
        return new InventoryAnalyticsResponse(
                days,
                stock.available(),
                stock.reserved(),
                analytics.lowStockCount(),
                lowStock,
                analytics.activeWarehouses(),
                productAvailability(analytics.productStock()),
                fillMovement(days, analytics.stockMovement(from)));
    }

    @Transactional(readOnly = true)
    public DispatchAnalyticsResponse dispatches(int days) {
        Instant from = periodStart(checkDays(days));
        DispatchTotals totals = analytics.dispatchTotals(from);
        return new DispatchAnalyticsResponse(
                days,
                totals.active(),
                totals.delivered(),
                totals.pending(),
                rate(totals.onTime(), totals.delivered()),
                analytics.dispatchStatusDistribution(from));
    }

    private static int checkDays(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The period must be between 1 and " + MAX_DAYS + " days.");
        }
        return days;
    }

    /** Midnight in India, `days - 1` days ago, so a period of 7 days means today and the previous six. */
    private Instant periodStart(int days) {
        return today().minusDays(days - 1L).atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private LocalDate today() {
        return clock.instant().atZone(BUSINESS_ZONE).toLocalDate();
    }

    private static Double rate(long part, long whole) {
        return whole == 0 ? null : (double) part / whole;
    }

    /** Includes days with no orders, which a time axis must show as zero rather than skip. */
    private List<DailyCountResponse> fillDays(int days, Map<LocalDate, Long> counts) {
        List<DailyCountResponse> series = new ArrayList<>();
        LocalDate day = today().minusDays(days - 1L);
        for (int i = 0; i < days; i++, day = day.plusDays(1)) {
            series.add(new DailyCountResponse(day, counts.getOrDefault(day, 0L)));
        }
        return series;
    }

    private List<StockMovementDayResponse> fillMovement(int days, List<MovementRow> rows) {
        Map<LocalDate, MovementRow> byDay =
                rows.stream().collect(Collectors.toMap(MovementRow::date, Function.identity()));
        List<StockMovementDayResponse> series = new ArrayList<>();
        LocalDate day = today().minusDays(days - 1L);
        for (int i = 0; i < days; i++, day = day.plusDays(1)) {
            MovementRow row = byDay.get(day);
            series.add(new StockMovementDayResponse(
                    day, row == null ? 0 : row.received(), row == null ? 0 : row.shipped()));
        }
        return series;
    }

    private static List<ProductAvailabilityResponse> productAvailability(List<ProductStockRow> rows) {
        Map<Long, List<ProductStockRow>> byProduct = rows.stream()
                .collect(Collectors.groupingBy(ProductStockRow::productId, LinkedHashMap::new, Collectors.toList()));
        return byProduct.entrySet().stream()
                .map(entry -> new ProductAvailabilityResponse(
                        entry.getKey(),
                        entry.getValue().getFirst().name(),
                        entry.getValue().stream().mapToLong(ProductStockRow::available).sum(),
                        entry.getValue().stream()
                                .map(row -> new ProductStockResponse(row.warehouseId(), row.available()))
                                .toList()))
                .sorted(Comparator.comparingLong(ProductAvailabilityResponse::totalAvailable)
                        .reversed()
                        .thenComparingLong(ProductAvailabilityResponse::productId))
                .toList();
    }
}
