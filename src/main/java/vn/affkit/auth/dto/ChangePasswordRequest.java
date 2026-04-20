package vn.affkit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Nhập mật khẩu mới")
        @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
        String newPassword
) {}
