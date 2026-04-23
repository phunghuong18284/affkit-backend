package vn.affkit.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.affkit.auth.dto.AuthResponse;
import vn.affkit.auth.dto.LoginRequest;
import vn.affkit.auth.dto.RegisterRequest;
import vn.affkit.auth.entity.EmailToken;
import vn.affkit.auth.entity.User;
import vn.affkit.auth.entity.UserSession;
import vn.affkit.auth.repository.EmailTokenRepository;
import vn.affkit.auth.repository.UserRepository;
import vn.affkit.auth.repository.UserSessionRepository;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserSessionRepository userSessionRepository;
    @Mock EmailTokenRepository emailTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;

    // Mock HTTP objects
    @Mock HttpServletRequest httpRequest;
    @Mock HttpServletResponse httpResponse;

    @BeforeEach
    void setUp() {
        // Inject @Value field refreshTokenTtlDays = 7
        ReflectionTestUtils.setField(authService, "refreshTokenTtlDays", 7);
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("✅ Đăng ký thành công → lưu user + gửi email")
        void register_success() {
            // Arrange
            RegisterRequest req = new RegisterRequest("test@gmail.com", "Nguyen Van A", "Password123");
            when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(emailTokenRepository.save(any(EmailToken.class))).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            // Act
            Map<String, String> result = authService.register(req);

            // Assert
            assertThat(result).containsKey("message");
            verify(userRepository).save(argThat(u ->
                    u.getEmail().equals("test@gmail.com") &&
                    u.getPasswordHash().equals("hashed_password")
            ));
            verify(emailTokenRepository).save(argThat(t ->
                    "EMAIL_VERIFY".equals(t.getType())
            ));
            verify(emailService).sendVerificationEmail(eq("test@gmail.com"), anyString());
        }

        @Test
        @DisplayName("❌ Email đã tồn tại → throw EMAIL_ALREADY_EXISTS")
        void register_emailAlreadyExists() {
            // Arrange
            RegisterRequest req = new RegisterRequest("existing@gmail.com", "Nguyen Van A", "Password123");
            when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("✅ Email được lowercase và trim trước khi lưu")
        void register_emailNormalized() {
            // Arrange
            RegisterRequest req = new RegisterRequest("  TEST@Gmail.COM  ", "Nguyen Van A", "Password123");
            when(userRepository.existsByEmail("  TEST@Gmail.COM  ")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(emailTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            // Act
            authService.register(req);

            // Assert
            verify(userRepository).save(argThat(u ->
                    u.getEmail().equals("test@gmail.com")
            ));
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Nested
    @DisplayName("login()")
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    class LoginTests {

        private User verifiedUser;

        @BeforeEach
        void setUpUser() {
            verifiedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(true)
                    .isLocked(false)
                    .failedAttempts((short) 0)
                    .plan("FREE")
                    .build();

            when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("✅ Login thành công → trả accessToken + set cookie")
        void login_success() {
            // Arrange
            LoginRequest req = new LoginRequest("user@gmail.com", "correct_password");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("correct_password", "hashed_pw")).thenReturn(true);
            when(jwtService.generateAccessToken(verifiedUser)).thenReturn("access_token_xyz");
            when(jwtService.generateRefreshToken()).thenReturn("refresh_token_abc");
            when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            AuthResponse response = authService.login(req, httpRequest, httpResponse);

            // Assert
            assertThat(response.accessToken()).isEqualTo("access_token_xyz");
            verify(userSessionRepository).save(argThat(s ->
                    s.getRefreshToken().equals("refresh_token_abc")
            ));
            verify(httpResponse).addCookie(argThat(c ->
                    "refresh_token".equals(c.getName()) &&
                    "refresh_token_abc".equals(c.getValue()) &&
                    c.isHttpOnly()
            ));
        }

        @Test
        @DisplayName("❌ Email không tồn tại → throw INVALID_CREDENTIALS")
        void login_emailNotFound() {
            // Arrange
            LoginRequest req = new LoginRequest("notfound@gmail.com", "password");
            when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(req, httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        }

        @Test
        @DisplayName("❌ Sai mật khẩu → throw INVALID_CREDENTIALS + tăng failedAttempts")
        void login_wrongPassword() {
            // Arrange
            LoginRequest req = new LoginRequest("user@gmail.com", "wrong_password");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(verifiedUser));
            when(passwordEncoder.matches("wrong_password", "hashed_pw")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(req, httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

            // failedAttempts phải tăng lên 1
            verify(userRepository).save(argThat(u -> u.getFailedAttempts() == 1));
        }

        @Test
        @DisplayName("❌ Chưa verify email → throw ACCOUNT_NOT_VERIFIED")
        void login_notVerified() {
            // Arrange
            User unverifiedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(false)
                    .isLocked(false)
                    .failedAttempts((short) 0)
                    .build();
            LoginRequest req = new LoginRequest("user@gmail.com", "correct_password");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(unverifiedUser));
            when(passwordEncoder.matches("correct_password", "hashed_pw")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(req, httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_NOT_VERIFIED));
        }

        @Test
        @DisplayName("❌ Tài khoản bị lock → throw ACCOUNT_LOCKED")
        void login_accountLocked() {
            // Arrange
            User lockedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(true)
                    .isLocked(true)
                    .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .failedAttempts((short) 5)
                    .build();
            LoginRequest req = new LoginRequest("user@gmail.com", "password");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(lockedUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(req, httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
        }

        @Test
        @DisplayName("✅ Tài khoản lock nhưng đã hết thời gian → tự mở khóa, login được")
        void login_lockExpired_autoUnlock() {
            // Arrange
            User expiredLockUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(true)
                    .isLocked(true)
                    .lockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES)) // đã hết hạn lock
                    .failedAttempts((short) 5)
                    .plan("FREE")
                    .build();
            LoginRequest req = new LoginRequest("user@gmail.com", "correct_password");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(expiredLockUser));
            when(passwordEncoder.matches("correct_password", "hashed_pw")).thenReturn(true);
            when(jwtService.generateAccessToken(any())).thenReturn("access_token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh_token");
            when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            AuthResponse response = authService.login(req, httpRequest, httpResponse);

            // Assert
            assertThat(response.accessToken()).isEqualTo("access_token");
        }

        @Test
        @DisplayName("❌ Sai mật khẩu 5 lần → tài khoản bị lock")
        void login_5FailedAttempts_accountLocked() {
            // Arrange
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(true)
                    .isLocked(false)
                    .failedAttempts((short) 4) // lần này là lần thứ 5
                    .plan("FREE")
                    .build();
            LoginRequest req = new LoginRequest("user@gmail.com", "wrong");
            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(req, httpRequest, httpResponse))
                    .isInstanceOf(AppException.class);

            verify(userRepository).save(argThat(u ->
                    u.isLocked() && u.getLockedUntil() != null
            ));
        }
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("✅ Refresh token hợp lệ → trả token mới + revoke cũ")
        void refresh_success() {
            // Arrange
            String oldRefreshToken = "old-refresh-token";
            Cookie cookie = new Cookie("refresh_token", oldRefreshToken);
            when(httpRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .plan("FREE")
                    .build();
            UserSession session = UserSession.builder()
                    .user(user)
                    .refreshToken(oldRefreshToken)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .isRevoked(false)
                    .build();

            when(userSessionRepository.findByRefreshTokenAndIsRevokedFalse(oldRefreshToken))
                    .thenReturn(Optional.of(session));
            when(jwtService.generateAccessToken(user)).thenReturn("new_access_token");
            when(jwtService.generateRefreshToken()).thenReturn("new_refresh_token");
            when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            AuthResponse response = authService.refresh(httpRequest, httpResponse);

            // Assert
            assertThat(response.accessToken()).isEqualTo("new_access_token");
            // Session cũ phải bị revoke
            assertThat(session.isRevoked()).isTrue();
            // Session mới phải được tạo
            verify(userSessionRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("❌ Không có cookie → throw REFRESH_TOKEN_INVALID")
        void refresh_noCookie() {
            // Arrange
            when(httpRequest.getCookies()).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("❌ Refresh token không tồn tại trong DB → throw REFRESH_TOKEN_INVALID")
        void refresh_tokenNotFound() {
            // Arrange
            Cookie cookie = new Cookie("refresh_token", "invalid-token");
            when(httpRequest.getCookies()).thenReturn(new Cookie[]{cookie});
            when(userSessionRepository.findByRefreshTokenAndIsRevokedFalse("invalid-token"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("❌ Refresh token hết hạn → throw REFRESH_TOKEN_EXPIRED + revoke session")
        void refresh_tokenExpired() {
            // Arrange
            String expiredToken = "expired-token";
            Cookie cookie = new Cookie("refresh_token", expiredToken);
            when(httpRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            User user = User.builder().id(UUID.randomUUID()).build();
            UserSession expiredSession = UserSession.builder()
                    .user(user)
                    .refreshToken(expiredToken)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)) // đã hết hạn
                    .isRevoked(false)
                    .build();

            when(userSessionRepository.findByRefreshTokenAndIsRevokedFalse(expiredToken))
                    .thenReturn(Optional.of(expiredSession));
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> authService.refresh(httpRequest, httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED));

            // Session phải bị revoke
            assertThat(expiredSession.isRevoked()).isTrue();
        }
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        private User unverifiedUser;
        private UUID validTokenUuid;
        private EmailToken validEmailToken;

        @BeforeEach
        void setUp() {
            unverifiedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("hashed_pw")
                    .isVerified(false)
                    .plan("FREE")
                    .build();

            validTokenUuid = UUID.randomUUID();
            validEmailToken = EmailToken.builder()
                    .token(validTokenUuid)
                    .user(unverifiedUser)
                    .type("EMAIL_VERIFY")
                    .used(false)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .build();
        }

        @Test
        @DisplayName("✅ Token hợp lệ → user.isVerified=true + trả accessToken + set cookie")
        void verifyEmail_success() {
            when(emailTokenRepository.findByTokenAndUsedFalse(validTokenUuid))
                    .thenReturn(Optional.of(validEmailToken));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(emailTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateAccessToken(unverifiedUser)).thenReturn("access_token_ok");
            when(jwtService.generateRefreshToken()).thenReturn("refresh_token_ok");
            when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.verifyEmail(validTokenUuid.toString(), httpResponse);

            assertThat(response.accessToken()).isEqualTo("access_token_ok");
            assertThat(unverifiedUser.isVerified()).isTrue();
            assertThat(validEmailToken.isUsed()).isTrue();
            verify(httpResponse).addCookie(argThat(c -> "refresh_token".equals(c.getName())));
        }

        @Test
        @DisplayName("❌ Token sai format UUID → throw TOKEN_INVALID")
        void verifyEmail_invalidUuidFormat() {
            assertThatThrownBy(() -> authService.verifyEmail("not-a-valid-uuid!!!", httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));

            verifyNoInteractions(emailTokenRepository);
        }

        @Test
        @DisplayName("❌ Token đã dùng (used=true) → throw TOKEN_INVALID")
        void verifyEmail_tokenAlreadyUsed() {
            when(emailTokenRepository.findByTokenAndUsedFalse(validTokenUuid))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail(validTokenUuid.toString(), httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));
        }

        @Test
        @DisplayName("❌ Token hết hạn → throw TOKEN_EXPIRED + user không được verify")
        void verifyEmail_tokenExpired() {
            EmailToken expiredToken = EmailToken.builder()
                    .token(validTokenUuid)
                    .user(unverifiedUser)
                    .type("EMAIL_VERIFY")
                    .used(false)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            when(emailTokenRepository.findByTokenAndUsedFalse(validTokenUuid))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.verifyEmail(validTokenUuid.toString(), httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_EXPIRED));

            assertThat(unverifiedUser.isVerified()).isFalse();
        }

        @Test
        @DisplayName("❌ Token type sai (PASSWORD_RESET dùng nhầm) → throw TOKEN_INVALID")
        void verifyEmail_wrongTokenType() {
            EmailToken wrongTypeToken = EmailToken.builder()
                    .token(validTokenUuid)
                    .user(unverifiedUser)
                    .type("PASSWORD_RESET")
                    .used(false)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            when(emailTokenRepository.findByTokenAndUsedFalse(validTokenUuid))
                    .thenReturn(Optional.of(wrongTypeToken));

            assertThatThrownBy(() -> authService.verifyEmail(validTokenUuid.toString(), httpResponse))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));
        }
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        private User existingUser;
        private UUID resetTokenUuid;
        private EmailToken resetToken;

        @BeforeEach
        void setUp() {
            existingUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("user@gmail.com")
                    .passwordHash("old_hashed_pw")
                    .isVerified(true)
                    .plan("FREE")
                    .build();

            resetTokenUuid = UUID.randomUUID();
            resetToken = EmailToken.builder()
                    .token(resetTokenUuid)
                    .user(existingUser)
                    .type("PASSWORD_RESET")
                    .used(false)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
        }

        @Test
        @DisplayName("✅ Reset thành công → password đổi + tất cả session bị revoke")
        void resetPassword_success() {
            UserSession activeSession1 = UserSession.builder()
                    .user(existingUser)
                    .refreshToken("token-1")
                    .isRevoked(false)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            UserSession activeSession2 = UserSession.builder()
                    .user(existingUser)
                    .refreshToken("token-2")
                    .isRevoked(false)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            when(emailTokenRepository.findByTokenAndUsedFalse(resetTokenUuid))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("NewPassword123")).thenReturn("new_hashed_pw");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(emailTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userSessionRepository.findAll()).thenReturn(List.of(activeSession1, activeSession2));
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Map<String, String> result = authService.resetPassword(resetTokenUuid.toString(), "NewPassword123");

            assertThat(result).containsKey("message");
            assertThat(existingUser.getPasswordHash()).isEqualTo("new_hashed_pw");
            assertThat(resetToken.isUsed()).isTrue();
            assertThat(activeSession1.isRevoked()).isTrue();
            assertThat(activeSession2.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("❌ Token sai format UUID → throw TOKEN_INVALID")
        void resetPassword_invalidUuidFormat() {
            assertThatThrownBy(() -> authService.resetPassword("bad-uuid!!!", "NewPass123"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));
        }

        @Test
        @DisplayName("❌ Token không tồn tại / đã dùng → throw TOKEN_INVALID")
        void resetPassword_tokenNotFound() {
            when(emailTokenRepository.findByTokenAndUsedFalse(resetTokenUuid))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(resetTokenUuid.toString(), "NewPass123"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Token hết hạn → throw TOKEN_EXPIRED + password không đổi")
        void resetPassword_tokenExpired() {
            EmailToken expiredToken = EmailToken.builder()
                    .token(resetTokenUuid)
                    .user(existingUser)
                    .type("PASSWORD_RESET")
                    .used(false)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            when(emailTokenRepository.findByTokenAndUsedFalse(resetTokenUuid))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.resetPassword(resetTokenUuid.toString(), "NewPass123"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_EXPIRED));

            assertThat(existingUser.getPasswordHash()).isEqualTo("old_hashed_pw");
        }

        @Test
        @DisplayName("❌ Token type sai (EMAIL_VERIFY dùng nhầm) → throw TOKEN_INVALID")
        void resetPassword_wrongTokenType() {
            EmailToken wrongType = EmailToken.builder()
                    .token(resetTokenUuid)
                    .user(existingUser)
                    .type("EMAIL_VERIFY")
                    .used(false)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            when(emailTokenRepository.findByTokenAndUsedFalse(resetTokenUuid))
                    .thenReturn(Optional.of(wrongType));

            assertThatThrownBy(() -> authService.resetPassword(resetTokenUuid.toString(), "NewPass123"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TOKEN_INVALID));
        }

        @Test
        @DisplayName("✅ Chỉ revoke session của đúng user, không revoke session user khác")
        void resetPassword_onlyRevokesCurrentUserSessions() {
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("other@gmail.com")
                    .build();

            UserSession mySession = UserSession.builder()
                    .user(existingUser)
                    .refreshToken("my-token")
                    .isRevoked(false)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            UserSession otherSession = UserSession.builder()
                    .user(otherUser)
                    .refreshToken("other-token")
                    .isRevoked(false)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();

            when(emailTokenRepository.findByTokenAndUsedFalse(resetTokenUuid))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(any())).thenReturn("new_hash");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(emailTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userSessionRepository.findAll()).thenReturn(List.of(mySession, otherSession));
            when(userSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            authService.resetPassword(resetTokenUuid.toString(), "NewPass123");

            assertThat(mySession.isRevoked()).isTrue();
            assertThat(otherSession.isRevoked()).isFalse();
        }
    }
}
