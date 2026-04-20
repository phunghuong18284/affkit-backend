package vn.affkit.link.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.entity.Link;
import vn.affkit.link.repository.LinkRepository;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private final StringRedisTemplate redisTemplate;
    private final LinkRepository linkRepository;
    private final ClickLogService clickLogService;

    public String resolveUrl(String shortCode) {
        // 1. Redis HIT → trả về ngay < 5ms
        String cached = redisTemplate.opsForValue().get("link:" + shortCode);
        if (cached != null) return cached;

        // 2. Redis MISS → query DB
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));

        if (link.isDeleted()) {
            throw new AppException(ErrorCode.LINK_DELETED);
        }

        // 3. Warm cache
        redisTemplate.opsForValue().set(
                "link:" + shortCode,
                link.getOriginalUrl(),
                Duration.ofHours(1)
        );

        return link.getOriginalUrl();
    }

    public void logClickAsync(String shortCode, HttpServletRequest request) {
        clickLogService.logAsync(shortCode, request);
    }
}