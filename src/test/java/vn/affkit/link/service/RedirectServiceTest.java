package vn.affkit.link.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.entity.Link;
import vn.affkit.link.repository.LinkRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedirectService Tests")
class RedirectServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock LinkRepository linkRepository;
    @Mock ClickLogService clickLogService;
    @Mock HttpServletRequest httpRequest;

    @InjectMocks RedirectService redirectService;

    @Nested
    @DisplayName("resolveUrl()")
    class ResolveUrlTests {

        @Test
        @DisplayName("✅ Redis HIT → trả URL ngay, không query DB")
        void resolveUrl_redisHit() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("link:abc123")).thenReturn("https://shopee.vn/product/123");

            // Act
            String url = redirectService.resolveUrl("abc123");

            // Assert
            assertThat(url).isEqualTo("https://shopee.vn/product/123");
            verify(linkRepository, never()).findByShortCode(anyString());
        }

        @Test
        @DisplayName("✅ Redis MISS → query DB + warm cache + trả URL đúng")
        void resolveUrl_redisMiss_dbHit() {
            // Arrange
            Link link = Link.builder()
                    .id(UUID.randomUUID())
                    .shortCode("xyz789")
                    .originalUrl("https://lazada.vn/product/456")
                    .build(); // deleted = false by default

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("link:xyz789")).thenReturn(null); // Redis MISS
            when(linkRepository.findByShortCode("xyz789")).thenReturn(Optional.of(link));

            // Act
            String url = redirectService.resolveUrl("xyz789");

            // Assert
            assertThat(url).isEqualTo("https://lazada.vn/product/456");
            // Cache phải được warm
            verify(valueOps).set(
                    eq("link:xyz789"),
                    eq("https://lazada.vn/product/456"),
                    eq(Duration.ofHours(1))
            );
        }

        @Test
        @DisplayName("❌ Short code không tồn tại → throw LINK_NOT_FOUND")
        void resolveUrl_shortCodeNotFound() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("link:notexist")).thenReturn(null);
            when(linkRepository.findByShortCode("notexist")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> redirectService.resolveUrl("notexist"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.LINK_NOT_FOUND));
        }

        @Test
        @DisplayName("❌ Link đã soft-delete → throw LINK_DELETED")
        void resolveUrl_linkDeleted() {
            // Arrange
            Link deletedLink = Link.builder()
                    .id(UUID.randomUUID())
                    .shortCode("del123")
                    .originalUrl("https://shopee.vn/deleted")
                    .deleted(true) // đã xóa
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("link:del123")).thenReturn(null);
            when(linkRepository.findByShortCode("del123")).thenReturn(Optional.of(deletedLink));

            // Act & Assert
            assertThatThrownBy(() -> redirectService.resolveUrl("del123"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.LINK_DELETED));

            // Cache không được warm với link đã xóa
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("logClickAsync()")
    class LogClickAsyncTests {

        @Test
        @DisplayName("✅ logClickAsync → delegate sang ClickLogService.logAsync()")
        void logClickAsync_delegatesToClickLogService() {
            // Act
            redirectService.logClickAsync("abc123", httpRequest);

            // Assert
            verify(clickLogService).logAsync("abc123", httpRequest);
        }
    }
}
