package com.supplychain.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record OrderItemRequest(
            @NotNull(message = "Choose a product.") Long productId,
            @NotNull(message = "Enter a quantity.")
                    @Min(value = 1, message = "The quantity must be at least 1.")
                    @Max(value = 99, message = "The quantity can't be more than 99.")
                    Integer quantity) {}

    /** There are deliberately no prices here. The server always uses its own. */
    public record OrderCreateRequest(
            @NotEmpty(message = "Your cart is empty.") List<@Valid OrderItemRequest> items,
            @NotBlank(message = "Enter the recipient's name.") @Size(max = 120, message = "Use 120 characters or fewer.")
                    String deliveryName,
            @NotBlank(message = "Enter a contact number.")
                    @Pattern(regexp = "^(\\+91[ -]?)?[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number.")
                    String deliveryPhone,
            @NotBlank(message = "Enter the delivery address.") @Size(max = 255, message = "Use 255 characters or fewer.")
                    String deliveryAddress,
            @NotBlank(message = "Choose a city.") String deliveryCity,
            @NotBlank(message = "Enter a PIN code.")
                    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code.")
                    String deliveryPostalCode,
            @NotNull(message = "Choose a delivery speed.") OrderPriority priority,
            @AssertTrue(message = "Confirm the simulated payment to place your order.")
                    boolean simulatedPaymentConfirmed) {}

    public record OrderItemResponse(
            long productId,
            String productName,
            String sku,
            @Schema(types = {"string", "null"}) String imageUrl,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal) {}

    public record OrderSummaryResponse(
            long id,
            String orderNumber,
            String customerName,
            Instant createdAt,
            OrderStatus status,
            PaymentStatus paymentStatus,
            OrderPriority priority,
            BigDecimal totalAmount,
            long unitCount,
            @Schema(types = {"string", "null"}) String selectedWarehouse) {}

    public record OrderDetailResponse(
            long id,
            String orderNumber,
            String customerName,
            Instant createdAt,
            OrderStatus status,
            PaymentStatus paymentStatus,
            OrderPriority priority,
            BigDecimal totalAmount,
            @Schema(types = {"string", "null"}) String selectedWarehouse,
            String deliveryName,
            String deliveryPhone,
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            String deliveryPostalCode,
            @Schema(types = {"string", "null"}) Instant confirmedAt,
            @Schema(types = {"string", "null"}) Instant cancelledAt,
            boolean cancellable,
            List<OrderItemResponse> items) {}
}
