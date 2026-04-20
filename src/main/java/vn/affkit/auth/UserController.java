package vn.affkit.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.affkit.auth.dto.ChangePasswordRequest;
import vn.affkit.auth.dto.UpdateProfileRequest;
import vn.affkit.auth.dto.UserProfileResponse;
import vn.affkit.auth.entity.User;
import vn.affkit.auth.repository.UserRepository;
import vn.affkit.common.ApiResponse;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.repository.LinkRepository;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LinkRepository linkRepository;

    // GET /api/v1/users/me — lấy thông tin profile
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        long linksUsed = linkRepository.countByUserIdAndDeletedFalse(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user, linksUsed)));
    }

    // PATCH /api/v1/users/me — cập nhật fullName
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest req) {
        user.setFullName(req.fullName().trim());
        userRepository.save(user);
        long linksUsed = linkRepository.countByUserIdAndDeletedFalse(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user, linksUsed)));
    }

    // POST /api/v1/users/me/change-password — đổi mật khẩu
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest req) {

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(
                java.util.Map.of("message", "Đổi mật khẩu thành công")));
    }
}