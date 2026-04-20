package vn.affkit.link.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateLinkRequest(

        @Size(max = 255)
        String title,

        UUID campaignId,

        @Size(max = 5)
        List<@Size(max = 50) String> tags
) {}