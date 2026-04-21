package vn.affkit.campaign.dto;

import vn.affkit.campaign.entity.Campaign;

import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID    id,
        String  name,
        String  description,
        long    totalClicks,
        long    linkCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignResponse from(Campaign campaign, long totalClicks, long linkCount) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                totalClicks,
                linkCount,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}