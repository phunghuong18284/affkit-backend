package vn.affkit.post.dto;

import jakarta.validation.constraints.NotBlank;

public record GeneratePostRequest(
        @NotBlank String productUrl,
        String productName,
        String productPrice,
        String affiliateUrl
) {}