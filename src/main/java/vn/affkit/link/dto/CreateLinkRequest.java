package vn.affkit.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateLinkRequest(

        @NotBlank(message = "URL khong duoc de trong")
        @Size(max = 2048, message = "URL qua dai")
        String originalUrl,

        @Size(max = 255, message = "Ten link toi da 255 ky tu")
        String title,

        UUID campaignId,

        @Size(max = 5, message = "Toi da 5 tags")
        List<@Size(max = 50, message = "Tag toi da 50 ky tu") String> tags,

        String affiliateUrl
) {}