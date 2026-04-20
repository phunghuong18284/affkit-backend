package vn.affkit.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.affkit.auth.dto.*;
import vn.affkit.auth.service.AuthService;
import vn.affkit.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody RegisterRequest req) {
        var result = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req, httpReq, httpRes)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(httpReq, httpRes)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        authService.logout(httpReq, httpRes);
        return ResponseEntity.ok(ApiResponse.ok(
                java.util.Map.of("message", "Dang xuat thanh cong")));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @RequestParam String token,
            HttpServletResponse httpRes) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmail(token, httpRes)));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(
            @RequestBody java.util.Map<String, String> body) {
        var result = authService.resendVerification(body.get("email"));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @RequestBody java.util.Map<String, String> body) {
        var result = authService.forgotPassword(body.get("email"));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @RequestBody java.util.Map<String, String> body) {
        var result = authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}