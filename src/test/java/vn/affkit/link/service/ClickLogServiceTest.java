package vn.affkit.link.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.affkit.link.entity.Link;
import vn.affkit.link.entity.LinkClick;
import vn.affkit.link.repository.LinkClickRepository;
import vn.affkit.link.repository.LinkRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickLogService Tests")
class ClickLogServiceTest {

    @Mock LinkClickRepository clickRepository;
    @Mock LinkRepository linkRepository;
    @Mock HttpServletRequest request;

    @InjectMocks ClickLogService clickLogService;

    // Helper tạo Link
    private Link makeLink(String shortCode) {
        return Link.builder()
                .id(UUID.randomUUID())
                .shortCode(shortCode)
                .originalUrl("https://tiki.vn/product/123")
                .build();
    }

    // =========================================================
    // BOT DETECTION
    // =========================================================

    @Nested
    @DisplayName("Bot detection")
    class BotDetectionTests {

        @Test
        @DisplayName("❌ User-Agent null → coi là bot, không lưu click")
        void logAsync_nullUserAgent_isBot() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn(null);

            clickLogService.logAsync("abc123", request);

            verify(clickRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ User-Agent chứa 'bot' → không lưu click")
        void logAsync_botUserAgent_notSaved() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Googlebot/2.1 (+http://www.google.com/bot.html)");

            clickLogService.logAsync("abc123", request);

            verify(clickRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ User-Agent chứa 'crawler' → không lưu click")
        void logAsync_crawlerUserAgent_notSaved() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (compatible; AhrefsCrawler)");

            clickLogService.logAsync("abc123", request);

            verify(clickRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ User-Agent chứa 'curl' → không lưu click")
        void logAsync_curlUserAgent_notSaved() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("curl/7.68.0");

            clickLogService.logAsync("abc123", request);

            verify(clickRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ User-Agent bình thường → lưu click")
        void logAsync_normalUserAgent_saved() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            verify(clickRepository).save(any(LinkClick.class));
        }
    }

    // =========================================================
    // DEVICE DETECTION
    // =========================================================

    @Nested
    @DisplayName("Device detection")
    class DeviceDetectionTests {

        @Test
        @DisplayName("✅ User-Agent mobile → deviceType = MOBILE")
        void logAsync_mobileUserAgent_deviceMobile() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent"))
                    .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0) Mobile/15E148 Safari/604.1");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getDeviceType()).isEqualTo("MOBILE");
        }

        @Test
        @DisplayName("✅ User-Agent android → deviceType = MOBILE")
        void logAsync_androidUserAgent_deviceMobile() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent"))
                    .thenReturn("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getDeviceType()).isEqualTo("MOBILE");
        }

        @Test
        @DisplayName("✅ User-Agent iPad → deviceType = TABLET")
        void logAsync_ipadUserAgent_deviceTablet() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent"))
                    .thenReturn("Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getDeviceType()).isEqualTo("TABLET");
        }

        @Test
        @DisplayName("✅ User-Agent desktop Chrome → deviceType = DESKTOP")
        void logAsync_desktopUserAgent_deviceDesktop() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent"))
                    .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getDeviceType()).isEqualTo("DESKTOP");
        }
    }

    // =========================================================
    // SOURCE DETECTION
    // =========================================================

    @Nested
    @DisplayName("Source detection")
    class SourceDetectionTests {

        private void setupBasicMocks(String referer) {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0");
            when(request.getHeader("Referer")).thenReturn(referer);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("✅ Referer null → source = DIRECT")
        void logAsync_noReferer_sourceDirect() {
            setupBasicMocks(null);

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("DIRECT");
        }

        @Test
        @DisplayName("✅ Referer chứa 'zalo' → source = ZALO")
        void logAsync_zaloReferer_sourceZalo() {
            setupBasicMocks("https://zalo.me/share/link");

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("ZALO");
        }

        @Test
        @DisplayName("✅ Referer chứa 't.me' → source = TELEGRAM")
        void logAsync_telegramReferer_sourceTelegram() {
            setupBasicMocks("https://t.me/channel/post123");

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("TELEGRAM");
        }

        @Test
        @DisplayName("✅ Referer chứa 'facebook' → source = FACEBOOK")
        void logAsync_facebookReferer_sourceFacebook() {
            setupBasicMocks("https://www.facebook.com/groups/affiliate");

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("FACEBOOK");
        }

        @Test
        @DisplayName("✅ Referer là trang khác → source = OTHER")
        void logAsync_otherReferer_sourceOther() {
            setupBasicMocks("https://shopee.vn/some-page");

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo("OTHER");
        }
    }

    // =========================================================
    // EDGE CASES
    // =========================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("✅ ShortCode không tồn tại → không throw, không lưu click")
        void logAsync_shortCodeNotFound_noException() {
            when(linkRepository.findByShortCode("notexist")).thenReturn(Optional.empty());

            // Không được throw exception
            clickLogService.logAsync("notexist", request);

            verify(clickRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ IP từ X-Forwarded-For header → dùng IP đó hash")
        void logAsync_xForwardedForHeader_usedForIpHash() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.113.152.100, 10.0.0.1");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            // ipHash phải là chuỗi SHA-256 hex (64 ký tự), không phải IP thô
            assertThat(captor.getValue().getIpHash())
                    .isNotNull()
                    .hasSize(64)
                    .doesNotContain("203.113.152.100");
        }

        @Test
        @DisplayName("✅ ipHash là SHA-256 hex, không lưu IP thô")
        void logAsync_ipHashNotRawIp() {
            when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(makeLink("abc123")));
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0");
            when(request.getHeader("Referer")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(clickRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            clickLogService.logAsync("abc123", request);

            ArgumentCaptor<LinkClick> captor = ArgumentCaptor.forClass(LinkClick.class);
            verify(clickRepository).save(captor.capture());
            assertThat(captor.getValue().getIpHash())
                    .doesNotContain("192.168.1.100")
                    .hasSize(64); // SHA-256 hex = 64 chars
        }
    }
}
