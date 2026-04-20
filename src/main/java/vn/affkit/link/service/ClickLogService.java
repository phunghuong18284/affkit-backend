package vn.affkit.link.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.affkit.link.entity.LinkClick;
import vn.affkit.link.repository.LinkClickRepository;
import vn.affkit.link.repository.LinkRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickLogService {

    private final LinkClickRepository clickRepository;
    private final LinkRepository linkRepository;

    @Async("clickLogExecutor")
    public void logAsync(String shortCode, HttpServletRequest request) {
        try {
            linkRepository.findByShortCode(shortCode).ifPresent(link -> {
                String ua = request.getHeader("User-Agent");
                if (isBot(ua)) return;

                LinkClick click = LinkClick.builder()
                        .linkId(link.getId())
                        .clickedAt(Instant.now())
                        .referrer(request.getHeader("Referer"))
                        .ipHash(hashIp(getClientIp(request)))
                        .userAgent(ua)
                        .deviceType(detectDevice(ua))
                        .source(detectSource(request.getHeader("Referer")))
                        .build();

                clickRepository.save(click);
            });
        } catch (Exception e) {
            log.warn("Click log thất bại cho {}: {}", shortCode, e.getMessage());
        }
    }

    private String hashIp(String ip) {
        try {
            String daily = ip + LocalDate.now();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(daily.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String detectDevice(String ua) {
        if (ua == null) return "UNKNOWN";
        ua = ua.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "MOBILE";
        if (ua.contains("tablet") || ua.contains("ipad")) return "TABLET";
        return "DESKTOP";
    }

    private String detectSource(String referer) {
        if (referer == null || referer.isBlank()) return "DIRECT";
        if (referer.contains("zalo")) return "ZALO";
        if (referer.contains("t.me") || referer.contains("telegram")) return "TELEGRAM";
        if (referer.contains("facebook") || referer.contains("fb.com")) return "FACEBOOK";
        return "OTHER";
    }

    private boolean isBot(String ua) {
        if (ua == null) return true;
        String u = ua.toLowerCase();
        return u.contains("bot") || u.contains("crawler") || u.contains("spider")
                || u.contains("curl") || u.contains("wget") || u.contains("python");
    }
}