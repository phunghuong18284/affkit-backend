package vn.affkit.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 2, max = 255)
        String fullName,

        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
                message = "Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số")
        String password
) {}