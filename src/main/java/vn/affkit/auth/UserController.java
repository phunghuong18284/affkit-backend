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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LinkRepository linkRepository;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        long linksUsed = linkRepository.countByUserIdAndDeletedFalse(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user, linksUsed)));
    }

    // PATCH /api/v1/users/me
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest req) {
        user.setFullName(req.fullName().trim());
        userRepository.save(user);
        long linksUsed = linkRepository.countByUserIdAndDeletedFalse(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user, linksUsed)));
    }

    // POST /api/v1/users/me/change-password
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
                Map.of("message", "Doi mat khau thanh cong")));
    }

    // PUT /api/v1/users/me/accesstrade-key — lưu API key AccessTrade
    @PutMapping("/me/accesstrade-key")
    public ResponseEntity<ApiResponse<Object>> saveAccessTradeKey(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        user.setAccesstradeApiKey(apiKey.trim());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("message", "Luu API key thanh cong")));
    }

    // DELETE /api/v1/users/me/accesstrade-key — xóa API key
    @DeleteMapping("/me/accesstrade-key")
    public ResponseEntity<ApiResponse<Object>> deleteAccessTradeKey(
            @AuthenticationPrincipal User user) {

        user.setAccesstradeApiKey(null);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("message", "Da xoa API key")));
    }
}