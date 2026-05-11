package vn.affkit.link.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.affkit.auth.entity.User;
import vn.affkit.auth.repository.UserRepository;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.dto.CreateLinkRequest;
import vn.affkit.link.dto.LinkResponse;
import vn.affkit.link.dto.UpdateLinkRequest;
import vn.affkit.link.entity.Link;
import vn.affkit.link.entity.LinkTag;
import vn.affkit.link.repository.LinkRepository;
import vn.affkit.link.repository.LinkTagRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkService {

    private static final int FREE_PLAN_LIMIT = 10;

    private final LinkRepository      linkRepository;
    private final LinkTagRepository   linkTagRepository;
    private final UserRepository      userRepository;
    private final ShortCodeService    shortCodeService;
    private final StringRedisTemplate redisTemplate;

    @Value("${AFFKIT_BASE_URL:http://localhost:8080/go/}")
    private String shortUrlBase;

    @PostConstruct
    public void init() {
        System.out.println("=== SHORT_URL_BASE = " + shortUrlBase + " ===");
    }

    @Transactional
    public LinkResponse create(UUID userId, CreateLinkRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if ("FREE".equals(user.getPlan())) {
            long count = linkRepository.countByUserIdAndDeletedFalse(userId);
            if (count >= FREE_PLAN_LIMIT) {
                throw new AppException(ErrorCode.LINK_LIMIT_EXCEEDED);
            }
        }

        String shortCode = shortCodeService.generate();
        String platform  = detectPlatform(req.originalUrl());

        Link link = Link.builder()
                .user(user)
                .shortCode(shortCode)
                .originalUrl(req.originalUrl())
                .title(req.title())
                .platform(platform)
                .campaignId(req.campaignId())
                .affiliateUrl(req.affiliateUrl())
                .build();
        linkRepository.save(link);

        if (req.tags() != null && !req.tags().isEmpty()) {
            saveTags(link, req.tags());
        }

        try {
            redisTemplate.opsForValue().set(
                    "link:" + shortCode,
                    req.originalUrl(),
                    Duration.ofHours(1)
            );
        } catch (Exception e) {
            // Redis không available, bỏ qua
        }

        return LinkResponse.from(link, shortUrlBase);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> list(UUID userId, String platform, String search, int page, int size) {
        return linkRepository
                .findByUserFiltered(userId, platform, search, PageRequest.of(page, size))
                .map(l -> LinkResponse.from(l, shortUrlBase));
    }

    @Transactional(readOnly = true)
    public LinkResponse getById(UUID userId, UUID linkId) {
        Link link = linkRepository.findByIdAndUserIdAndDeletedFalse(linkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));
        return LinkResponse.from(link, shortUrlBase);
    }

    @Transactional
    public LinkResponse update(UUID userId, UUID linkId, UpdateLinkRequest req) {
        Link link = linkRepository.findByIdAndUserIdAndDeletedFalse(linkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));

        if (req.title()      != null) link.setTitle(req.title());
        if (req.campaignId() != null) link.setCampaignId(req.campaignId());

        if (req.tags() != null) {
            linkTagRepository.deleteByLinkId(link.getId());
            link.getTags().clear();
            saveTags(link, req.tags());
        }

        linkRepository.save(link);
        return LinkResponse.from(link, shortUrlBase);
    }

    @Transactional
    public void delete(UUID userId, UUID linkId) {
        Link link = linkRepository.findByIdAndUserIdAndDeletedFalse(linkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));

        link.setDeleted(true);
        link.setDeletedAt(Instant.now());
        linkRepository.save(link);

        try {
            redisTemplate.delete("link:" + link.getShortCode());
        } catch (Exception e) {
            // Redis không available, bỏ qua
        }
    }

    @Transactional
    public LinkResponse saveAffiliateUrl(UUID userId, UUID linkId, String affiliateUrl) {
        Link link = linkRepository.findByIdAndUserIdAndDeletedFalse(linkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));

        link.setAffiliateUrl(affiliateUrl);
        linkRepository.save(link);
        return LinkResponse.from(link, shortUrlBase);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void saveTags(Link link, List<String> tagNames) {
        List<LinkTag> tags = tagNames.stream()
                .map(name -> LinkTag.builder()
                        .link(link)
                        .tag(name.toLowerCase().trim())
                        .build())
                .toList();
        linkTagRepository.saveAll(tags);
        link.getTags().addAll(tags);
    }

    private String detectPlatform(String url) {
        if (url.contains("shopee.vn"))  return "SHOPEE";
        if (url.contains("lazada.vn"))  return "LAZADA";
        if (url.contains("tiki.vn"))    return "TIKI";
        if (url.contains("tiktok.com")) return "TIKTOK";
        return "OTHER";
    }
}