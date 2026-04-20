package vn.affkit.campaign.dto;

import jakarta.validation.constraints.Size;

public record UpdateCampaignRequest(

        @Size(max = 255)
        String name,

        @Size(max = 1000)
        String description
) {}