package vn.affkit.campaign.dto;

import vn.affkit.campaign.entity.Campaign;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CampaignResponse(
        UUID      id,
        String    name,
        String    description,
        LocalDate startDate,
        LocalDate endDate,
        String    status,
        long      totalClicks,
        long      linkCount,
        Instant   createdAt,
        Instant   updatedAt
) {
    public static CampaignResponse from(Campaign campaign, long totalClicks, long linkCount) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                computeStatus(campaign),
                totalClicks,
                linkCount,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    private static String computeStatus(Campaign campaign) {
        LocalDate today = LocalDate.now();
        LocalDate start = campaign.getStartDate();
        LocalDate end   = campaign.getEndDate();

        if (start == null && end == null) return "ACTIVE";
        if (end != null && today.isAfter(end)) return "ENDED";
        if (start != null && today.isBefore(start)) return "UPCOMING";
        return "ACTIVE";
    }
}