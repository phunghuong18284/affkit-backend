package vn.affkit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Tên không được để trống")
        @Size(min = 2, max = 50, message = "Tên từ 2 đến 50 ký tự")
        String fullName
) {}
