package vn.affkit.campaign.dto;

import vn.affkit.campaign.entity.Campaign;

import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID    id,
        String  name,
        String  description,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}