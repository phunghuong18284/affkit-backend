package vn.affkit.post.dto;

public record ScrapedProduct(
        String name,
        long price,
        String imageUrl,
        String sourceUrl
) {}