package vn.affkit.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.affkit.auth.dto.*;
import vn.affkit.auth.entity.*;
import vn.affkit.auth.repository.*;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.session.refresh-token-ttl-days:7}")
    private int refreshTokenTtlDays;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    // ─── Register ────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(req.email().toLowerCase().trim())
                .fullName(req.fullName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailToken emailToken = EmailToken.builder()
                .user(user)
                .token(UUID.fromString(token))
                .type("EMAIL_VERIFY")
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        emailTokenRepository.save(emailToken);

        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("User registered: {}", user.getEmail());
        return Map.of("message",
                "Dang ky thanh cong. Vui long kiem tra email de xac nhan tai khoan.");
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req,
                              HttpServletRequest httpReq,
                              HttpServletResponse httpRes) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.isLocked()) {
            if (user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
                user.setLocked(false);
                user.setLockedUntil(null);
                user.setFailedAttempts((short) 0);
            } else {
                throw new AppException(ErrorCode.ACCOUNT_LOCKED);
            }
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttemptService.handleFailedLogin(user.getId());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts((short) 0);
            user.setLocked(false);
            user.setLockedUntil(null);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceInfo(extractDeviceInfo(httpReq))
                .ipAddress(extractIpAddress(httpReq))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build();
        userSessionRepository.save(session);
        userRepository.save(user);

        setRefreshTokenCookie(httpRes, refreshToken);
        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.of(accessToken, jwtService.getAccessTokenTtlSeconds());
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        String refreshToken = extractRefreshTokenFromCookie(httpReq);

        UserSession session = userSessionRepository
                .findByRefreshTokenAndIsRevokedFalse(refreshToken)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (Instant.now().isAfter(session.getExpiresAt())) {
            session.setRevoked(true);
            userSessionRepository.save(session);
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = session.getUser();
        session.setRevoked(true);
        userSessionRepository.save(session);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken();

        UserSession newSession = UserSession.builder()
                .user(user)
                .refreshToken(newRefreshToken)
                .deviceInfo(extractDeviceInfo(httpReq))
                .ipAddress(extractIpAddress(httpReq))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build();
        userSessionRepository.save(newSession);

        setRefreshTokenCookie(httpRes, newRefreshToken);
        return AuthResponse.of(newAccessToken, jwtService.getAccessTokenTtlSeconds());
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Transactional
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        try {
            String refreshToken = extractRefreshTokenFromCookie(httpReq);
            userSessionRepository.findByRefreshTokenAndIsRevokedFalse(refreshToken)
                    .ifPresent(s -> {
                        s.setRevoked(true);
                        userSessionRepository.save(s);
                    });
        } catch (Exception ignored) {}
        clearRefreshTokenCookie(httpRes);
    }

    // ─── Verify Email ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse verifyEmail(String tokenStr, HttpServletResponse httpRes) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(tokenStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        EmailToken emailToken = emailTokenRepository
                .findByTokenAndUsedFalse(tokenUuid)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (!"EMAIL_VERIFY".equals(emailToken.getType())) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        if (Instant.now().isAfter(emailToken.getExpiresAt())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);

        User user = emailToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build();
        userSessionRepository.save(session);
        setRefreshTokenCookie(httpRes, refreshToken);

        return AuthResponse.of(accessToken, jwtService.getAccessTokenTtlSeconds(),
                "Email da duoc xac nhan thanh cong");
    }

    // ─── Forgot Password ─────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> forgotPassword(String email) {
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            EmailToken emailToken = EmailToken.builder()
                    .user(user)
                    .token(UUID.fromString(token))
                    .type("PASSWORD_RESET")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            emailTokenRepository.save(emailToken);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("Password reset email sent to: {}", user.getEmail());
        });

        return Map.of("message",
                "Neu email ton tai, ban se nhan duoc huong dan dat lai mat khau.");
    }

    // ─── Reset Password ──────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> resetPassword(String tokenStr, String newPassword) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(tokenStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        EmailToken emailToken = emailTokenRepository
                .findByTokenAndUsedFalse(tokenUuid)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (!"PASSWORD_RESET".equals(emailToken.getType())) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        if (Instant.now().isAfter(emailToken.getExpiresAt())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);

        User user = emailToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        userSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()) && !s.isRevoked())
                .forEach(s -> {
                    s.setRevoked(true);
                    userSessionRepository.save(s);
                });

        log.info("Password reset for user: {}", user.getEmail());
        return Map.of("message", "Mat khau da duoc dat lai thanh cong.");
    }

    // ─── Resend Verification ─────────────────────────────────────────────────

    @Transactional
    public Map<String, String> resendVerification(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            return Map.of("message", "Email nay da duoc xac nhan roi.");
        }

        String token = UUID.randomUUID().toString();
        EmailToken emailToken = EmailToken.builder()
                .user(user)
                .token(UUID.fromString(token))
                .type("EMAIL_VERIFY")
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        emailTokenRepository.save(emailToken);
        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("Verification email resent to: {}", user.getEmail());
        return Map.of("message", "Email xac nhan da duoc gui lai.");
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(User user) {
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        short attempts = (short) (freshUser.getFailedAttempts() + 1);
        freshUser.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            freshUser.setLocked(true);
            freshUser.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            log.warn("Account locked: {}", freshUser.getEmail());
        }
        userRepository.save(freshUser);
    }

    private void setRefreshTokenCookie(HttpServletResponse res, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(refreshTokenTtlDays * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        res.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse res) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest req) {
        if (req.getCookies() == null) throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        return Arrays.stream(req.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    private String extractIpAddress(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isBlank()) ? ip.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String extractDeviceInfo(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}