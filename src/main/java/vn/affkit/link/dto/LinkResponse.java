package vn.affkit.link.dto;

import vn.affkit.link.entity.Link;
import vn.affkit.link.entity.LinkTag;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LinkResponse(
        UUID    id,
        String  shortCode,
        String  shortUrl,
        String  originalUrl,
        String  title,
        String  platform,
        String  affiliateUrl,
        UUID    campaignId,
        List<String> tags,
        Instant createdAt
) {
    public static LinkResponse from(Link link, String baseUrl) {
        List<String> tagNames = link.getTags().stream()
                .map(LinkTag::getTag)
                .toList();

        return new LinkResponse(
                link.getId(),
                link.getShortCode(),
                baseUrl + link.getShortCode(),
                link.getOriginalUrl(),
                link.getTitle(),
                link.getPlatform(),
                link.getAffiliateUrl(),
                link.getCampaignId(),
                tagNames,
                link.getCreatedAt()
        );
    }
}