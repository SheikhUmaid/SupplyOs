package com.supplychain.auth;

import com.supplychain.user.User;
import com.supplychain.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Enter your name.") @Size(max = 120, message = "Use 120 characters or fewer.")
                    String fullName,
            @NotBlank(message = "Enter your email address.")
                    @Email(message = "Enter a valid email address.")
                    @Size(max = 190, message = "Use 190 characters or fewer.")
                    String email,
            // BCrypt only accepts 72 bytes, so longer passwords are refused rather than cut short.
            @NotBlank(message = "Enter a password.")
                    @Size(min = 8, max = 72, message = "Use between 8 and 72 characters.")
                    String password,
            @Pattern(regexp = "^(\\+91[ -]?)?[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number.")
                    String phone) {}

    public record LoginRequest(
            @NotBlank(message = "Enter your email address.") String email,
            @NotBlank(message = "Enter your password.") String password) {}

    public record UserResponse(
            long id,
            String fullName,
            String email,
            @Schema(types = {"string", "null"}) String phone,
            UserRole role) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
        }
    }
}
